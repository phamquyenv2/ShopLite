import React, { useState } from 'react';
import {
    IonPage, IonHeader, IonToolbar, IonContent, IonIcon, IonButtons, IonButton,
    IonSpinner, IonFab, IonFabButton, useIonRouter, useIonViewWillEnter
} from '@ionic/react';
import {
    arrowBackOutline, searchOutline, swapVerticalOutline,
    filterOutline, chevronDownOutline, addOutline, chevronBackOutline
} from 'ionicons/icons';
import { importOrderService } from '../services/importOrder.service';
import type { ImportOrder } from '../api/types';
import './ImportOrdersPage.css';

const statusLabel: Record<string, string> = {
    PENDING: 'Chờ nhập',
    COMPLETED: 'Đã nhập hàng',
    CANCELLED: 'Đã hủy',
};

const fmt = (n?: number) => (n ?? 0).toLocaleString('vi-VN');

const ImportOrdersPage: React.FC = () => {
    const ionRouter = useIonRouter();
    const [orders, setOrders] = useState<ImportOrder[]>([]);
    const [loading, setLoading] = useState(false);

    const loadData = async () => {
        setLoading(true);
        try {
            const data = await importOrderService.getAll();
            setOrders(data);
        } catch { /* silently */ } finally {
            setLoading(false);
        }
    };

    useIonViewWillEnter(() => { loadData(); });

    // Group by date
    const grouped = React.useMemo(() => {
        const map = new Map<string, ImportOrder[]>();
        const sorted = [...orders].sort((a, b) =>
            new Date(b.createdAt || 0).getTime() - new Date(a.createdAt || 0).getTime()
        );
        sorted.forEach(o => {
            const d = o.createdAt ? new Date(o.createdAt) : new Date();
            const dayNames = ['CHỦ NHẬT', 'THỨ HAI', 'THỨ BA', 'THỨ TƯ', 'THỨ NĂM', 'THỨ SÁU', 'THỨ BẢY'];
            const label = `${dayNames[d.getDay()]}, ${String(d.getDate()).padStart(2, '0')}/${String(d.getMonth() + 1).padStart(2, '0')}/${d.getFullYear()}`;
            if (!map.has(label)) map.set(label, []);
            map.get(label)!.push(o);
        });
        return map;
    }, [orders]);

    const totalAmount = orders.reduce((s, o) => s + (o.totalAmount || 0), 0);
    const totalItems = orders.reduce((s, o) => s + (o.items?.length || 0), 0);
    const totalQty = orders.reduce((s, o) => s + (o.items?.reduce((q, i) => q + i.quantity, 0) || 0), 0);

    return (
        <IonPage className="io-page">
            <IonHeader className="io-header ion-no-border">
                <div className="io-top-card">
                    <IonToolbar className="io-toolbar">
                        <IonButtons slot="start">
                            <IonButton color="dark" onClick={() => ionRouter.goBack()}>
                                <IonIcon icon={chevronBackOutline} style={{ fontSize: '26px' }} />
                            </IonButton>
                        </IonButtons>
                        <div className="io-title">Nhập hàng</div>
                        <IonButtons slot="end">
                            <IonButton color="dark"><IonIcon icon={searchOutline} style={{ fontSize: '22px' }} /></IonButton>
                            <IonButton color="dark">
                                <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                                    <path d="M7 16V4M7 4L3 8M7 4L11 8M17 8V20M17 20L21 16M17 20L13 16" />
                                </svg>
                            </IonButton>
                        </IonButtons>
                    </IonToolbar>

                    <div className="io-filter-bar">
                        <button className="io-filter-btn">
                            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                                <line x1="4" y1="6" x2="20" y2="6"></line>
                                <line x1="8" y1="12" x2="16" y2="12"></line>
                                <line x1="10" y1="18" x2="14" y2="18"></line>
                            </svg>
                        </button>
                        <button className="io-period-btn">
                            Tháng này <IonIcon icon={chevronDownOutline} />
                        </button>
                    </div>

                    <div className="io-summary">
                        <div className="io-summary-title">
                            Tổng tiền hàng <IonIcon icon={chevronDownOutline} />
                            <span className="io-summary-amount">{fmt(totalAmount)}</span>
                        </div>
                        <div className="io-summary-sub">
                            {orders.length} phiếu - SL: {fmt(totalQty)}
                        </div>
                    </div>
                </div>
            </IonHeader>

            <IonContent className="io-content">
                {loading ? (
                    <div className="io-loading"><IonSpinner name="crescent" color="primary" /></div>
                ) : orders.length === 0 ? (
                    <div className="io-empty">Chưa có phiếu nhập hàng</div>
                ) : (
                    <div className="io-list-card">
                        {Array.from(grouped.entries()).map(([label, items]) => (
                            <div key={label} className="io-list-group">
                                <div className="io-date-label">{label}</div>
                                {items.map((o, idx) => {
                                    const d = o.createdAt ? new Date(o.createdAt) : new Date();
                                    const time = `${String(d.getDate()).padStart(2, '0')}/${String(d.getMonth() + 1).padStart(2, '0')}/${d.getFullYear()} ${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`;
                                    const code = `PN${String(o.id).padStart(6, '0')}`;
                                    const qtyTotal = o.items?.reduce((s, i) => s + i.quantity, 0) || 0;
                                    const itemCount = o.items?.length || 0;
                                    const itemNames = o.items?.map(i => `${i.productName || 'SP'} x${i.quantity}`).join(', ') || '';
                                    return (
                                        <div key={o.id} className={`io-list-item ${idx === items.length - 1 ? 'last-item' : ''}`} onClick={() => ionRouter.push(`/import-orders/${o.id}`)}>
                                            <div className="io-item-top">
                                                <div className="io-item-supplier">{o.supplierName || 'Đại lý'}</div>
                                                <div className="io-item-amount">{fmt(o.totalAmount)}</div>
                                            </div>
                                            <div className="io-item-meta">
                                                <span>{time} • {code}</span>
                                                {o.status === 'PENDING' ? (
                                                    <span className="io-badge-draft">Phiếu tạm</span>
                                                ) : (
                                                    <span className="io-item-payment">Tiền mặt</span>
                                                )}
                                            </div>
                                            <div className="io-item-qty">{itemCount} mặt hàng • Số lượng: {fmt(qtyTotal)}</div>
                                            <div className="io-item-names">{itemNames}</div>
                                        </div>
                                    );
                                })}
                            </div>
                        ))}
                    </div>
                )}
            </IonContent>

            <IonFab vertical="bottom" horizontal="end" slot="fixed" className="io-fab-wrap">
                <IonFabButton className="io-fab" onClick={() => ionRouter.push('/import-order/new')}>
                    <IonIcon icon={addOutline} />
                </IonFabButton>
            </IonFab>
        </IonPage>
    );
};

export default ImportOrdersPage;
