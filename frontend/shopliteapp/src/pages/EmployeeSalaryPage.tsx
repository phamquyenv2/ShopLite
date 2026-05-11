import React, { useCallback, useState } from 'react';
import {
    IonContent,
    IonIcon,
    IonPage,
    IonSpinner,
    IonToast,
    useIonActionSheet,
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
import type { Employee, EmployeeSalaryHistory, SalaryType } from '../api/types';
import { employeeService } from '../services/employee.service';
import { employeeSalaryService } from '../services/employeeSalary.service';
import './EmployeeSalaryPage.css';

// ─── Types ────────────────────────────────────────────────────────────────────

type SalaryTypeOption = SalaryType | '';

interface SalaryConfig {
    salaryType: SalaryTypeOption;
    baseRate: string;
    bonus: string;
    commission: string;
    allowance: string;
    deduction: string;
    effectiveFrom: string;
    reason: string;
}

interface ExpandState {
    bonus: boolean;
    commission: boolean;
    allowance: boolean;
    deduction: boolean;
}

// ─── Constants ────────────────────────────────────────────────────────────────

const SALARY_TYPE_OPTIONS: { value: SalaryType; label: string; unit: string }[] = [
    { value: 'HOURLY', label: 'Lương theo giờ', unit: 'giờ' },
    { value: 'DAILY', label: 'Lương theo ngày', unit: 'ngày' },
    { value: 'MONTHLY', label: 'Lương theo tháng', unit: 'tháng' },
];

// ─── Helpers ──────────────────────────────────────────────────────────────────

const todayStr = () => new Date().toISOString().slice(0, 10);

const blankConfig = (): SalaryConfig => ({
    salaryType: '',
    baseRate: '',
    bonus: '',
    commission: '',
    allowance: '',
    deduction: '',
    effectiveFrom: todayStr(),
    reason: '',
});

const fmtVND = (val: string | number | null | undefined): string => {
    const n = typeof val === 'number' ? val : parseFloat(String(val ?? ''));
    if (isNaN(n)) return '';
    return new Intl.NumberFormat('vi-VN').format(n) + ' đ';
};

const toInputValue = (val: number | null | undefined): string =>
    val != null && val > 0 ? String(val) : '';

const fmtHistoryDate = (dateStr: string): string => {
    const [y, m, d] = dateStr.split('-');
    return `${d}/${m}/${y}`;
};

// ─── Main Component ───────────────────────────────────────────────────────────

const EmployeeSalaryPage: React.FC = () => {
    const router = useIonRouter();
    const { id } = useParams<{ id: string }>();
    const employeeId = Number(id);

    const [employee, setEmployee] = useState<Employee | null>(null);
    const [salaryHistory, setSalaryHistory] = useState<EmployeeSalaryHistory[]>([]);
    const [loading, setLoading] = useState(true);
    const [saving, setSaving] = useState(false);
    const [toast, setToast] = useState<string | null>(null);
    const [config, setConfig] = useState<SalaryConfig>(blankConfig());
    const [expanded, setExpanded] = useState<ExpandState>({
        bonus: false,
        commission: false,
        allowance: false,
        deduction: false,
    });

    const [presentActionSheet] = useIonActionSheet();

    // ── Load data ──────────────────────────────────────────────────────────────
    const loadData = useCallback(async () => {
        setLoading(true);
        try {
            const [emps, histories] = await Promise.all([
                employeeService.getEmployees(),
                employeeSalaryService.getHistory(employeeId).catch(() => [] as EmployeeSalaryHistory[]),
            ]);
            const emp = emps.find(e => e.id === employeeId) ?? null;
            const current = histories.find(item => item.current) ?? histories[0] ?? null;

            setEmployee(emp);
            setSalaryHistory(histories);

            if (emp) {
                setConfig({
                    salaryType: current?.salaryType ?? '',
                    baseRate: toInputValue(current?.baseRate ?? emp.salaryRate),
                    bonus: toInputValue(current?.recurringBonus),
                    commission: toInputValue(current?.commission),
                    allowance: toInputValue(current?.allowance),
                    deduction: toInputValue(current?.recurringDeduction),
                    effectiveFrom: todayStr(),
                    reason: '',
                });
                setExpanded({
                    bonus: !!(current?.recurringBonus),
                    commission: !!(current?.commission),
                    allowance: !!(current?.allowance),
                    deduction: !!(current?.recurringDeduction),
                });
            }
        } catch (err: any) {
            setToast(err.message || 'Không thể tải thông tin nhân viên');
        } finally {
            setLoading(false);
        }
    }, [employeeId]);

    useIonViewWillEnter(() => { loadData(); });

    // ── Save ───────────────────────────────────────────────────────────────────
    const handleSave = async () => {
        if (!employee) return;
        if (!config.salaryType) {
            setToast('Vui lòng chọn loại lương');
            return;
        }
        const rate = parseFloat(config.baseRate);
        if (isNaN(rate) || rate < 0) {
            setToast('Mức lương phải >= 0');
            return;
        }

        setSaving(true);
        try {
            const saved = await employeeSalaryService.create(employee.id, {
                salaryType: config.salaryType,
                baseRate: rate,
                recurringBonus: config.bonus !== '' ? parseFloat(config.bonus) || 0 : 0,
                commission: config.commission !== '' ? parseFloat(config.commission) || 0 : 0,
                allowance: config.allowance !== '' ? parseFloat(config.allowance) || 0 : 0,
                recurringDeduction: config.deduction !== '' ? parseFloat(config.deduction) || 0 : 0,
                effectiveFrom: config.effectiveFrom || null,
                reason: config.reason.trim() || null,
            });
            setSalaryHistory(prev => [saved, ...prev.filter(h => h.id !== saved.id)]);
            setEmployee(prev => prev ? { ...prev, salaryRate: saved.baseRate } : prev);
            setToast('Đã lưu thiết lập lương');
            setTimeout(() => router.goBack(), 900);
        } catch (err: any) {
            setToast(err.message || 'Không thể lưu lương');
        } finally {
            setSaving(false);
        }
    };

    // ── Helpers ────────────────────────────────────────────────────────────────
    const setField = (key: keyof SalaryConfig, value: string) =>
        setConfig(prev => ({ ...prev, [key]: value }));

    const toggleExpand = (key: keyof ExpandState) =>
        setExpanded(prev => ({ ...prev, [key]: !prev[key] }));

    const selectedType = SALARY_TYPE_OPTIONS.find(o => o.value === config.salaryType);
    const hasSummary = !!(config.salaryType && config.baseRate);

    const openSalaryTypeSheet = () => {
        presentActionSheet({
            header: 'Chọn loại lương',
            buttons: [
                ...SALARY_TYPE_OPTIONS.map(o => ({
                    text: o.label,
                    handler: () => setField('salaryType', o.value)
                })),
                {
                    text: 'Đóng',
                    role: 'cancel'
                }
            ]
        });
    };

    // ── Render ─────────────────────────────────────────────────────────────────
    return (
        <IonPage className="esp-page">

            {/* ── Custom Header ── */}
            <div className="esp-header">
                <button className="esp-header-close" onClick={() => router.goBack()}>
                    <IonIcon icon={closeOutline} />
                </button>

                <div className="esp-header-center">
                    <div className="esp-header-title">Tạo thiết lập lương</div>
                    {employee && (
                        <div className="esp-header-sub">{employee.username}</div>
                    )}
                </div>

                <button
                    className={`esp-header-save${saving ? ' esp-saving' : ''}`}
                    disabled={saving || loading}
                    onClick={handleSave}
                >
                    {saving ? <IonSpinner name="crescent" className="esp-save-spinner" /> : 'Lưu'}
                </button>
            </div>

            <IonContent className="esp-content">

                {loading ? (
                    <div className="esp-loading">
                        <IonSpinner name="crescent" color="primary" />
                    </div>
                ) : !employee ? (
                    <div className="esp-empty">Không tìm thấy nhân viên.</div>
                ) : (
                    <>
                        {/* ── LOẠI LƯƠNG VÀ MỨC LƯƠNG ── */}
                        <div className="esp-section-label">LOẠI LƯƠNG VÀ MỨC LƯƠNG</div>

                        <div className="esp-card esp-card-padded">
                            {/* Dropdown chọn loại lương */}
                            <div className={`esp-select-wrap${config.salaryType ? ' selected' : ''}`} onClick={openSalaryTypeSheet} style={{ cursor: 'pointer' }}>
                                <div className="esp-select-display" style={{ padding: '14px 40px 14px 14px', flex: 1, fontSize: '15px', color: config.salaryType ? '#374151' : '#9ca3af' }}>
                                    {config.salaryType ? selectedType?.label : 'Loại lương'}
                                </div>
                                <IonIcon icon={chevronDownOutline} className="esp-select-caret" />
                            </div>

                            {/* Nhập mức lương (hiện khi đã chọn loại) */}
                            {config.salaryType && (
                                <>
                                    <div className="esp-divider" />
                                    <div className="esp-field">
                                        <label className="esp-field-label">
                                            Mức lương (đ&thinsp;/&thinsp;{selectedType?.unit})
                                        </label>
                                        <div className="esp-input-row">
                                            <input
                                                type="number"
                                                min={0}
                                                inputMode="decimal"
                                                placeholder="Nhập mức lương"
                                                className="esp-input"
                                                value={config.baseRate}
                                                onChange={e => setField('baseRate', e.target.value)}
                                            />
                                            <span className="esp-input-unit">đ</span>
                                        </div>
                                        {config.baseRate && !isNaN(parseFloat(config.baseRate)) && (
                                            <div className="esp-input-hint">{fmtVND(config.baseRate)}</div>
                                        )}
                                    </div>
                                </>
                            )}
                        </div>

                        {/* ── Ngày áp dụng & Lý do ── */}
                        <div className="esp-card">
                            <div className="esp-row-field">
                                <span className="esp-row-label">Ngày áp dụng</span>
                                <input
                                    type="date"
                                    className="esp-row-date"
                                    value={config.effectiveFrom}
                                    onChange={e => setField('effectiveFrom', e.target.value)}
                                />
                            </div>
                            <div className="esp-divider" />
                            <div className="esp-row-field">
                                <span className="esp-row-label">Lý do thay đổi</span>
                                <input
                                    className="esp-row-input"
                                    placeholder="Tăng lương, điều chỉnh phụ cấp..."
                                    value={config.reason}
                                    onChange={e => setField('reason', e.target.value)}
                                />
                            </div>
                        </div>

                        {/* ── Expandable addon cards ── */}
                        <AddonCard
                            expanded={expanded.bonus}
                            label="thưởng"
                            displayLabel="Thêm thưởng"
                            value={config.bonus}
                            onToggle={() => toggleExpand('bonus')}
                            onChange={v => setField('bonus', v)}
                        />
                        <AddonCard
                            expanded={expanded.commission}
                            label="hoa hồng"
                            displayLabel="Thêm hoa hồng"
                            value={config.commission}
                            onToggle={() => toggleExpand('commission')}
                            onChange={v => setField('commission', v)}
                        />
                        <AddonCard
                            expanded={expanded.allowance}
                            label="phụ cấp"
                            displayLabel="Thêm phụ cấp"
                            value={config.allowance}
                            onToggle={() => toggleExpand('allowance')}
                            onChange={v => setField('allowance', v)}
                        />
                        <AddonCard
                            expanded={expanded.deduction}
                            label="giảm trừ"
                            displayLabel="Thêm giảm trừ"
                            value={config.deduction}
                            onToggle={() => toggleExpand('deduction')}
                            onChange={v => setField('deduction', v)}
                            isDeduction
                        />

                        {/* ── Ước tính lương ── */}
                        {hasSummary && (
                            <>
                                <div className="esp-section-label">ƯỚC TÍNH LƯƠNG</div>
                                <div className="esp-summary-card">
                                    <div className="esp-summary-row">
                                        <span>Lương cơ bản</span>
                                        <span>{fmtVND(config.baseRate)}</span>
                                    </div>
                                    {config.bonus !== '' && parseFloat(config.bonus) > 0 && (
                                        <div className="esp-summary-row esp-green">
                                            <span>Thưởng cố định</span>
                                            <span>+ {fmtVND(config.bonus)}</span>
                                        </div>
                                    )}
                                    {config.commission !== '' && parseFloat(config.commission) > 0 && (
                                        <div className="esp-summary-row esp-green">
                                            <span>Hoa hồng</span>
                                            <span>+ {fmtVND(config.commission)}</span>
                                        </div>
                                    )}
                                    {config.allowance !== '' && parseFloat(config.allowance) > 0 && (
                                        <div className="esp-summary-row esp-green">
                                            <span>Phụ cấp</span>
                                            <span>+ {fmtVND(config.allowance)}</span>
                                        </div>
                                    )}
                                    {config.deduction !== '' && parseFloat(config.deduction) > 0 && (
                                        <div className="esp-summary-row esp-red">
                                            <span>Giảm trừ</span>
                                            <span>- {fmtVND(config.deduction)}</span>
                                        </div>
                                    )}
                                </div>
                            </>
                        )}

                        {/* ── Lịch sử lương ── */}
                        {salaryHistory.length > 0 && (
                            <>
                                <div className="esp-section-label">LỊCH SỬ LƯƠNG</div>
                                <div className="esp-summary-card">
                                    {salaryHistory.slice(0, 5).map(item => (
                                        <div key={item.id} className="esp-summary-row">
                                            <span>
                                                {fmtHistoryDate(item.effectiveFrom)}
                                                {item.current
                                                    ? ' - hiện tại'
                                                    : item.effectiveTo
                                                        ? ` - ${fmtHistoryDate(item.effectiveTo)}`
                                                        : ''}
                                            </span>
                                            <span>{fmtVND(item.baseRate)}</span>
                                        </div>
                                    ))}
                                </div>
                            </>
                        )}

                        <div className="esp-bottom-spacer" />
                    </>
                )}
            </IonContent>

            <IonToast
                isOpen={toast !== null}
                message={toast ?? ''}
                duration={2400}
                position="bottom"
                onDidDismiss={() => setToast(null)}
            />
        </IonPage>
    );
};

// ─── AddonCard Component ──────────────────────────────────────────────────────

interface AddonCardProps {
    expanded: boolean;
    label: string;
    displayLabel: string;
    value: string;
    onToggle: () => void;
    onChange: (value: string) => void;
    isDeduction?: boolean;
}

const AddonCard: React.FC<AddonCardProps> = ({
    expanded,
    label,
    displayLabel,
    value,
    onToggle,
    onChange,
    isDeduction = false,
}) => {
    const hasValue = value !== '' && !isNaN(parseFloat(value)) && parseFloat(value) > 0;
    const prefix = isDeduction ? '- ' : '+ ';

    return (
        <div className="esp-addon-card">
            <button className="esp-addon-trigger" onClick={onToggle}>
                <IonIcon
                    icon={expanded ? removeOutline : addOutline}
                    className="esp-addon-icon"
                />
                <span>
                    {expanded && hasValue
                        ? `${displayLabel}: ${prefix}${fmtVND(value)}`
                        : displayLabel}
                </span>
            </button>
            {expanded && (
                <div className="esp-addon-body">
                    <div className="esp-input-row esp-input-row--bordered">
                        <input
                            type="number"
                            min={0}
                            inputMode="decimal"
                            placeholder={`Số tiền ${label}`}
                            className="esp-input"
                            value={value}
                            onChange={e => onChange(e.target.value)}
                            autoFocus
                        />
                        <span className="esp-input-unit">đ</span>
                    </div>
                    {hasValue && (
                        <div className={`esp-input-hint${isDeduction ? ' esp-hint-red' : ''}`}>
                            {prefix}{fmtVND(value)}
                        </div>
                    )}
                </div>
            )}
        </div>
    );
};

export default EmployeeSalaryPage;
