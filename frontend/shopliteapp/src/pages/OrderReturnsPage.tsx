import React, { useMemo, useState } from 'react';
import {
    IonPage, IonHeader, IonToolbar, IonContent, IonIcon, IonButtons, IonButton,
    IonSpinner, IonFab, IonFabButton, useIonRouter, useIonViewWillEnter, IonToast
} from '@ionic/react';
import {
    searchOutline, addOutline, chevronDownOutline, chevronBackOutline
} from 'ionicons/icons';
import { authApis, endpoints } from '../utils/Apis';
import '../pages/Orders.css'; // Reusing Orders.css for exact same format

const fmt = (n?: number | null) => (n ?? 0).toLocaleString('vi-VN');

const DAY_NAMES = ['CHỦ NHẬT', 'THỨ HAI', 'THỨ BA', 'THỨ TƯ', 'THỨ NĂM', 'THỨ SÁU', 'THỨ BẢY'];

const fmtDate = (d: Date) =>
    `${String(d.getDate()).padStart(2, '0')}/${String(d.getMonth() + 1).padStart(2, '0')}/${d.getFullYear()}`;
const fmtTime = (d: Date) =>
    `${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`;
const fmtDayLabel = (d: Date) => `${DAY_NAMES[d.getDay()]}, ${fmtDate(d)}`;

// Dummy data for mockup if API fails
const DUMMY_RETURNS = [
    {
        id: 1,
        orderId: 101,
        orderCode: 'HD000006',
        customerName: 'Phạm Quyền',
        refundAmount: 50000000,
        createdAt: new Date().toISOString(),
        items: [{ productName: 'Iphone 17 promax', quantity: 1 }]
    },
    {
        id: 2,
        orderId: 102,
        orderCode: 'HD000004',
        customerName: 'Khách lẻ',
        refundAmount: 10000,
        createdAt: new Date(Date.now() - 86400000).toISOString(),
        items: [{ productName: 'Bánh mì ngon', quantity: 1 }]
    }
];

const OrderReturnsPage: React.FC = () => {
    const ionRouter = useIonRouter();
    const [returns, setReturns] = useState<any[]>([]);
    const [loading, setLoading] = useState(false);
    const [toast, setToast] = useState<string | null>(null);

    const loadData = async () => {
        setLoading(true);
        try {
            // Attempt to load from real API when ready
            const res = await authApis().get<any>('/api/v1/order-returns');
            const list = Array.isArray(res.data?.data) ? res.data.data : res.data;
            setReturns(list);
        } catch {
            // Fallback to dummy data for mockup
            setReturns(DUMMY_RETURNS);
        } finally {
            setLoading(false);
        }
    };

    useIonViewWillEnter(() => { void loadData(); });

    const grouped = useMemo(() => {
        const map = new Map<string, any[]>();
        const sorted = [...returns].sort((a, b) =>
            new Date(b.createdAt || 0).getTime() - new Date(a.createdAt || 0).getTime()
        );
        sorted.forEach(o => {
            const d = o.createdAt ? new Date(o.createdAt) : new Date();
            const label = fmtDayLabel(d);
            if (!map.has(label)) map.set(label, []);
            map.get(label)!.push(o);
        });
        return map;
    }, [returns]);

    const totalAmount = returns.reduce((s, o) => s + (o.refundAmount ?? 0), 0);

    return (
        <IonPage className="ord-page">
            <IonHeader className="ord-header ion-no-border">
                <div className="ord-top-card">
                    <IonToolbar className="ord-toolbar">
                        <IonButtons slot="start">
                            <IonButton color="dark" onClick={() => ionRouter.goBack()}>
                                <IonIcon icon={chevronBackOutline} style={{ fontSize: '26px' }} />
                            </IonButton>
                        </IonButtons>
                        <div className="ord-title">Phiếu trả hàng</div>
                        <IonButtons slot="end">
                            <IonButton color="dark">
                                <IonIcon icon={searchOutline} style={{ fontSize: '22px' }} />
                            </IonButton>
                        </IonButtons>
                    </IonToolbar>

                    <div className="ord-filter-bar">
                        <button className="ord-period-btn">
                            Toàn thời gian <IonIcon icon={chevronDownOutline} />
                        </button>
                    </div>

                    <div className="ord-summary">
                        <div className="ord-summary-title">
                            Tổng tiền hoàn <IonIcon icon={chevronDownOutline} />
                            <span className="ord-summary-amount">{fmt(totalAmount)}</span>
                        </div>
                        <div className="ord-summary-sub">{returns.length} phiếu trả</div>
                    </div>
                </div>
            </IonHeader>

            <IonContent className="ord-content">
                {loading ? (
                    <div className="ord-loading"><IonSpinner name="crescent" color="primary" /></div>
                ) : returns.length === 0 ? (
                    <div className="ord-empty">Chưa có phiếu trả hàng nào</div>
                ) : (
                    <div className="ord-list-card">
                        {Array.from(grouped.entries()).map(([label, items]) => (
                            <div key={label} className="ord-list-group">
                                <div className="ord-date-label">{label}</div>
                                {items.map((o, idx) => {
                                    const d = o.createdAt ? new Date(o.createdAt) : new Date();
                                    const timeStr = `${fmtDate(d)} ${fmtTime(d)}`;
                                    const code = `TH${String(o.id).padStart(6, '0')}`;
                                    const firstItem = o.items?.[0];

                                    return (
                                        <div key={o.id}
                                            className={`ord-list-item${idx === items.length - 1 ? ' last-item' : ''}`}
                                        >
                                            <div className="ord-item-top">
                                                <div className="ord-item-customer">
                                                    {o.customerName || 'Khách lẻ'}
                                                </div>
                                                <div className="ord-item-amount">-{fmt(o.refundAmount)}</div>
                                            </div>
                                            <div className="ord-item-meta">
                                                <span>{timeStr} · {code}</span>
                                                <span className="ord-item-payment">
                                                    Từ {o.orderCode}
                                                </span>
                                            </div>
                                            {firstItem && (
                                                <div className="ord-item-products">
                                                    {firstItem.productName} x{firstItem.quantity}
                                                    {o.items.length > 1 && (
                                                        <div className="ord-item-extra">+{o.items.length - 1} mặt hàng khác</div>
                                                    )}
                                                </div>
                                            )}
                                        </div>
                                    );
                                })}
                            </div>
                        ))}
                    </div>
                )}
            </IonContent>

            <IonFab vertical="bottom" horizontal="end" slot="fixed" style={{ marginBottom: '16px' }}>
                <IonFabButton className="ord-fab" onClick={() => ionRouter.push('/order-returns/new')}>
                    <IonIcon icon={addOutline} />
                </IonFabButton>
            </IonFab>

            <IonToast isOpen={toast !== null} message={toast ?? ''} duration={2500}
                onDidDismiss={() => setToast(null)} />
        </IonPage>
    );
};

export default OrderReturnsPage;
