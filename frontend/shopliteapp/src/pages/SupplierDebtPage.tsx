import React, { useState } from 'react';
import {
    IonPage,
    IonHeader,
    IonToolbar,
    IonContent,
    IonIcon,
    IonButtons,
    IonButton,
    IonSpinner,
    useIonRouter,
    useIonViewWillEnter,
} from '@ionic/react';
import { useParams } from 'react-router-dom';
import { chevronBackOutline } from 'ionicons/icons';
import { supplierService } from '../services/supplier.service';
import { importOrderService } from '../services/importOrder.service';
import type { Supplier } from '../api/types';
import './CustomerDebtPage.css';

const fmt = (n: number) => n.toLocaleString('vi-VN');

const fmtDate = (dString?: string | null) => {
    if (!dString) return '';
    const d = new Date(dString);
    const time = `${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`;
    const date = `${String(d.getDate()).padStart(2, '0')}/${String(d.getMonth() + 1).padStart(2, '0')}/${d.getFullYear()}`;
    return `${date} ${time}`;
};

type DebtRecord = {
    id: string;
    code: string;
    date: string;
    amount: number;
    debtRemaining: number;
    isPayment: boolean;
};

const SupplierDebtPage: React.FC = () => {
    const ionRouter = useIonRouter();
    const { id } = useParams<{ id: string }>();

    const [supplier, setSupplier] = useState<Supplier | null>(null);
    const [records, setRecords] = useState<DebtRecord[]>([]);
    const [loading, setLoading] = useState(false);
    const [totalDebt, setTotalDebt] = useState(0);

    useIonViewWillEnter(() => {
        if (!id) return;
        setLoading(true);
        Promise.all([
            supplierService.getById(id),
            importOrderService.getAll()
        ]).then(([sup, allOrders]) => {
            if (sup) setSupplier(sup);
            
            const ords = allOrders.filter(o => o.supplierId === Number(id));

            // Build debt records
            const sortedOrders = [...ords].sort((a, b) => 
                new Date(a.createdAt || 0).getTime() - new Date(b.createdAt || 0).getTime()
            );

            const recs: DebtRecord[] = [];
            let currentDebt = 0;

            for (const o of sortedOrders) {
                const amt = o.totalAmount || 0;
                
                // Order entry (Nhập hàng làm tăng công nợ phải trả)
                currentDebt += amt;
                recs.push({
                    id: `order-${o.id}`,
                    code: `PN${String(o.id).padStart(6, '0')}`,
                    date: o.createdAt || '',
                    amount: amt,
                    debtRemaining: currentDebt,
                    isPayment: false
                });

                // Payment entry
                if (o.status === 'COMPLETED' || (o.amountPaid && o.amountPaid > 0)) {
                    const paidAmt = o.amountPaid || amt; 
                    currentDebt -= paidAmt;
                    recs.push({
                        id: `pay-${o.id}`,
                        code: `PC${String(o.id).padStart(6, '0')}`,
                        date: o.createdAt || '',
                        amount: -paidAmt,
                        debtRemaining: currentDebt,
                        isPayment: true
                    });
                }
            }
            
            setTotalDebt(currentDebt);
            setRecords(recs.reverse());
        }).catch(console.error)
          .finally(() => setLoading(false));
    });

    return (
        <IonPage className="cdebt-page">
            <IonHeader className="cdebt-header ion-no-border">
                <IonToolbar className="cdebt-toolbar">
                    <IonButtons slot="start">
                        <IonButton color="dark" onClick={() => ionRouter.goBack()}>
                            <IonIcon icon={chevronBackOutline} style={{ fontSize: '26px' }} />
                        </IonButton>
                    </IonButtons>
                    <div className="cdebt-title-wrap">
                        <div className="cdebt-title">Công nợ</div>
                        {supplier && (
                            <div className="cdebt-subtitle">{supplier.name}</div>
                        )}
                    </div>
                </IonToolbar>

                <div className="cdebt-summary">
                    <div className="cdebt-summary-left">
                        <div className="cdebt-summary-label">Nợ cần trả</div>
                        <div className="cdebt-summary-count">{records.length} giao dịch</div>
                    </div>
                    <div className="cdebt-summary-right">
                        {fmt(totalDebt)}
                    </div>
                </div>
            </IonHeader>

            <IonContent className="cdebt-content">
                {loading ? (
                    <div className="cdebt-loading">
                        <IonSpinner name="crescent" color="primary" />
                    </div>
                ) : (
                    <div className="cdebt-list">
                        {records.map(r => (
                            <div key={r.id} className="cdebt-item">
                                <div className="cdebt-item-left">
                                    <div className="cdebt-code">{r.code}</div>
                                    <div className="cdebt-date">{fmtDate(r.date)}</div>
                                </div>
                                <div className="cdebt-item-right">
                                    <div className={`cdebt-amount ${r.isPayment ? 'cdebt-amount-green' : ''}`}>
                                        {r.isPayment ? fmt(r.amount) : fmt(r.amount)}
                                    </div>
                                    <div className="cdebt-remaining">
                                        Nợ còn: {fmt(r.debtRemaining)}
                                    </div>
                                    {r.isPayment && (
                                        <div className="cdebt-tag-wrap">
                                            <span className="cdebt-tag">Thanh toán</span>
                                        </div>
                                    )}
                                </div>
                            </div>
                        ))}
                        <div style={{ height: 100 }} />
                    </div>
                )}
            </IonContent>

            <div className="cdebt-footer">
                <button className="cdebt-btn cdebt-btn-outline">Điều chỉnh</button>
                <button className="cdebt-btn cdebt-btn-primary">Thanh toán</button>
            </div>
        </IonPage>
    );
};

export default SupplierDebtPage;
