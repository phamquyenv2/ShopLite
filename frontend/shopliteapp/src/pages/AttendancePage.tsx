import React, { useMemo, useState } from 'react';
import {
    IonButton,
    IonButtons,
    IonContent,
    IonHeader,
    IonIcon,
    IonPage,
    IonSpinner,
    IonToast,
    IonToolbar,
    useIonRouter,
    useIonViewWillEnter,
} from '@ionic/react';
import {
    alertCircleOutline,
    checkmarkCircleOutline,
    chevronBackOutline,
    locationOutline,
    logInOutline,
    logOutOutline,
    refreshOutline,
    searchOutline,
    timeOutline,
    calendarOutline,
    banOutline,
} from 'ionicons/icons';
import type { Attendance, AttendanceLocationPayload, Roster } from '../api/types';
import { attendanceService } from '../services/attendance.service';
import { getStoredCurrentStore, getStoredUser } from '../utils/Apis';
import './WorkforcePages.css';

const pad = (value: number) => String(value).padStart(2, '0');
const today = () => {
    const date = new Date();
    return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`;
};

const minutesToHours = (minutes?: number | null) => minutes ? `${(minutes / 60).toFixed(2)} giờ` : '0 giờ';

const formatDateTime = (value?: string | null) => {
    if (!value) return '--';
    return new Date(value).toLocaleString('vi-VN', {
        hour: '2-digit',
        minute: '2-digit',
        day: '2-digit',
        month: '2-digit',
    });
};

const formatShiftTime = (value?: string | null) => value?.slice(0, 5) || '--:--';

const checkInWindowText = (rosters: Roster[]) => {
    const workingRosters = rosters
        .filter(roster => roster.type === 'WORKING' && roster.startTime && roster.endTime)
        .sort((a, b) => String(a.startTime).localeCompare(String(b.startTime)));

    if (workingRosters.length === 0) return '';

    const windows = workingRosters.map(roster => {
        const allowedStart = roster.checkInAllowedFrom
            ? formatShiftTime(roster.checkInAllowedFrom)
            : (() => {
                const [hour = 0, minute = 0] = String(roster.startTime).split(':').map(Number);
                const start = new Date();
                start.setHours(hour, minute - 30, 0, 0);
                return `${pad(start.getHours())}:${pad(start.getMinutes())}`;
            })();
        const allowedEnd = formatShiftTime(roster.checkInAllowedTo || roster.endTime);
        const shiftStart = formatShiftTime(roster.startTime);
        const shiftEnd = formatShiftTime(roster.endTime);
        return `tu ${allowedStart} den ${allowedEnd} (ca ${shiftStart}-${shiftEnd})`;
    });

    return `Chi duoc check-in trong khung gio ${windows.join('; ')}`;
};

const withCheckInWindow = (message: string, rosters: Roster[]) => {
    const normalized = message.toLowerCase();
    const isCheckInWindowError = normalized.includes('30p') || normalized.includes('checkin') || normalized.includes('check-in');
    const windowText = checkInWindowText(rosters);
    return isCheckInWindowError && windowText ? windowText : message;
};

const friendlyAttendanceError = (message?: string) => {
    const raw = message || '';
    if (raw.includes('Employee not found')) {
        return 'Tài khoản này chưa được gắn với hồ sơ nhân viên nên chưa thể tự chấm công.';
    }
    if (raw.includes('has no office assigned')) {
        return 'Nhân viên chưa được gán chi nhánh/văn phòng để kiểm tra vị trí chấm công.';
    }
    if (raw.includes('open shift')) {
        return 'Bạn đang có ca làm chưa check-out.';
    }
    if (raw.includes('No open shift')) {
        return 'Bạn chưa có ca đang mở để check-out.';
    }
    if (raw.includes('OUT_OF_ZONE')) {
        return raw.replace(/^OUT_OF_ZONE:\s*/, '');
    }
    return raw || 'Không thể thực hiện chấm công';
};

const getLocation = (): Promise<AttendanceLocationPayload> => new Promise((resolve, reject) => {
    if (!navigator.geolocation) {
        reject(new Error('Thiết bị không hỗ trợ lấy vị trí'));
        return;
    }

    navigator.geolocation.getCurrentPosition(
        pos => resolve({
            latitude: pos.coords.latitude,
            longitude: pos.coords.longitude,
            deviceId: navigator.userAgent.slice(0, 120),
        }),
        () => reject(new Error('Không thể lấy vị trí. Vui lòng cấp quyền GPS.')),
        { enableHighAccuracy: true, timeout: 12000, maximumAge: 30000 },
    );
});

// Derive a human-readable status + color class for each roster card
type ShiftStatus = 'active' | 'open' | 'done' | 'expired' | 'leave';
const getRosterStatus = (
    roster: Roster,
    openShiftRosterId: number | null | undefined,
    completedIds: Set<number>,
): ShiftStatus => {
    if (roster.type !== 'WORKING') return 'leave';
    if (openShiftRosterId && roster.id === openShiftRosterId) return 'open';
    if (completedIds.has(roster.id)) return 'done';
    if (roster.expired) return 'expired';
    return 'active';
};

const shiftStatusLabel: Record<ShiftStatus, string> = {
    active: 'Có thể check-in',
    open: 'Đang trong ca',
    done: 'Đã chấm xong',
    expired: 'Hết hạn check-in',
    leave: 'Nghỉ / không chấm công',
};

const shiftStatusClass: Record<ShiftStatus, string> = {
    active: 'shift-card--active',
    open: 'shift-card--open',
    done: 'shift-card--done',
    expired: 'shift-card--expired',
    leave: 'shift-card--leave',
};

const AttendancePage: React.FC = () => {
    const router = useIonRouter();
    const currentRole = useMemo(() => {
        const store = getStoredCurrentStore<{ memberRole?: string | null }>();
        const user = getStoredUser<{ roleName?: string | null }>();
        return (store?.memberRole || user?.roleName || '').toUpperCase();
    }, []);
    const showSelfAttendance = !/MANAGER|OWNER|ADMIN/.test(currentRole);
    const [todayAttendance, setTodayAttendance] = useState<Attendance | null>(null);
    const [todayRosters, setTodayRosters] = useState<Roster[]>([]);
    const [selectedRosterId, setSelectedRosterId] = useState<number | null>(null);
    const [records, setRecords] = useState<Attendance[]>([]);
    const [selfAttendanceMessage, setSelfAttendanceMessage] = useState('');
    const [query, setQuery] = useState('');
    const [scope, setScope] = useState<'today' | 'all'>('today');
    const [loading, setLoading] = useState(false);
    const [acting, setActing] = useState(false);
    const [toast, setToast] = useState<string | null>(null);

    const openShift = !!todayAttendance?.checkIn && !todayAttendance?.checkOut;
    const canSelfCheck = !selfAttendanceMessage;

    const completedRosterIds = useMemo(() => {
        return new Set(records.filter(item => !!item.checkOut).map(item => item.rosterId).filter(Boolean) as number[]);
    }, [records]);

    const selectedRoster = useMemo(() => {
        const activeRosterId = openShift ? todayAttendance?.rosterId : selectedRosterId;
        return todayRosters.find(item => item.id === activeRosterId) || null;
    }, [openShift, selectedRosterId, todayAttendance?.rosterId, todayRosters]);

    const selectedRosterExpired = !!selectedRoster?.expired;

    const filteredRecords = useMemo(() => {
        const keyword = query.trim().toLowerCase();
        return records.filter(item => {
            if (scope === 'today' && item.workingDay !== today()) return false;
            if (!keyword) return true;
            return (item.employeeUsername || '').toLowerCase().includes(keyword)
                || String(item.employeeId).includes(keyword)
                || (item.status || '').toLowerCase().includes(keyword);
        });
    }, [query, records, scope]);

    const stats = useMemo(() => {
        const todayRows = records.filter(item => item.workingDay === today());
        return {
            present: todayRows.length,
            open: todayRows.filter(item => !item.checkOut).length,
            late: todayRows.filter(item => (item.lateMinutes || 0) > 0).length,
        };
    }, [records]);

    const loadData = async () => {
        setLoading(true);
        try {
            const all = await attendanceService.getAll().catch(() => []);
            setRecords(all);

            if (showSelfAttendance) {
                try {
                    const [meToday, rosters] = await Promise.all([
                        attendanceService.getMeToday(),
                        attendanceService.getMyTodayRosters(),
                    ]);
                    setTodayAttendance(meToday);
                    setTodayRosters(rosters);
                    setSelectedRosterId(prev => {
                        if (meToday?.rosterId) return meToday.rosterId;
                        if (prev && rosters.some(item => item.id === prev && !item.expired)) return prev;
                        return rosters.find(item => item.type === 'WORKING' && !item.expired)?.id ?? null;
                    });
                    setSelfAttendanceMessage('');
                } catch (err: any) {
                    setTodayAttendance(null);
                    setTodayRosters([]);
                    setSelectedRosterId(null);
                    setSelfAttendanceMessage(friendlyAttendanceError(err.message));
                }
            } else {
                setTodayAttendance(null);
                setTodayRosters([]);
                setSelectedRosterId(null);
                setSelfAttendanceMessage('');
            }
        } catch (err: any) {
            setToast(friendlyAttendanceError(err.message) || 'Không thể tải chấm công');
        } finally {
            setLoading(false);
        }
    };

    useIonViewWillEnter(() => {
        loadData();
    });

    const doAttendance = async (mode: 'in' | 'out') => {
        if (!canSelfCheck) {
            setToast(selfAttendanceMessage);
            return;
        }
        if (mode === 'in' && !selectedRosterId) {
            setToast('Vui long chon ca lam de check-in');
            return;
        }

        setActing(true);
        try {
            const payload = await getLocation();
            const attendancePayload = mode === 'in'
                ? { ...payload, rosterId: selectedRosterId }
                : payload;
            const result = mode === 'in'
                ? await attendanceService.checkIn(attendancePayload)
                : await attendanceService.checkOut(attendancePayload);
            setTodayAttendance(result);
            await loadData();
            setToast(mode === 'in' ? 'Đã check-in' : 'Đã check-out');
        } catch (err: any) {
            setToast(withCheckInWindow(friendlyAttendanceError(err.message), todayRosters));
        } finally {
            setActing(false);
        }
    };

    return (
        <IonPage className="workforce-page attendance-page">
            <IonHeader className="wf-header ion-no-border">
                <IonToolbar className="wf-toolbar">
                    <IonButtons slot="start">
                        <IonButton color="dark" onClick={() => router.goBack()}>
                            <IonIcon icon={chevronBackOutline} style={{ fontSize: 26 }} />
                        </IonButton>
                    </IonButtons>
                    <div className="wf-title">Chấm công</div>
                    <IonButtons slot="end">
                        <IonButton onClick={loadData}>
                            <IonIcon icon={refreshOutline} />
                        </IonButton>
                    </IonButtons>
                </IonToolbar>

                <div className="wf-top-card">
                    {showSelfAttendance && <div className="wf-attendance-panel">
                        <div className="wf-attendance-head">
                            <div>
                                <span className={`wf-status-pill ${openShift ? 'green' : todayAttendance ? 'blue' : 'gray'}`}>
                                    {openShift ? 'Đang trong ca' : todayAttendance ? 'Đã hoàn tất' : 'Chưa check-in'}
                                </span>
                                <div className="wf-attendance-title">
                                    {openShift ? 'Ca làm đang mở' : todayAttendance ? 'Ca hôm nay đã xong' : 'Sẵn sàng chấm công'}
                                </div>
                                {selectedRoster && (
                                    <div className="wf-attendance-shift-name">
                                        {formatShiftTime(selectedRoster.startTime)} - {formatShiftTime(selectedRoster.endTime)}
                                        {selectedRoster.note ? ` · ${selectedRoster.note}` : ''}
                                    </div>
                                )}
                            </div>
                        </div>

                        {/* ── Ca làm hôm nay ── */}
                        {todayRosters.length > 0 && (
                            <div className="shift-cards-section">
                                <div className="shift-cards-title">
                                    <IonIcon icon={calendarOutline} />
                                    Ca làm hôm nay ({todayRosters.length})
                                </div>
                                <div className="shift-cards-list">
                                    {todayRosters.map(roster => {
                                        const status = getRosterStatus(
                                            roster,
                                            openShift ? todayAttendance?.rosterId : null,
                                            completedRosterIds,
                                        );
                                        const isSelectable = !openShift && status === 'active';
                                        const isSelected = selectedRosterId === roster.id && !openShift;
                                        const isCurrentOpen = status === 'open';
                                        return (
                                            <button
                                                key={roster.id}
                                                className={[
                                                    'shift-card',
                                                    shiftStatusClass[status],
                                                    isSelected ? 'shift-card--selected' : '',
                                                    acting ? 'shift-card--acting' : '',
                                                ].filter(Boolean).join(' ')}
                                                disabled={!isSelectable || acting}
                                                onClick={() => isSelectable && setSelectedRosterId(roster.id)}
                                            >
                                                <div className="shift-card__left">
                                                    <div className="shift-card__icon">
                                                        <IonIcon icon={
                                                            status === 'done' ? checkmarkCircleOutline
                                                            : status === 'expired' ? banOutline
                                                            : status === 'open' ? locationOutline
                                                            : calendarOutline
                                                        } />
                                                    </div>
                                                    <div className="shift-card__info">
                                                        <span className="shift-card__time">
                                                            {formatShiftTime(roster.startTime)} – {formatShiftTime(roster.endTime)}
                                                        </span>
                                                        {roster.note && (
                                                            <span className="shift-card__note">{roster.note}</span>
                                                        )}
                                                        {roster.expectedHours && (
                                                            <span className="shift-card__note">{roster.expectedHours} giờ</span>
                                                        )}
                                                    </div>
                                                </div>
                                                <div className={`shift-card__badge shift-card__badge--${status}`}>
                                                    {isCurrentOpen && <span className="shift-card__pulse" />}
                                                    {shiftStatusLabel[status]}
                                                </div>
                                            </button>
                                        );
                                    })}
                                </div>
                            </div>
                        )}

                        {!openShift && !selfAttendanceMessage && todayRosters.length === 0 && (
                            <div className="wf-blocked-card">
                                <IonIcon icon={alertCircleOutline} />
                                <span>Hôm nay bạn chưa có ca làm được xếp lịch.</span>
                            </div>
                        )}

                        <div className="wf-time-grid">
                            <div className="wf-time-box">
                                <span>Giờ vào</span>
                                <strong>{formatDateTime(todayAttendance?.checkIn)}</strong>
                            </div>
                            <div className="wf-time-box">
                                <span>Giờ ra</span>
                                <strong>{formatDateTime(todayAttendance?.checkOut)}</strong>
                            </div>
                        </div>

                        {selfAttendanceMessage && (
                            <div className="wf-blocked-card">
                                <IonIcon icon={alertCircleOutline} />
                                <span>{selfAttendanceMessage} Vào mục Nhân viên để tạo/gắn nhân viên cho tài khoản này trước khi check-in.</span>
                            </div>
                        )}

                        <div className="wf-attendance-actions">
                            <button
                                className="wf-action-main"
                                disabled={acting || openShift || !canSelfCheck || !selectedRosterId || selectedRosterExpired || completedRosterIds.has(selectedRosterId)}
                                onClick={() => doAttendance('in')}
                            >
                                <IonIcon icon={logInOutline} />
                                Check-in
                            </button>
                            <button
                                className="wf-action-dark"
                                disabled={acting || !openShift || !canSelfCheck}
                                onClick={() => doAttendance('out')}
                            >
                                <IonIcon icon={logOutOutline} />
                                Check-out
                            </button>
                        </div>
                    </div>}

                    <div className="wf-summary">
                        <div className="wf-summary-item">
                            <div className="wf-summary-value">{stats.present}</div>
                            <div className="wf-summary-label">Lượt hôm nay</div>
                        </div>
                        <div className="wf-summary-item">
                            <div className="wf-summary-value">{stats.open}</div>
                            <div className="wf-summary-label">Đang làm</div>
                        </div>
                        <div className="wf-summary-item">
                            <div className="wf-summary-value">{stats.late}</div>
                            <div className="wf-summary-label">Đi trễ</div>
                        </div>
                    </div>

                    <div className="wf-segments">
                        <button className={`wf-segment ${scope === 'today' ? 'active' : ''}`} onClick={() => setScope('today')}>Hôm nay</button>
                        <button className={`wf-segment ${scope === 'all' ? 'active' : ''}`} onClick={() => setScope('all')}>Tất cả</button>
                    </div>

                    <div className="wf-search">
                        <IonIcon icon={searchOutline} />
                        <input value={query} onChange={e => setQuery(e.target.value)} placeholder="Tìm nhân viên, trạng thái" />
                    </div>
                </div>
            </IonHeader>

            <IonContent className="wf-content">
                <div className="wf-list-card">
                    <div className="wf-section-label">Lịch sử chấm công</div>
                    {loading ? <div className="wf-loading"><IonSpinner name="crescent" /></div> : (
                        filteredRecords.length === 0 ? (
                            <div className="wf-empty">Chưa có dữ liệu chấm công.</div>
                        ) : filteredRecords.map(item => (
                            <div className="wf-row" key={item.id}>
                                <div className="wf-avatar">
                                    <IonIcon icon={item.checkOut ? timeOutline : locationOutline} />
                                </div>
                                <div className="wf-row-main">
                                    <div className="wf-row-title">{item.employeeUsername || `NV #${item.employeeId}`}</div>
                                    <div className="wf-row-sub">
                                        {item.workingDay || '--'} · {formatDateTime(item.checkIn)} - {formatDateTime(item.checkOut)}
                                    </div>
                                    <div className="wf-row-note">
                                        Tính lương {minutesToHours(item.payableMinutes)} · Trễ {item.lateMinutes || 0}p · Về sớm {item.earlyLeaveMinutes || 0}p
                                    </div>
                                </div>
                                <span className={`wf-badge ${item.checkOut ? 'green' : 'orange'}`}>{item.checkOut ? 'Đã ra' : 'Trong ca'}</span>
                            </div>
                        ))
                    )}
                </div>
            </IonContent>

            <IonToast isOpen={toast !== null} message={toast ?? ''} duration={2800} onDidDismiss={() => setToast(null)} />
        </IonPage>
    );
};

export default AttendancePage;
