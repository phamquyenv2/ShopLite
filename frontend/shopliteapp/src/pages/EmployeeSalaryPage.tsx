import React, { useCallback, useState } from 'react';
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
    addOutline,
    chevronDownOutline,
    closeOutline,
    removeOutline,
} from 'ionicons/icons';
import type { Employee } from '../api/types';
import { employeeService } from '../services/employee.service';
import type { EmployeeUpdatePayload } from '../services/employee.service';
import './EmployeeSalaryPage.css';

// ── types ─────────────────────────────────────────────────────────────────────
type SalaryType = 'HOURLY' | 'DAILY' | 'MONTHLY' | '';

interface SalaryConfig {
    salaryType: SalaryType;
    baseRate: string;
    bonus: string;
    commission: string;
    allowance: string;
    deduction: string;
}

interface ExpandState {
    bonus: boolean;
    commission: boolean;
    allowance: boolean;
    deduction: boolean;
}

const SALARY_TYPE_OPTIONS: { value: SalaryType; label: string; unit: string }[] = [
    { value: 'HOURLY',  label: 'Lương theo giờ',  unit: 'giờ' },
    { value: 'DAILY',   label: 'Lương theo ngày',  unit: 'ngày' },
    { value: 'MONTHLY', label: 'Lương theo tháng', unit: 'tháng' },
];

const blankConfig = (): SalaryConfig => ({
    salaryType: '', baseRate: '', bonus: '', commission: '', allowance: '', deduction: '',
});

const parseNote = (note?: string | null): Partial<SalaryConfig> => {
    if (!note) return {};
    try {
        const p = JSON.parse(note);
        return {
            salaryType: p.salaryType ?? '',
            bonus:      p.bonus      != null ? String(p.bonus)      : '',
            commission: p.commission != null ? String(p.commission) : '',
            allowance:  p.allowance  != null ? String(p.allowance)  : '',
            deduction:  p.deduction  != null ? String(p.deduction)  : '',
        };
    } catch { return {}; }
};

const fmtVND = (val: string) => {
    const n = parseFloat(val);
    if (isNaN(n)) return '';
    return new Intl.NumberFormat('vi-VN').format(n) + ' đ';
};

// ── component ─────────────────────────────────────────────────────────────────
const EmployeeSalaryPage: React.FC = () => {
    const router = useIonRouter();
    const { id } = useParams<{ id: string }>();
    const employeeId = Number(id);

    const [employee, setEmployee] = useState<Employee | null>(null);
    const [loading, setLoading] = useState(true);
    const [saving, setSaving] = useState(false);
    const [toast, setToast] = useState<string | null>(null);
    const [config, setConfig] = useState<SalaryConfig>(blankConfig());
    const [expanded, setExpanded] = useState<ExpandState>({
        bonus: false, commission: false, allowance: false, deduction: false,
    });

    const loadEmployee = useCallback(async () => {
        setLoading(true);
        try {
            const emps = await employeeService.getEmployees();
            const emp = emps.find(e => e.id === employeeId) ?? null;
            setEmployee(emp);
            if (emp) {
                const n = parseNote(emp.note);
                setConfig({
                    salaryType: n.salaryType ?? '',
                    baseRate:   String(emp.salaryRate > 0 ? emp.salaryRate : ''),
                    bonus:      n.bonus      ?? '',
                    commission: n.commission ?? '',
                    allowance:  n.allowance  ?? '',
                    deduction:  n.deduction  ?? '',
                });
                setExpanded({
                    bonus:      (n.bonus      ?? '') !== '',
                    commission: (n.commission ?? '') !== '',
                    allowance:  (n.allowance  ?? '') !== '',
                    deduction:  (n.deduction  ?? '') !== '',
                });
            }
        } catch (err: any) {
            setToast(err.message || 'Không thể tải thông tin nhân viên');
        } finally {
            setLoading(false);
        }
    }, [employeeId]);

    useIonViewWillEnter(() => { loadEmployee(); });

    const save = async () => {
        if (!employee) return;
        if (!config.salaryType) { setToast('Vui lòng chọn loại lương'); return; }
        const rate = parseFloat(config.baseRate);
        if (isNaN(rate) || rate < 0) { setToast('Mức lương phải >= 0'); return; }
        if (!employee.officeId) { setToast('Nhân viên chưa được gán vào văn phòng'); return; }

        const noteObj: Record<string, unknown> = { salaryType: config.salaryType };
        if (config.bonus      !== '') noteObj.bonus      = parseFloat(config.bonus)      || 0;
        if (config.commission !== '') noteObj.commission = parseFloat(config.commission) || 0;
        if (config.allowance  !== '') noteObj.allowance  = parseFloat(config.allowance)  || 0;
        if (config.deduction  !== '') noteObj.deduction  = parseFloat(config.deduction)  || 0;

        setSaving(true);
        try {
            const payload: EmployeeUpdatePayload = {
                userId:     employee.userId,
                officeId:   employee.officeId,
                salaryRate: rate,
                qr:         employee.qr ?? null,
                note:       JSON.stringify(noteObj),
            };
            const updated = await employeeService.updateEmployee(employee.id, payload);
            setEmployee(updated);
            setToast('Đã lưu thiết lập lương');
            setTimeout(() => router.goBack(), 900);
        } catch (err: any) {
            setToast(err.message || 'Không thể lưu lương');
        } finally {
            setSaving(false);
        }
    };

    const set = (key: keyof SalaryConfig, value: string) =>
        setConfig(prev => ({ ...prev, [key]: value }));

    const toggleExpand = (key: keyof ExpandState) =>
        setExpanded(prev => ({ ...prev, [key]: !prev[key] }));

    const selectedType = SALARY_TYPE_OPTIONS.find(o => o.value === config.salaryType);

    const hasSummary = config.salaryType && config.baseRate;

    // ── render ────────────────────────────────────────────────────────────────
    return (
        <IonPage className="salary-page">
            <IonHeader className="sp-header ion-no-border">
                <IonToolbar className="sp-toolbar">
                    <IonButtons slot="start">
                        <IonButton className="sp-close-btn" fill="clear" onClick={() => router.goBack()}>
                            <IonIcon icon={closeOutline} style={{ fontSize: 24, color: '#374151' }} />
                        </IonButton>
                    </IonButtons>

                    <div className="sp-title-block">
                        <div className="sp-title">Tạo thiết lập lương</div>
                        {employee && <div className="sp-subtitle">{employee.username}</div>}
                    </div>

                    <IonButtons slot="end">
                        <button
                            className="sp-save-header-btn"
                            disabled={saving || loading}
                            onClick={save}
                        >
                            {saving ? '...' : 'Lưu'}
                        </button>
                    </IonButtons>
                </IonToolbar>
            </IonHeader>

            <IonContent className="sp-content">
                {loading ? (
                    <div className="sp-loading"><IonSpinner name="crescent" /></div>
                ) : !employee ? (
                    <div className="sp-empty">Không tìm thấy nhân viên.</div>
                ) : (
                    <>
                        {/* ── Loại lương và mức lương ── */}
                        <div className="sp-section-label">LOẠI LƯƠNG VÀ MỨC LƯƠNG</div>

                        <div className="sp-card">
                            {/* salary type select */}
                            <div className={`sp-select-wrap ${config.salaryType ? 'has-value' : ''}`}>
                                <select
                                    className="sp-select"
                                    value={config.salaryType}
                                    onChange={e => set('salaryType', e.target.value as SalaryType)}
                                >
                                    <option value="" disabled>Loại lương</option>
                                    {SALARY_TYPE_OPTIONS.map(o => (
                                        <option key={o.value} value={o.value}>{o.label}</option>
                                    ))}
                                </select>
                                <IonIcon icon={chevronDownOutline} className="sp-select-icon" />
                            </div>

                            {/* base rate — shows after type selected */}
                            {config.salaryType && (
                                <>
                                    <div className="sp-field-divider" />
                                    <div className="sp-rate-field">
                                        <label className="sp-rate-label">
                                            Mức lương (đ / {selectedType?.unit})
                                        </label>
                                        <div className="sp-rate-input-wrap">
                                            <input
                                                type="number"
                                                min={0}
                                                inputMode="decimal"
                                                placeholder="Nhập mức lương"
                                                className="sp-rate-input"
                                                value={config.baseRate}
                                                onChange={e => set('baseRate', e.target.value)}
                                            />
                                            <span className="sp-rate-unit">đ</span>
                                        </div>
                                        {config.baseRate && !isNaN(parseFloat(config.baseRate)) && (
                                            <div className="sp-rate-hint">{fmtVND(config.baseRate)}</div>
                                        )}
                                    </div>
                                </>
                            )}
                        </div>

                        <div className="sp-gap" />

                        {/* ── Thưởng ── */}
                        <div className="sp-expand-card">
                            <button className="sp-expand-trigger" onClick={() => toggleExpand('bonus')}>
                                <IonIcon icon={expanded.bonus ? removeOutline : addOutline} className="sp-expand-icon" />
                                <span>{expanded.bonus && config.bonus ? `Thưởng: ${fmtVND(config.bonus)}` : 'Thêm thưởng'}</span>
                            </button>
                            {expanded.bonus && (
                                <div className="sp-expand-body">
                                    <div className="sp-rate-input-wrap">
                                        <input
                                            type="number" min={0} inputMode="decimal"
                                            placeholder="Số tiền thưởng"
                                            className="sp-rate-input"
                                            value={config.bonus}
                                            onChange={e => set('bonus', e.target.value)}
                                        />
                                        <span className="sp-rate-unit">đ</span>
                                    </div>
                                    <div className="sp-expand-hint">Thưởng sẽ được cộng vào tổng lương khi tính phiếu lương.</div>
                                </div>
                            )}
                        </div>

                        <div className="sp-gap" />

                        {/* ── Hoa hồng ── */}
                        <div className="sp-expand-card">
                            <button className="sp-expand-trigger" onClick={() => toggleExpand('commission')}>
                                <IonIcon icon={expanded.commission ? removeOutline : addOutline} className="sp-expand-icon" />
                                <span>{expanded.commission && config.commission ? `Hoa hồng: ${fmtVND(config.commission)}` : 'Thêm hoa hồng'}</span>
                            </button>
                            {expanded.commission && (
                                <div className="sp-expand-body">
                                    <div className="sp-rate-input-wrap">
                                        <input
                                            type="number" min={0} inputMode="decimal"
                                            placeholder="Số tiền hoa hồng"
                                            className="sp-rate-input"
                                            value={config.commission}
                                            onChange={e => set('commission', e.target.value)}
                                        />
                                        <span className="sp-rate-unit">đ</span>
                                    </div>
                                    <div className="sp-expand-hint">Hoa hồng cố định được cộng thêm vào tổng lương.</div>
                                </div>
                            )}
                        </div>

                        <div className="sp-gap" />

                        {/* ── Phụ cấp ── */}
                        <div className="sp-expand-card">
                            <button className="sp-expand-trigger" onClick={() => toggleExpand('allowance')}>
                                <IonIcon icon={expanded.allowance ? removeOutline : addOutline} className="sp-expand-icon" />
                                <span>{expanded.allowance && config.allowance ? `Phụ cấp: ${fmtVND(config.allowance)}` : 'Thêm phụ cấp'}</span>
                            </button>
                            {expanded.allowance && (
                                <div className="sp-expand-body">
                                    <div className="sp-rate-input-wrap">
                                        <input
                                            type="number" min={0} inputMode="decimal"
                                            placeholder="Số tiền phụ cấp"
                                            className="sp-rate-input"
                                            value={config.allowance}
                                            onChange={e => set('allowance', e.target.value)}
                                        />
                                        <span className="sp-rate-unit">đ</span>
                                    </div>
                                    <div className="sp-expand-hint">Phụ cấp ăn, đi lại, xăng xe... được cộng vào lương.</div>
                                </div>
                            )}
                        </div>

                        <div className="sp-gap" />

                        {/* ── Giảm trừ ── */}
                        <div className="sp-expand-card">
                            <button className="sp-expand-trigger" onClick={() => toggleExpand('deduction')}>
                                <IonIcon icon={expanded.deduction ? removeOutline : addOutline} className="sp-expand-icon" />
                                <span>{expanded.deduction && config.deduction ? `Giảm trừ: ${fmtVND(config.deduction)}` : 'Thêm giảm trừ'}</span>
                            </button>
                            {expanded.deduction && (
                                <div className="sp-expand-body">
                                    <div className="sp-rate-input-wrap">
                                        <input
                                            type="number" min={0} inputMode="decimal"
                                            placeholder="Số tiền giảm trừ"
                                            className="sp-rate-input"
                                            value={config.deduction}
                                            onChange={e => set('deduction', e.target.value)}
                                        />
                                        <span className="sp-rate-unit">đ</span>
                                    </div>
                                    <div className="sp-expand-hint">Số tiền bị trừ khỏi tổng lương (vi phạm, tạm ứng...).</div>
                                </div>
                            )}
                        </div>

                        {/* ── Ước tính lương ── */}
                        {hasSummary && (
                            <>
                                <div className="sp-summary-label">ƯỚC TÍNH LƯƠNG</div>
                                <div className="sp-summary-card">
                                    <div className="sp-summary-row">
                                        <span>Lương cơ bản</span>
                                        <span>{fmtVND(config.baseRate)}</span>
                                    </div>
                                    {config.bonus && (
                                        <div className="sp-summary-row green">
                                            <span>Thưởng</span>
                                            <span>+ {fmtVND(config.bonus)}</span>
                                        </div>
                                    )}
                                    {config.commission && (
                                        <div className="sp-summary-row green">
                                            <span>Hoa hồng</span>
                                            <span>+ {fmtVND(config.commission)}</span>
                                        </div>
                                    )}
                                    {config.allowance && (
                                        <div className="sp-summary-row green">
                                            <span>Phụ cấp</span>
                                            <span>+ {fmtVND(config.allowance)}</span>
                                        </div>
                                    )}
                                    {config.deduction && (
                                        <div className="sp-summary-row red">
                                            <span>Giảm trừ</span>
                                            <span>- {fmtVND(config.deduction)}</span>
                                        </div>
                                    )}
                                </div>
                            </>
                        )}

                        <div style={{ height: 48 }} />
                    </>
                )}
            </IonContent>

            <IonToast
                isOpen={toast !== null}
                message={toast ?? ''}
                duration={2400}
                onDidDismiss={() => setToast(null)}
            />
        </IonPage>
    );
};

export default EmployeeSalaryPage;
