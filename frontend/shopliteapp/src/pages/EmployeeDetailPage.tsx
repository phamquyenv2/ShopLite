import React, { useCallback, useEffect, useMemo, useState } from 'react';
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
import { useParams } from 'react-router-dom';
import {
    callOutline,
    chevronBackOutline,
    chevronForwardOutline,
    createOutline,
    ellipsisHorizontalOutline,
    checkmarkOutline,
    businessOutline,
} from 'ionicons/icons';
import type { Employee, Payroll, Roster } from '../api/types';
import { employeeService } from '../services/employee.service';
import { rosterService } from '../services/roster.service';
import { authApis, endpoints } from '../utils/Apis';
import './EmployeeDetailPage.css';

// ── date helpers ──────────────────────────────────────────────────────────────
const pad = (n: number) => String(n).padStart(2, '0');
const fmtDate = (d: Date) => `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`;

const getMonday = (date: Date) => {
    const d = new Date(date);
    const day = d.getDay();
    d.setDate(d.getDate() - (day === 0 ? 6 : day - 1));
    d.setHours(0, 0, 0, 0);
    return d;
};

const addDays = (d: Date, n: number) => {
    const r = new Date(d);
    r.setDate(r.getDate() + n);
    return r;
};

const getWeekDates = (monday: Date) => Array.from({ length: 7 }, (_, i) => addDays(monday, i));

const getWeekNumber = (d: Date) => {
    const jan1 = new Date(d.getFullYear(), 0, 1);
    return Math.ceil(((d.getTime() - jan1.getTime()) / 86400000 + jan1.getDay() + 1) / 7);
};

const currentMonthStr = () => {
    const n = new Date();
    return `${n.getFullYear()}-${pad(n.getMonth() + 1)}`;
};

const WEEKDAY_LABELS = ['T2', 'T3', 'T4', 'T5', 'T6', 'T7', 'CN'];

const empCode = (id: number) => `NV${String(id).padStart(6, '0')}`;

const getInitials = (name?: string | null) => {
    if (!name) return 'NV';
    const words = name.trim().split(' ');
    return words[words.length - 1][0]?.toUpperCase() ?? 'N';
};

const formatShift = (t?: string | null) => t?.slice(0, 5) ?? '--:--';

const formatPeriod = (period: string) => {
    const [y, m] = period.split('-');
    return `Bảng lương tháng ${Number(m)}/${y}`;
};

const formatPeriodCode = (period: string) => {
    const [y, m] = period.split('-').map(Number);
    const last = new Date(y, m, 0).getDate();
    return `01/${pad(m)}/${y} - ${pad(last)}/${pad(m)}/${y}`;
};

// ── payroll fetch helper ──────────────────────────────────────────────────────
async function fetchPayrolls(employeeId: number): Promise<Payroll[]> {
    try {
        const res = await authApis().get<any>(endpoints['payroll-by-employee'](employeeId));
        const d = res.data;
        return (d?.data ?? d ?? []) as Payroll[];
    } catch {
        return [];
    }
}

// ── component ─────────────────────────────────────────────────────────────────
const EmployeeDetailPage: React.FC = () => {
    const router = useIonRouter();
    const { id } = useParams<{ id: string }>();
    const employeeId = Number(id);

    const [employee, setEmployee] = useState<Employee | null>(null);
    const [rosters, setRosters] = useState<Roster[]>([]);
    const [payrolls, setPayrolls] = useState<Payroll[]>([]);
    const [loading, setLoading] = useState(true);
    const [weekLoading, setWeekLoading] = useState(false);
    const [toast, setToast] = useState<string | null>(null);
    const [mondayOffset, setMondayOffset] = useState(0);

    const today = useMemo(() => new Date(), []);
    const todayStr = useMemo(() => fmtDate(today), [today]);

    const monday = useMemo(() => {
        const base = getMonday(today);
        base.setDate(base.getDate() + mondayOffset * 7);
        return base;
    }, [today, mondayOffset]);

    const weekDates = useMemo(() => getWeekDates(monday), [monday]);
    const weekFrom = useMemo(() => fmtDate(weekDates[0]), [weekDates]);
    const weekTo = useMemo(() => fmtDate(weekDates[6]), [weekDates]);
    const weekLabel = `Tuần ${getWeekNumber(monday)}`;
    const weekSub = `Tháng ${monday.getMonth() + 1}, ${monday.getFullYear()}`;

    // Unique shifts in this week (group by startTime-endTime)
    const weekShiftGroups = useMemo(() => {
        const map = new Map<string, { label: string; startTime: string; endTime: string; activeDates: string[] }>();
        rosters.filter(r => r.type === 'WORKING').forEach(r => {
            const key = `${r.startTime}-${r.endTime}`;
            if (!map.has(key)) {
                map.set(key, {
                    label: r.note || 'Ca làm',
                    startTime: r.startTime ?? '',
                    endTime: r.endTime ?? '',
                    activeDates: [],
                });
            }
            map.get(key)!.activeDates.push(r.workingDay);
        });
        return Array.from(map.values()).sort((a, b) => a.startTime.localeCompare(b.startTime));
    }, [rosters]);

    // Month stats (from current month)
    const monthStr = currentMonthStr();
    const monthRosters = useMemo(() => rosters.filter(r => r.workingDay.startsWith(monthStr)), [rosters, monthStr]);
    const workingCount = useMemo(() => monthRosters.filter(r => r.type === 'WORKING').length, [monthRosters]);
    const leaveCount = useMemo(() => monthRosters.filter(r => r.type !== 'WORKING').length, [monthRosters]);

    // ── load week rosters ────────────────────────────────────────────────────
    const loadWeekRosters = useCallback(async (from: string, to: string) => {
        if (!employeeId) return;
        setWeekLoading(true);
        try {
            const list = await rosterService.getByEmployee(employeeId, from, to);
            setRosters(list);
        } catch (err: any) {
            // silently fail for week load
        } finally {
            setWeekLoading(false);
        }
    }, [employeeId]);

    // ── initial load ─────────────────────────────────────────────────────────
    const loadData = useCallback(async () => {
        setLoading(true);
        try {
            const emps = await employeeService.getEmployees();
            const emp = emps.find(e => e.id === employeeId) ?? null;
            setEmployee(emp);

            if (emp) {
                // load this week + payrolls in parallel
                const [weekList, payrollList] = await Promise.all([
                    rosterService.getByEmployee(employeeId, weekFrom, weekTo).catch(() => [] as Roster[]),
                    fetchPayrolls(employeeId),
                ]);
                setRosters(weekList);
                setPayrolls(payrollList);
            }
        } catch (err: any) {
            setToast(err.message || 'Không thể tải dữ liệu');
        } finally {
            setLoading(false);
        }
    }, [employeeId, weekFrom, weekTo]);

    useIonViewWillEnter(() => {
        loadData();
    });

    // Reload rosters when week changes
    useEffect(() => {
        if (!loading) {
            loadWeekRosters(weekFrom, weekTo);
        }
    // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [weekFrom, weekTo]);

    // ── skeleton header ───────────────────────────────────────────────────────
    const PageHeader = (
        <IonHeader className="ed-header ion-no-border">
            <IonToolbar className="ed-toolbar">
                <IonButtons slot="start">
                    <IonButton color="dark" onClick={() => router.goBack()}>
                        <IonIcon icon={chevronBackOutline} style={{ fontSize: 26 }} />
                    </IonButton>
                </IonButtons>
                <div className="ed-title">Chi tiết nhân viên</div>
                <IonButtons slot="end">
                    <IonButton color="dark">
                        <IonIcon icon={ellipsisHorizontalOutline} style={{ fontSize: 22 }} />
                    </IonButton>
                </IonButtons>
            </IonToolbar>
        </IonHeader>
    );

    if (loading) {
        return (
            <IonPage className="emp-detail-page">
                {PageHeader}
                <IonContent className="ed-content">
                    <div className="ed-loading"><IonSpinner name="crescent" /></div>
                </IonContent>
            </IonPage>
        );
    }

    if (!employee) {
        return (
            <IonPage className="emp-detail-page">
                {PageHeader}
                <IonContent className="ed-content">
                    <div className="ed-empty">Không tìm thấy nhân viên.</div>
                </IonContent>
            </IonPage>
        );
    }

    return (
        <IonPage className="emp-detail-page">
            {PageHeader}

            <IonContent className="ed-content">

                {/* ── Profile ── */}
                <div className="ed-card ed-profile-card">
                    <div className="ed-avatar">{getInitials(employee.username)}</div>
                    <div className="ed-profile-info">
                        <div className="ed-profile-name">{employee.username || 'Nhân viên'}</div>
                        <div className="ed-profile-code">{empCode(employee.id)}</div>
                    </div>
                    <button className="ed-edit-btn" onClick={() => router.push(`/employees`)}>
                        <IonIcon icon={createOutline} />
                        Sửa
                    </button>
                </div>

                {/* ── Contact info ── */}
                <div className="ed-card">
                    <div className="ed-info-row">
                        <div className="ed-info-block">
                            <div className="ed-info-label">Số điện thoại</div>
                            <div className="ed-info-value">{employee.phone || 'Chưa cập nhật'}</div>
                        </div>
                        {employee.phone && (
                            <a href={`tel:${employee.phone}`} className="ed-call-btn">
                                <IonIcon icon={callOutline} />
                            </a>
                        )}
                    </div>
                    <div className="ed-divider" />
                    <div className="ed-info-row">
                        <div className="ed-info-block">
                            <div className="ed-info-label">Phòng ban</div>
                            <div className="ed-info-value ed-info-value--muted">
                                {employee.officeName || <span className="ed-info-placeholder">Chưa gán phòng ban</span>}
                            </div>
                        </div>
                        <IonIcon icon={businessOutline} style={{ color: '#94a3b8', fontSize: 20 }} />
                    </div>
                </div>

                {/* ── Salary link row ── */}
                <div className="ed-card ed-salary-link">
                    <button
                        className="ed-salary-btn"
                        onClick={() => router.push(`/employees/${employee.id}/salary`)}
                    >
                        Thêm thiết lập lương
                    </button>
                </div>

                {/* ── Weekly roster ── */}
                <div className="ed-card ed-roster-card">
                    <div className="ed-section-header">
                        <div className="ed-section-label">LỊCH LÀM VIỆC TUẦN NÀY</div>
                        <button className="ed-section-action" onClick={() => router.push('/roster')}>
                            <IonIcon icon={chevronForwardOutline} />
                        </button>
                    </div>

                    {/* week nav */}
                    <div className="ed-week-nav">
                        <button className="ed-week-nav-btn" onClick={() => setMondayOffset(o => o - 1)}>
                            <IonIcon icon={chevronBackOutline} />
                        </button>
                        <div className="ed-week-label">
                            <span className="ed-week-main">{weekLabel}</span>
                            <span className="ed-week-sub">{weekSub}</span>
                        </div>
                        <button className="ed-week-nav-btn" onClick={() => setMondayOffset(o => o + 1)}>
                            <IonIcon icon={chevronForwardOutline} />
                        </button>
                    </div>

                    {/* grid */}
                    <div className="ed-roster-grid">
                        {/* header row */}
                        <div className="ed-roster-col-header" />
                        {weekDates.map((d, i) => {
                            const isToday = fmtDate(d) === todayStr;
                            return (
                                <div key={i} className="ed-roster-day-head">
                                    <span className={`ed-roster-weekday ${isToday ? 'today' : ''}`}>
                                        {WEEKDAY_LABELS[i]}
                                    </span>
                                    <span className={`ed-roster-date ${isToday ? 'today-circle' : ''}`}>
                                        {d.getDate()}
                                    </span>
                                </div>
                            );
                        })}

                        {/* shift rows */}
                        {weekLoading ? (
                            <div className="ed-roster-loading"><IonSpinner name="dots" /></div>
                        ) : weekShiftGroups.length === 0 ? (
                            <div className="ed-roster-empty">Tuần này chưa có ca nào</div>
                        ) : weekShiftGroups.map(shift => (
                            <React.Fragment key={shift.startTime + shift.endTime}>
                                <div className="ed-roster-shift-label">
                                    <span className="ed-roster-shift-name">{shift.label}</span>
                                    <span className="ed-roster-shift-time">
                                        {formatShift(shift.startTime)} -<br />{formatShift(shift.endTime)}
                                    </span>
                                </div>
                                {weekDates.map((d, i) => (
                                    <div key={i} className="ed-roster-cell">
                                        {shift.activeDates.includes(fmtDate(d)) && (
                                            <IonIcon icon={checkmarkOutline} className="ed-roster-check" />
                                        )}
                                    </div>
                                ))}
                            </React.Fragment>
                        ))}
                    </div>
                </div>

                {/* ── Attendance stats this month ── */}
                <div className="ed-card">
                    <div className="ed-section-header">
                        <div className="ed-section-label">
                            CHẤM CÔNG THG {today.getMonth() + 1}, {today.getFullYear()}
                        </div>
                        <button className="ed-section-action" onClick={() => router.push('/attendance')}>
                            <IonIcon icon={chevronForwardOutline} />
                        </button>
                    </div>
                    <div className="ed-stats-grid">
                        <div className="ed-stat-item">
                            <div className="ed-stat-label">Đi làm</div>
                            <div className="ed-stat-value">{workingCount} ca</div>
                        </div>
                        <div className="ed-stat-item">
                            <div className="ed-stat-label">Nghỉ làm</div>
                            <div className="ed-stat-value">{leaveCount} ca</div>
                        </div>
                        <div className="ed-stat-item">
                            <div className="ed-stat-label">Đi muộn</div>
                            <div className="ed-stat-value">0 lần</div>
                        </div>
                        <div className="ed-stat-item">
                            <div className="ed-stat-label">Về sớm</div>
                            <div className="ed-stat-value">0 lần</div>
                        </div>
                    </div>
                    <div className="ed-overtime-row">
                        <span className="ed-stat-label">Làm thêm</span>
                        <span className="ed-stat-value-sm">0 giờ 0 phút</span>
                    </div>
                </div>

                {/* ── Payroll ── */}
                <div className="ed-card">
                    <div className="ed-section-label" style={{ marginBottom: 12 }}>PHIẾU LƯƠNG</div>
                    {payrolls.length === 0 ? (
                        <div className="ed-empty-text">Chưa có dữ liệu</div>
                    ) : payrolls.slice(0, 3).map(p => (
                        <div key={p.id} className="ed-payroll-row" onClick={() => router.push('/payrolls')}>
                            <div className="ed-payroll-info">
                                <div className="ed-payroll-title">{formatPeriod(p.period)}</div>
                                <div className="ed-payroll-code">{formatPeriodCode(p.period)}</div>
                            </div>
                            <div className="ed-payroll-right">
                                <span className="ed-payroll-amount">
                                    {new Intl.NumberFormat('vi-VN').format(p.totalSalary ?? 0)}
                                </span>
                                <IonIcon icon={chevronForwardOutline} className="ed-payroll-arrow" />
                            </div>
                        </div>
                    ))}
                </div>

                {/* ── Debt & Advance ── */}
                <div className="ed-card">
                    <div className="ed-section-header">
                        <div className="ed-section-label">NỢ VÀ TẠM ỨNG</div>
                        <button className="ed-section-action">
                            <IonIcon icon={chevronForwardOutline} />
                        </button>
                    </div>
                    <div className="ed-empty-text">Chưa có dữ liệu</div>
                </div>

                <div style={{ height: 32 }} />
            </IonContent>

            <IonToast isOpen={toast !== null} message={toast ?? ''} duration={2400} onDidDismiss={() => setToast(null)} />
        </IonPage>
    );
};

export default EmployeeDetailPage;
