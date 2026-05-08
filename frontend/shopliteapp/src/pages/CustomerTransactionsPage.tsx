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
import { chevronBackOutline, caretDownOutline } from 'ionicons/icons';
import { customerService } from '../services/customer.service';
import type { Order } from '../api/types';
import './CustomerTransactionsPage.css';

const fmt = (n: number) => n.toLocaleString('vi-VN');

const fmtDate = (dString?: string | null) => {
    if (!dString) return '';
    const d = new Date(dString);
    const time = `${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`;
    const date = `${String(d.getDate()).padStart(2, '0')}/${String(d.getMonth() + 1).padStart(2, '0')}/${d.getFullYear()}`;
    return `${date} ${time}`;
};

const methodLabel = (m: string | null | undefined) => {
    switch (m) {
        case 'CASH': return 'Tiền mặt';
        case 'BANK': return 'Chuyển khoản';
        case 'SEPAY_QR': return 'QR Code';
        case 'COD': return 'COD';
        case 'VNPAY': return 'VNPay';
        case 'MOMO': return 'Momo';
        default: return m || 'Tiền mặt';
    }
};

const CustomerTransactionsPage: React.FC = () => {
    const ionRouter = useIonRouter();
    const { id } = useParams<{ id: string }>();

    const [orders, setOrders] = useState<Order[]>([]);
    const [loading, setLoading] = useState(false);

    useIonViewWillEnter(() => {
        if (!id) return;
        setLoading(true);
        customerService.getOrdersByCustomer(Number(id))
            .then(res => setOrders(res))
            .catch(console.error)
            .finally(() => setLoading(false));
    });

    const totalAmount = orders.reduce((sum, o) => sum + (o.totalAmount || 0), 0);

    return (
        <IonPage className="ct-page">
            <IonHeader className="ct-header ion-no-border">
                <IonToolbar className="ct-toolbar">
                    <IonButtons slot="start">
                        <IonButton color="dark" onClick={() => ionRouter.goBack()}>
                            <IonIcon icon={chevronBackOutline} style={{ fontSize: '26px' }} />
                        </IonButton>
                    </IonButtons>
                    <div className="ct-title">Giao dịch</div>
                </IonToolbar>
                
                <div className="ct-summary">
                    <div className="ct-summary-left">
                        <div className="ct-summary-type">
                            Tổng bán - Tổng trả
                            <IonIcon icon={caretDownOutline} className="ct-summary-icon" />
                        </div>
                        <div className="ct-summary-count">{orders.length} giao dịch</div>
                    </div>
                    <div className="ct-summary-right">
                        {fmt(totalAmount)}
                    </div>
                </div>
            </IonHeader>

            <IonContent className="ct-content">
                {loading ? (
                    <div className="ct-loading">
                        <IonSpinner name="crescent" color="primary" />
                    </div>
                ) : (
                    <div className="ct-list">
                        {orders.map(o => (
                            <div key={o.id} className="ct-item">
                                <div className="ct-item-top">
                                    <span className="ct-code">{o.code || `HD${String(o.id).padStart(6, '0')}`}</span>
                                    <span className="ct-amount">{fmt(o.totalAmount || 0)}</span>
                                </div>
                                <div className="ct-item-mid">
                                    <span className="ct-date-user">
                                        {fmtDate(o.createdAt)} • {o.username || 'System'}
                                    </span>
                                    <span className="ct-method">
                                        {methodLabel(o.paymentMethod)}
                                    </span>
                                </div>
                                <div className="ct-item-bot">
                                    {o.items && o.items.length > 0 
                                        ? o.items.map(i => i.productName).join(', ')
                                        : 'Sản phẩm'}
                                </div>
                            </div>
                        ))}
                    </div>
                )}
            </IonContent>
        </IonPage>
    );
};

export default CustomerTransactionsPage;
