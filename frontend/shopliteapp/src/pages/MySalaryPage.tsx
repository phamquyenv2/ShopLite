import React, { useState } from 'react';
import {
    IonContent,
    IonIcon,
    IonPage,
    IonSpinner,
    useIonRouter,
    useIonViewWillEnter,
} from '@ionic/react';
import { chevronBackOutline } from 'ionicons/icons';
import type { EmployeeSalaryHistory } from '../api/types';
import { employeeSalaryService } from '../services/employeeSalary.service';
import './EmployeeSalaryPage.css';

const fmtVND = (val: string | number | null | undefined): string => {
    const n = typeof val === 'number' ? val : parseFloat(String(val ?? ''));
    if (isNaN(n)) return '';
    return new Intl.NumberFormat('vi-VN').format(n) + ' đ';
};

const fmtDate = (dateStr: string): string => {
    if (!dateStr) return '';
    const [y, m, d] = dateStr.split('-');
    return `${d}/${m}/${y}`;
};

const typeLabel = (t: string) => {
    if (t === 'HOURLY') return 'Lương theo giờ';
    if (t === 'DAILY') return 'Lương theo ngày';
    if (t === 'MONTHLY') return 'Lương theo tháng';
    return t;
};

const MySalaryPage: React.FC = () => {
    const router = useIonRouter();
    const [current, setCurrent] = useState<EmployeeSalaryHistory | null>(null);
    const [history, setHistory] = useState<EmployeeSalaryHistory[]>([]);
    const [loading, setLoading] = useState(true);

    useIonViewWillEnter(() => {
        const load = async () => {
            setLoading(true);
            try {
                const [c, h] = await Promise.all([
                    employeeSalaryService.getMine().catch(() => null),
                    employeeSalaryService.getMyHistory().catch(() => []),
                ]);
                setCurrent(c);
                setHistory(h);
            } catch (err) {
                console.error(err);
            } finally {
                setLoading(false);
            }
        };
        load();
    });

    return (
        <IonPage className="esp-page">
            <div className="esp-header">
                <button className="esp-header-close" onClick={() => router.goBack()}>
                    <IonIcon icon={chevronBackOutline} />
                </button>
                <div className="esp-header-center">
                    <div className="esp-header-title">Mức lương của tôi</div>
                </div>
                <div style={{ width: 44 }}></div>
            </div>

            <IonContent className="esp-content">
                {loading ? (
                    <div className="esp-loading">
                        <IonSpinner name="crescent" color="primary" />
                    </div>
                ) : !current ? (
                    <div className="esp-empty">Chưa có thiết lập lương nào cho tài khoản này.</div>
                ) : (
                    <>
                        <div className="esp-section-label">THÔNG TIN LƯƠNG HIỆN TẠI</div>
                        <div className="esp-summary-card">
                            <div className="esp-summary-row">
                                <span style={{ fontWeight: 600 }}>{typeLabel(current.salaryType)}</span>
                                <span style={{ fontWeight: 600 }}>{fmtVND(current.baseRate)}</span>
                            </div>
                            <div className="esp-divider" style={{ margin: '8px 0' }} />
                            {current.recurringBonus > 0 && (
                                <div className="esp-summary-row esp-green">
                                    <span>Thưởng cố định</span>
                                    <span>+ {fmtVND(current.recurringBonus)}</span>
                                </div>
                            )}
                            {current.commission > 0 && (
                                <div className="esp-summary-row esp-green">
                                    <span>Hoa hồng</span>
                                    <span>+ {fmtVND(current.commission)}</span>
                                </div>
                            )}
                            {current.allowance > 0 && (
                                <div className="esp-summary-row esp-green">
                                    <span>Phụ cấp</span>
                                    <span>+ {fmtVND(current.allowance)}</span>
                                </div>
                            )}
                            {current.recurringDeduction > 0 && (
                                <div className="esp-summary-row esp-red">
                                    <span>Giảm trừ</span>
                                    <span>- {fmtVND(current.recurringDeduction)}</span>
                                </div>
                            )}
                            {current.recurringBonus === 0 && current.commission === 0 && current.allowance === 0 && current.recurringDeduction === 0 && (
                                <div className="esp-summary-row" style={{ color: '#6b7280', fontStyle: 'italic', fontSize: '14px' }}>
                                    Không có phụ cấp / giảm trừ.
                                </div>
                            )}
                            {current.reason && (
                                <div style={{ fontSize: '13px', color: '#6b7280', marginTop: 12 }}>
                                    Lý do: {current.reason}
                                </div>
                            )}
                        </div>

                        {history.length > 0 && (
                            <>
                                <div className="esp-section-label">LỊCH SỬ THAY ĐỔI</div>
                                <div className="esp-summary-card">
                                    {history.map((item, index) => (
                                        <div key={item.id}>
                                            {index > 0 && <div className="esp-divider" style={{ margin: '12px 0' }} />}
                                            <div className="esp-summary-row" style={{ flexWrap: 'wrap', marginBottom: 4 }}>
                                                <div style={{ width: '100%', fontSize: '13px', color: '#3b82f6', marginBottom: 4, fontWeight: 500 }}>
                                                    Từ {fmtDate(item.effectiveFrom)} {item.current ? '(Hiện tại)' : ''}
                                                </div>
                                                <span>{typeLabel(item.salaryType)}</span>
                                                <span style={{ fontWeight: 500 }}>{fmtVND(item.baseRate)}</span>
                                                {item.reason && (
                                                    <div style={{ width: '100%', fontSize: '13px', color: '#6b7280', marginTop: 4 }}>
                                                        Lý do: {item.reason}
                                                    </div>
                                                )}
                                            </div>
                                        </div>
                                    ))}
                                </div>
                            </>
                        )}
                        <div className="esp-bottom-spacer" />
                    </>
                )}
            </IonContent>
        </IonPage>
    );
};

export default MySalaryPage;
