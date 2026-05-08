import React, { useMemo, useState } from 'react';
import {
    IonButton,
    IonButtons,
    IonContent,
    IonHeader,
    IonIcon,
    IonModal,
    IonPage,
    IonSpinner,
    IonToast,
    IonAlert,
    IonToolbar,
    useIonRouter,
    useIonViewWillEnter,
} from '@ionic/react';
import {
    addOutline,
    briefcaseOutline,
    calendarClearOutline,
    checkmarkOutline,
    chevronBackOutline,
    chevronDownOutline,
    chevronForwardOutline,
    closeOutline,
    createOutline,
    personOutline,
    searchOutline,
    timeOutline,
    trashOutline,
} from 'ionicons/icons';
import type { Employee, Roster, RosterType, RosterUpsert } from '../api/types';
import { employeeService } from '../services/employee.service';
import { rosterService } from '../services/roster.service';
import { useStorePermissions } from '../utils/useStorePermissions';
import './WorkforcePages.css';

const pad = (value: number) => String(value).padStart(2, '0');
const toDateKey = (date: Date) => `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`;
const today = () => toDateKey(new Date());
const currentMonth = () => today().slice(0, 7);
const formatShiftTime = (value?: string | null) => value?.slice(0, 5) || '--:--';

const parseDateKey = (value: string) => {
    const [year, month, day] = value.split('-').map(Number);
    return new Date(year, month - 1, day);
};

const rosterLabels: Record<RosterType, string> = {
    WORKING: 'Ca làm',
    LEAVE_APPROVED: 'Nghỉ phép',
    LEAVE_UNAPPROVED: 'Nghỉ không phép',
};

const weekdayLabels = ['T2', 'T3', 'T4', 'T5', 'T6', 'T7', 'CN'];

const blankForm = (date: string): RosterUpsert => ({
    employeeId: 0,
    workingDay: date,
    startTime: '08:00',
    endTime: '17:00',
    checkInAllowedFrom: '07:30',
    checkInAllowedTo: '17:00',
    checkOutAllowedFrom: '16:30',
    checkOutAllowedTo: '17:00',
    type: 'WORKING',
    note: '',
    unpaidBreakMinutes: 60,
});

const getBadgeClass = (type: RosterType) => {
    if (type === 'WORKING') return 'green';
    if (type === 'LEAVE_UNAPPROVED') return 'red';
    return 'orange';
};

const getMonthLabel = (month: string) => {
    const [year, monthIndex] = month.split('-').map(Number);
    return `Tháng ${monthIndex}/${year}`;
};

const getCalendarCells = (month: string) => {
    const [year, monthIndex] = month.split('-').map(Number);
    const first = new Date(year, monthIndex - 1, 1);
    const last = new Date(year, monthIndex, 0);
    const mondayBasedStart = (first.getDay() + 6) % 7;
    const cells: Array<{ date: string; inMonth: boolean }> = [];

    for (let i = mondayBasedStart; i > 0; i -= 1) {
        const date = new Date(year, monthIndex - 1, 1 - i);
        cells.push({ date: toDateKey(date), inMonth: false });
    }

    for (let day = 1; day <= last.getDate(); day += 1) {
        cells.push({ date: toDateKey(new Date(year, monthIndex - 1, day)), inMonth: true });
    }

    while (cells.length % 7 !== 0) {
        const date = new Date(year, monthIndex - 1, last.getDate() + (cells.length % 7 === 0 ? 0 : cells.length - mondayBasedStart - last.getDate() + 1));
        cells.push({ date: toDateKey(date), inMonth: false });
    }

    return cells;
};

const RosterPage: React.FC = () => {
    const router = useIonRouter();
    const { can } = useStorePermissions();
    const canViewEmployees = can('/api/v1/employees', 'GET');
    const canCreateRoster = can('/api/v1/roster', 'POST');
    const canUpdateRoster = can('/api/v1/roster/{id}', 'PUT');
    const canDeleteRoster = can('/api/v1/roster/{id}', 'DELETE');
    const [month, setMonth] = useState(currentMonth());
    const [selectedDate, setSelectedDate] = useState(today());
    const [query, setQuery] = useState('');
    const [rosters, setRosters] = useState<Roster[]>([]);
    const [employees, setEmployees] = useState<Employee[]>([]);
    const [loading, setLoading] = useState(false);
    const [saving, setSaving] = useState(false);
    const [modalOpen, setModalOpen] = useState(false);
    const [employeePickerOpen, setEmployeePickerOpen] = useState(false);
    const [employeeSearch, setEmployeeSearch] = useState('');
    const [editing, setEditing] = useState<Roster | null>(null);
    const [deleteTarget, setDeleteTarget] = useState<Roster | null>(null);
    const [form, setForm] = useState<RosterUpsert>(blankForm(today()));
    const [toast, setToast] = useState<string | null>(null);

    const employeeName = useMemo(() => {
        const map = new Map<number, string>();
        employees.forEach(emp => map.set(emp.id, emp.username || `NV #${emp.id}`));
        return map;
    }, [employees]);

    const rostersByDay = useMemo(() => {
        const map = new Map<string, Roster[]>();
        rosters.forEach(item => {
            const list = map.get(item.workingDay) || [];
            list.push(item);
            map.set(item.workingDay, list);
        });
        return map;
    }, [rosters]);

    const selectedRosters = useMemo(() => {
        const keyword = query.trim().toLowerCase();
        const list = [...(rostersByDay.get(selectedDate) || [])].sort((a, b) => {
            const nameA = a.employeeUsername || employeeName.get(a.employeeId) || '';
            const nameB = b.employeeUsername || employeeName.get(b.employeeId) || '';
            if (nameA !== nameB) return nameA.localeCompare(nameB, 'vi');
            return (a.startTime || '').localeCompare(b.startTime || '');
        });
        if (!keyword) return list;
        return list.filter(item => {
            const name = item.employeeUsername || employeeName.get(item.employeeId) || '';
            return name.toLowerCase().includes(keyword) || item.note?.toLowerCase().includes(keyword);
        });
    }, [employeeName, query, rostersByDay, selectedDate]);

    const stats = useMemo(() => ({
        days: new Set(rosters.map(item => item.workingDay)).size,
        working: rosters.filter(item => item.type === 'WORKING').length,
        leave: rosters.filter(item => item.type !== 'WORKING').length,
    }), [rosters]);

    const selectedStats = useMemo(() => {
        const list = rostersByDay.get(selectedDate) || [];
        const employeeCount = new Set(list.map(item => item.employeeId)).size;
        return {
            total: list.length,
            working: list.filter(item => item.type === 'WORKING').length,
            leave: list.filter(item => item.type !== 'WORKING').length,
            employeeCount,
        };
    }, [rostersByDay, selectedDate]);

    const calendarCells = useMemo(() => getCalendarCells(month), [month]);

    const selectedEmployee = useMemo(
        () => employees.find(emp => emp.id === form.employeeId) || null,
        [employees, form.employeeId],
    );

    const filteredEmployees = useMemo(() => {
        const keyword = employeeSearch.trim().toLowerCase();
        if (!keyword) return employees;
        return employees.filter(emp =>
            (emp.username || '').toLowerCase().includes(keyword) ||
            (emp.phone || '').includes(keyword) ||
            (emp.roleName || '').toLowerCase().includes(keyword),
        );
    }, [employeeSearch, employees]);

    const loadData = async (nextMonth = month) => {
        setLoading(true);
        try {
            const [list, emps] = await Promise.all([
                rosterService.getByMonth(nextMonth),
                canViewEmployees ? employeeService.getEmployees() : Promise.resolve([]),
            ]);
            setRosters(list);
            setEmployees(emps.filter(emp => !emp.deleted));
        } catch (err: any) {
            setToast(err.message || 'Không thể tải lịch làm việc');
        } finally {
            setLoading(false);
        }
    };

    useIonViewWillEnter(() => {
        loadData();
    });

    const changeMonth = (step: number) => {
        const [year, monthIndex] = month.split('-').map(Number);
        const next = new Date(year, monthIndex - 1 + step, 1);
        const nextMonth = `${next.getFullYear()}-${pad(next.getMonth() + 1)}`;
        setMonth(nextMonth);
        setSelectedDate(toDateKey(next));
        setQuery('');
        loadData(nextMonth);
    };

    const openCreate = () => {
        setEditing(null);
        setEmployeeSearch('');
        setForm(blankForm(selectedDate));
        setModalOpen(true);
    };

    const openEdit = (item: Roster) => {
        setEditing(item);
        setEmployeeSearch('');
        setForm({
            employeeId: item.employeeId,
            workingDay: item.workingDay,
            startTime: item.startTime || '08:00',
            endTime: item.endTime || '17:00',
            checkInAllowedFrom: item.checkInAllowedFrom || '07:30',
            checkInAllowedTo: item.checkInAllowedTo || item.endTime || '17:00',
            checkOutAllowedFrom: item.checkOutAllowedFrom || item.checkInAllowedFrom || '07:30',
            checkOutAllowedTo: item.checkOutAllowedTo || item.checkInAllowedTo || item.endTime || '17:00',
            type: item.type,
            note: item.note || '',
            unpaidBreakMinutes: item.unpaidBreakMinutes || 0,
        });
        setModalOpen(true);
    };

    const submit = async () => {
        if (!form.employeeId) {
            setToast('Vui lòng chọn nhân viên');
            return;
        }
        if (form.type === 'WORKING' && (!form.startTime || !form.endTime)) {
            setToast('Ca làm cần giờ bắt đầu và kết thúc');
            return;
        }
        if (form.type === 'WORKING' && (!form.checkInAllowedFrom || !form.checkInAllowedTo)) {
            setToast('Vui long nhap khoang thoi gian duoc phep cham cong');
            return;
        }

        setSaving(true);
        try {
            const payload = form.type === 'WORKING'
                ? {
                    ...form,
                    checkOutAllowedFrom: form.checkInAllowedFrom,
                    checkOutAllowedTo: form.checkInAllowedTo,
                }
                : { ...form, startTime: null, endTime: null, checkInAllowedFrom: null, checkInAllowedTo: null, checkOutAllowedFrom: null, checkOutAllowedTo: null, unpaidBreakMinutes: 0 };
            if (editing) await rosterService.update(editing.id, payload);
            else await rosterService.create(payload);

            const nextMonth = form.workingDay.slice(0, 7);
            setModalOpen(false);
            setMonth(nextMonth);
            setSelectedDate(form.workingDay);
            await loadData(nextMonth);
            setToast(editing ? 'Đã cập nhật lịch' : 'Đã thêm ca làm');
        } catch (err: any) {
            setToast(err.message || 'Không thể lưu lịch');
        } finally {
            setSaving(false);
        }
    };

    const doRemove = async (item: Roster) => {
        try {
            await rosterService.remove(item.id);
            await loadData(month);
            setToast('Đã xóa lịch');
        } catch (err: any) {
            setToast(err.message || 'Không thể xóa lịch');
        } finally {
            setDeleteTarget(null);
        }
    };

    return (
        <IonPage className="workforce-page roster-calendar-page">
            <IonHeader className="wf-header ion-no-border">
                <IonToolbar className="wf-toolbar">
                    <IonButtons slot="start">
                        <IonButton color="dark" onClick={() => router.goBack()}>
                            <IonIcon icon={chevronBackOutline} style={{ fontSize: 26 }} />
                        </IonButton>
                    </IonButtons>
                    <div className="wf-title">Lịch làm việc</div>
                </IonToolbar>

                <div className="wf-top-card roster-top-card">
                    <div className="roster-month-bar">
                        <button className="roster-month-nav" onClick={() => changeMonth(-1)}>
                            <IonIcon icon={chevronBackOutline} />
                        </button>
                        <div>
                            <div className="roster-month-title">{getMonthLabel(month)}</div>
                            <div className="roster-month-sub">Bấm vào ngày để xem và xếp ca</div>
                        </div>
                        <button className="roster-month-nav" onClick={() => changeMonth(1)}>
                            <IonIcon icon={chevronForwardOutline} />
                        </button>
                    </div>

                    <div className="wf-summary roster-summary">
                        <div className="wf-summary-item">
                            <div className="wf-summary-value">{stats.days}</div>
                            <div className="wf-summary-label">Ngày có lịch</div>
                        </div>
                        <div className="wf-summary-item">
                            <div className="wf-summary-value">{stats.working}</div>
                            <div className="wf-summary-label">Ca làm</div>
                        </div>
                        <div className="wf-summary-item">
                            <div className="wf-summary-value">{stats.leave}</div>
                            <div className="wf-summary-label">Ngày nghỉ</div>
                        </div>
                    </div>
                </div>
            </IonHeader>

            <IonContent className="wf-content">
                <div className="roster-calendar-card">
                    <div className="roster-weekdays">
                        {weekdayLabels.map(label => <span key={label}>{label}</span>)}
                    </div>
                    <div className="roster-calendar-grid">
                        {calendarCells.map(cell => {
                            const dayItems = rostersByDay.get(cell.date) || [];
                            const working = dayItems.filter(item => item.type === 'WORKING').length;
                            const leave = dayItems.length - working;
                            const isSelected = cell.date === selectedDate;
                            const isToday = cell.date === today();
                            return (
                                <button
                                    className={[
                                        'roster-day-cell',
                                        cell.inMonth ? '' : 'muted',
                                        isSelected ? 'selected' : '',
                                        isToday ? 'today' : '',
                                        dayItems.length ? 'has-roster' : '',
                                    ].filter(Boolean).join(' ')}
                                    key={cell.date}
                                    onClick={() => {
                                        setSelectedDate(cell.date);
                                        if (cell.date.slice(0, 7) !== month) {
                                            const nextMonth = cell.date.slice(0, 7);
                                            setMonth(nextMonth);
                                            loadData(nextMonth);
                                        }
                                    }}
                                >
                                    <span className="roster-day-number">{parseDateKey(cell.date).getDate()}</span>
                                    {dayItems.length > 0 && (
                                        <span className="roster-day-count">{working} ca{leave ? ` · ${leave} nghỉ` : ''}</span>
                                    )}
                                    <span className="roster-day-dots">
                                        {working > 0 && <i className="work" />}
                                        {leave > 0 && <i className="leave" />}
                                    </span>
                                </button>
                            );
                        })}
                    </div>
                    {loading && <div className="roster-calendar-loading"><IonSpinner name="crescent" /></div>}
                </div>

                <div className="wf-list-card roster-day-panel">
                    <div className="roster-selected-head">
                        <div>
                            <div className="wf-section-label">Ca ngày {parseDateKey(selectedDate).toLocaleDateString('vi-VN')}</div>
                            <div className="roster-selected-stats">
                                {selectedStats.employeeCount} nhân viên · {selectedStats.working} ca làm · {selectedStats.leave} nghỉ
                            </div>
                        </div>
                        {canCreateRoster && (
                            <button className="wf-primary-btn" onClick={openCreate}>
                                <IonIcon icon={addOutline} />
                                Thêm ca
                            </button>
                        )}
                    </div>

                    <div className="wf-search roster-day-search">
                        <IonIcon icon={searchOutline} />
                        <input value={query} onChange={e => setQuery(e.target.value)} placeholder="Tìm nhân viên trong ngày này" />
                    </div>

                    {selectedRosters.length === 0 ? (
                        <div className="wf-empty roster-empty">
                            <IonIcon icon={calendarClearOutline} />
                            <span>Ngày này chưa có ca. Chủ cửa hàng có thể thêm ca cho từng nhân viên.</span>
                        </div>
                    ) : selectedRosters.map((item, index) => {
                        const previous = selectedRosters[index - 1];
                        const currentName = item.employeeUsername || employeeName.get(item.employeeId) || `NV #${item.employeeId}`;
                        const previousName = previous ? previous.employeeUsername || employeeName.get(previous.employeeId) || `NV #${previous.employeeId}` : '';
                        const showEmployeeHeader = currentName !== previousName;

                        return (
                            <React.Fragment key={item.id}>
                                {showEmployeeHeader && (
                                    <div className="roster-employee-label">
                                        <IonIcon icon={personOutline} />
                                        <span>{currentName}</span>
                                    </div>
                                )}
                                <div className="wf-row roster-shift-row">
                                    <div className="wf-avatar roster-shift-avatar">
                                        <IonIcon icon={item.type === 'WORKING' ? timeOutline : briefcaseOutline} />
                                    </div>
                                    <div className="wf-row-main">
                                        <div className="wf-row-title">
                                            {item.type === 'WORKING' ? `${item.startTime} - ${item.endTime}` : rosterLabels[item.type]}
                                        </div>
                                        <div className="wf-row-sub">
                                            {item.expectedHours ? `${item.expectedHours} giờ tính công` : rosterLabels[item.type]}
                                        </div>
                                        {item.type === 'WORKING' && (
                                            <div className="wf-row-note">
                                                Cham cong: {formatShiftTime(item.checkInAllowedFrom)} - {formatShiftTime(item.checkInAllowedTo)}
                                            </div>
                                        )}
                                        {item.note && <div className="wf-row-note">{item.note}</div>}
                                    </div>
                                    <span className={`wf-badge ${getBadgeClass(item.type)}`}>{rosterLabels[item.type]}</span>
                                    {(canUpdateRoster || canDeleteRoster) && (
                                        <div className="wf-row-actions">
                                            {canUpdateRoster && <button className="wf-icon-btn" onClick={() => openEdit(item)}><IonIcon icon={createOutline} /></button>}
                                            {canDeleteRoster && <button className="wf-icon-btn" onClick={() => setDeleteTarget(item)}><IonIcon icon={trashOutline} /></button>}
                                        </div>
                                    )}
                                </div>
                            </React.Fragment>
                        );
                    })}
                </div>
            </IonContent>

            <IonModal isOpen={modalOpen} onDidDismiss={() => setModalOpen(false)} className="wf-modal">
                <div className="wf-modal-header roster-modal-header">{editing ? 'Sửa ca làm' : 'Thêm ca làm'}</div>
                <IonContent className="wf-modal-content">
                    <div className="wf-form-card roster-form-card">
                        <div className="wf-form-field">
                            <label>Nhân viên</label>
                            <button className={`roster-picker-trigger ${selectedEmployee ? 'has-value' : ''}`} onClick={() => setEmployeePickerOpen(true)}>
                                <span>{selectedEmployee?.username || 'Chọn nhân viên'}</span>
                                <IonIcon icon={chevronDownOutline} />
                            </button>
                        </div>
                        <div className="wf-form-field">
                            <label>Ngày làm việc</label>
                            <input type="date" value={form.workingDay} onChange={e => setForm({ ...form, workingDay: e.target.value })} />
                        </div>
                        <div className="wf-form-field">
                            <label>Loại lịch</label>
                            <div className="roster-type-segment">
                                {(['WORKING', 'LEAVE_APPROVED', 'LEAVE_UNAPPROVED'] as RosterType[]).map(type => (
                                    <button
                                        key={type}
                                        className={form.type === type ? 'active' : ''}
                                        onClick={() => setForm({ ...form, type })}
                                    >
                                        {rosterLabels[type]}
                                    </button>
                                ))}
                            </div>
                        </div>
                        {form.type === 'WORKING' && (
                            <>
                                <div className="wf-form-grid">
                                    <div className="wf-form-field">
                                        <label>Bắt đầu</label>
                                        <input type="time" value={form.startTime || ''} onChange={e => setForm({ ...form, startTime: e.target.value })} />
                                    </div>
                                    <div className="wf-form-field">
                                        <label>Kết thúc</label>
                                        <input type="time" value={form.endTime || ''} onChange={e => setForm({ ...form, endTime: e.target.value })} />
                                    </div>
                                </div>
                                <div className="wf-form-grid">
                                    <div className="wf-form-field">
                                        <label>Cho cham cong tu</label>
                                        <input type="time" value={form.checkInAllowedFrom || ''} onChange={e => setForm({ ...form, checkInAllowedFrom: e.target.value })} />
                                    </div>
                                    <div className="wf-form-field">
                                        <label>Cho cham cong den</label>
                                        <input type="time" value={form.checkInAllowedTo || ''} onChange={e => setForm({ ...form, checkInAllowedTo: e.target.value })} />
                                    </div>
                                </div>
                                <div className="wf-form-field">
                                    <label>Nghỉ không lương (phút)</label>
                                    <input type="number" min={0} value={form.unpaidBreakMinutes || 0} onChange={e => setForm({ ...form, unpaidBreakMinutes: Number(e.target.value) })} />
                                </div>
                            </>
                        )}
                        <div className="wf-form-field">
                            <label>Ghi chú</label>
                            <textarea value={form.note || ''} onChange={e => setForm({ ...form, note: e.target.value })} placeholder="Ví dụ: Ca sáng, đổi ca, nghỉ có phép..." />
                        </div>
                    </div>
                </IonContent>
                <div className="wf-modal-footer">
                    <button className="wf-ghost-btn" onClick={() => setModalOpen(false)}>Hủy</button>
                    <button className="wf-primary-btn" disabled={saving} onClick={submit}>{saving ? 'Đang lưu...' : 'Lưu'}</button>
                </div>
            </IonModal>

            <IonModal isOpen={employeePickerOpen} onDidDismiss={() => setEmployeePickerOpen(false)} className="roster-picker-modal">
                <div className="roster-picker-header">
                    <button onClick={() => setEmployeePickerOpen(false)}>
                        <IonIcon icon={closeOutline} />
                    </button>
                    <span>Chọn nhân viên</span>
                </div>
                <IonContent className="roster-picker-content">
                    <div className="roster-picker-search">
                        <IonIcon icon={searchOutline} />
                        <input value={employeeSearch} onChange={e => setEmployeeSearch(e.target.value)} placeholder="Tìm tên, số điện thoại, vai trò" />
                    </div>
                    <div className="roster-picker-list">
                        {filteredEmployees.length === 0 ? (
                            <div className="roster-picker-empty">Không tìm thấy nhân viên</div>
                        ) : filteredEmployees.map(emp => (
                            <button
                                className={`roster-picker-item ${form.employeeId === emp.id ? 'active' : ''}`}
                                key={emp.id}
                                onClick={() => {
                                    setForm({ ...form, employeeId: emp.id });
                                    setEmployeePickerOpen(false);
                                }}
                            >
                                <div className="roster-picker-avatar">{(emp.username || 'N')[0]?.toUpperCase()}</div>
                                <div className="roster-picker-info">
                                    <strong>{emp.username || `NV #${emp.id}`}</strong>
                                    <span>{emp.phone || emp.roleName || 'Nhân viên'}</span>
                                </div>
                                {form.employeeId === emp.id && <IonIcon icon={checkmarkOutline} className="roster-picker-check" />}
                            </button>
                        ))}
                    </div>
                </IonContent>
            </IonModal>

            <IonAlert
                isOpen={!!deleteTarget}
                onDidDismiss={() => setDeleteTarget(null)}
                header="Xác nhận xóa"
                message="Bạn có chắc chắn muốn xóa lịch làm việc này không?"
                buttons={[
                    { text: 'Hủy', role: 'cancel' },
                    { text: 'Xóa', role: 'destructive', handler: () => { if (deleteTarget) doRemove(deleteTarget); } }
                ]}
            />

            <IonToast isOpen={toast !== null} message={toast ?? ''} duration={2400} onDidDismiss={() => setToast(null)} />
        </IonPage>
    );
};

export default RosterPage;
