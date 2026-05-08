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
    calculatorOutline,
    cashOutline,
    chevronBackOutline,
    refreshOutline,
    searchOutline,
} from 'ionicons/icons';
import type { Employee, Payroll } from '../api/types';
import { employeeService } from '../services/employee.service';
import { payrollService } from '../services/payroll.service';
import { useStorePermissions } from '../utils/useStorePermissions';
import './WorkforcePages.css';

const currentMonth = () => {
    const now = new Date();
    return `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}`;
};

const toPeriodDate = (month: string) => `${month}-01`;

const money = (value?: number | null) => new Intl.NumberFormat('vi-VN', {
    style: 'currency',
    currency: 'VND',
    maximumFractionDigits: 0,
}).format(value || 0);

const PayrollsPage: React.FC = () => {
    const router = useIonRouter();
    const { can } = useStorePermissions();
    const canViewAllPayrolls = can('/api/v1/payrolls', 'GET');
    const canViewMyPayrolls = can('/api/v1/payrolls/me', 'GET');
    const canViewEmployees = can('/api/v1/employees', 'GET');
    const canSyncPayrolls = can('/api/v1/payrolls/sync-monthly', 'POST');
    const [payrolls, setPayrolls] = useState<Payroll[]>([]);
    const [employees, setEmployees] = useState<Employee[]>([]);
    const [employeeId, setEmployeeId] = useState('');
    const [month, setMonth] = useState(currentMonth());
    const [query, setQuery] = useState('');
    const [bonus, setBonus] = useState(0);
    const [penalty, setPenalty] = useState(0);
    const [penaltyPerAbsent, setPenaltyPerAbsent] = useState(0);
    const [loading, setLoading] = useState(false);
    const [syncing, setSyncing] = useState(false);
    const [toast, setToast] = useState<string | null>(null);

    const filteredPayrolls = useMemo(() => {
        const keyword = query.trim().toLowerCase();
        return payrolls.filter(item => {
            if (canViewAllPayrolls && employeeId && item.employeeId !== Number(employeeId)) return false;
            if (!item.period?.startsWith(month)) return false;
            if (!keyword) return true;
            return (item.employeeUsername || '').toLowerCase().includes(keyword)
                || String(item.employeeId).includes(keyword);
        });
    }, [canViewAllPayrolls, employeeId, month, payrolls, query]);

    const totals = useMemo(() => ({
        rows: filteredPayrolls.length,
        salary: filteredPayrolls.reduce((sum, item) => sum + (item.totalSalary || 0), 0),
        hours: filteredPayrolls.reduce((sum, item) => sum + (item.totalHours || 0), 0),
    }), [filteredPayrolls]);

    const loadData = async () => {
        if (!canViewAllPayrolls && !canViewMyPayrolls) {
            setPayrolls([]);
            setEmployees([]);
            return;
        }
        setLoading(true);
        try {
            const [list, emps] = await Promise.all([
                canViewAllPayrolls ? payrollService.getAll() : payrollService.getMine(),
                canViewAllPayrolls && canViewEmployees ? employeeService.getEmployees() : Promise.resolve([]),
            ]);
            setPayrolls(list);
            setEmployees(emps.filter(emp => !emp.deleted));
        } catch (err: any) {
            setToast(err.message || 'Không thể tải bảng lương');
        } finally {
            setLoading(false);
        }
    };

    useIonViewWillEnter(() => {
        loadData();
    });

    const sync = async () => {
        setSyncing(true);
        try {
            const result = await payrollService.syncMonthly({
                employeeId: employeeId ? Number(employeeId) : null,
                period: toPeriodDate(month),
                bonus,
                penalty,
                penaltyPerAbsent,
            });
            setPayrolls(prev => {
                const merged = new Map(prev.map(item => [item.id, item]));
                result.forEach(item => merged.set(item.id, item));
                return Array.from(merged.values()).sort((a, b) => b.period.localeCompare(a.period));
            });
            setToast('Đã tính lại bảng lương');
        } catch (err: any) {
            setToast(err.message || 'Không thể tính bảng lương');
        } finally {
            setSyncing(false);
        }
    };

    return (
        <IonPage className="workforce-page">
            <IonHeader className="wf-header ion-no-border">
                <IonToolbar className="wf-toolbar">
                    <IonButtons slot="start">
                        <IonButton color="dark" onClick={() => router.goBack()}>
                            <IonIcon icon={chevronBackOutline} style={{ fontSize: 26 }} />
                        </IonButton>
                    </IonButtons>
                    <div className="wf-title">Bảng lương</div>
                    <IonButtons slot="end">
                        <IonButton onClick={loadData}>
                            <IonIcon icon={refreshOutline} />
                        </IonButton>
                    </IonButtons>
                </IonToolbar>

                <div className="wf-top-card">
                    <div className="wf-filter-row">
                        <input className="wf-field" type="month" value={month} onChange={e => setMonth(e.target.value)} />
                        {canSyncPayrolls && <button className="wf-primary-btn" disabled={syncing} onClick={sync}>
                            <IonIcon icon={calculatorOutline} />
                            Tính lương
                        </button>}
                    </div>

                    {canViewAllPayrolls && <div className="wf-filter-row" style={{ marginTop: 10 }}>
                        <select className="wf-field" value={employeeId} onChange={e => setEmployeeId(e.target.value)}>
                            <option value="">Tất cả nhân viên</option>
                            {employees.map(emp => <option key={emp.id} value={emp.id}>{emp.username}</option>)}
                        </select>
                    </div>}

                    <div className="wf-search">
                        <IonIcon icon={searchOutline} />
                        <input value={query} onChange={e => setQuery(e.target.value)} placeholder="Tìm nhân viên trong bảng lương" />
                    </div>

                    <div className="wf-summary">
                        <div className="wf-summary-item">
                            <div className="wf-summary-value">{totals.rows}</div>
                            <div className="wf-summary-label">Nhân viên</div>
                        </div>
                        <div className="wf-summary-item">
                            <div className="wf-summary-value">{totals.hours.toFixed(1)}</div>
                            <div className="wf-summary-label">Giờ công</div>
                        </div>
                        <div className="wf-summary-item">
                            <div className="wf-summary-value">{money(totals.salary).replace('₫', '')}</div>
                            <div className="wf-summary-label">Tổng lương</div>
                        </div>
                    </div>
                </div>
            </IonHeader>

            <IonContent className="wf-content">
                <div className="wf-list-card">
                    <div className="wf-section-label">Thiết lập nhanh</div>
                    <div className="wf-form-card" style={{ marginTop: 0 }}>
                        <div className="wf-form-grid">
                            <div className="wf-form-field">
                                <label>Thưởng chung</label>
                                <input type="number" min={0} value={bonus} onChange={e => setBonus(Number(e.target.value))} />
                            </div>
                            <div className="wf-form-field">
                                <label>Phạt cố định</label>
                                <input type="number" min={0} value={penalty} onChange={e => setPenalty(Number(e.target.value))} />
                            </div>
                        </div>
                        <div className="wf-form-field">
                            <label>Phạt mỗi ngày vắng không phép</label>
                            <input type="number" min={0} value={penaltyPerAbsent} onChange={e => setPenaltyPerAbsent(Number(e.target.value))} />
                        </div>
                    </div>

                    <div className="wf-section-label">Danh sách lương</div>
                    {loading ? <div className="wf-loading"><IonSpinner name="crescent" /></div> : (
                        filteredPayrolls.length === 0 ? (
                            <div className="wf-empty">Chưa có bảng lương tháng này. Bấm “Tính lương” để đồng bộ từ chấm công.</div>
                        ) : filteredPayrolls.map(item => (
                            <div className="wf-row" key={item.id}>
                                <div className="wf-avatar">
                                    <IonIcon icon={cashOutline} />
                                </div>
                                <div className="wf-row-main">
                                    <div className="wf-row-title">{item.employeeUsername || `NV #${item.employeeId}`}</div>
                                    <div className="wf-row-sub">
                                        {item.totalHours || 0} giờ · {money(item.salaryRate)}/giờ
                                    </div>
                                    <div className="wf-row-note">
                                        Đi làm {item.actualPresentDays}/{item.scheduledWorkingDays} ngày · Nghỉ phép {item.approvedLeaveDays} · Vắng {item.absentWithoutLeaveDays}
                                    </div>
                                </div>
                                <div className="wf-money">{money(item.totalSalary)}</div>
                            </div>
                        ))
                    )}
                </div>
            </IonContent>

            <IonToast isOpen={toast !== null} message={toast ?? ''} duration={2400} onDidDismiss={() => setToast(null)} />
        </IonPage>
    );
};

export default PayrollsPage;
