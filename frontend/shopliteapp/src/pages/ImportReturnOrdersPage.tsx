import React, { useState } from 'react';
import {
    IonPage, IonHeader, IonToolbar, IonContent, IonIcon,
    IonButtons, IonButton, IonSpinner, useIonRouter, useIonViewWillEnter
} from '@ionic/react';
import { chevronBackOutline, searchOutline, chevronDownOutline } from 'ionicons/icons';
import { importReturnOrderService } from '../services/importReturnOrder.service';
import type { ImportReturnOrder } from '../api/types';
import './ImportReturnOrdersPage.css';

const fmt = (n?: number) => (n ?? 0).toLocaleString('vi-VN');

const ImportReturnOrdersPage: React.FC = () => {
    const ionRouter = useIonRouter();
    const [orders, setOrders] = useState<ImportReturnOrder[]>([]);
    const [loading, setLoading] = useState(false);

    const loadData = async () => {
        setLoading(true);
        try {
            const data = await importReturnOrderService.getAll();
            setOrders(data);
        } catch { /* silently */ } finally {
            setLoading(false);
        }
    };

    useIonViewWillEnter(() => { loadData(); });

    // Group by date label
    const grouped = React.useMemo(() => {
        const map = new Map<string, ImportReturnOrder[]>();
        const sorted = [...orders].sort((a, b) =>
            new Date(b.createdAt || 0).getTime() - new Date(a.createdAt || 0).getTime()
        );
        sorted.forEach(o => {
            const d = o.createdAt ? new Date(o.createdAt) : new Date();
            const today = new Date();
            const isToday =
                d.getDate() === today.getDate() &&
                d.getMonth() === today.getMonth() &&
                d.getFullYear() === today.getFullYear();
            const pad = (v: number) => String(v).padStart(2, '0');
            const label = isToday
                ? `HÔM NAY ${pad(d.getDate())}/${pad(d.getMonth() + 1)}/${d.getFullYear()}`
                : `${pad(d.getDate())}/${pad(d.getMonth() + 1)}/${d.getFullYear()}`;
            if (!map.has(label)) map.set(label, []);
            map.get(label)!.push(o);
        });
        return map;
    }, [orders]);

    const totalAmount = orders.reduce((s, o) => s + (o.totalAmount || 0), 0);

    return (
        <IonPage className="iro-page">
            <IonHeader className="iro-header ion-no-border">
                <div className="iro-top-card">
                    <IonToolbar className="iro-toolbar">
                        <IonButtons slot="start">
                            <IonButton color="dark" onClick={() => ionRouter.goBack()}>
                                <IonIcon icon={chevronBackOutline} style={{ fontSize: '26px' }} />
                            </IonButton>
                        </IonButtons>
                        <div className="iro-title">Trả hàng nhập</div>
                        <IonButtons slot="end">
                            <IonButton color="dark">
                                <IonIcon icon={searchOutline} style={{ fontSize: '22px' }} />
                            </IonButton>
                            <IonButton color="dark">
                                <svg width="22" height="22" viewBox="0 0 24 24" fill="none"
                                    stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                                    <path d="M7 16V4M7 4L3 8M7 4L11 8M17 8V20M17 20L21 16M17 20L13 16" />
                                </svg>
                            </IonButton>
                        </IonButtons>
                    </IonToolbar>

                    <div className="iro-filter-bar">
                        <button className="iro-filter-btn">
                            <svg width="18" height="18" viewBox="0 0 24 24" fill="none"
                                stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                                <line x1="4" y1="6" x2="20" y2="6" />
                                <line x1="8" y1="12" x2="16" y2="12" />
                                <line x1="10" y1="18" x2="14" y2="18" />
                            </svg>
                        </button>
                        <button className="iro-period-btn">
                            Tháng này <IonIcon icon={chevronDownOutline} />
                        </button>
                    </div>

                    <div className="iro-summary">
                        <div className="iro-summary-title">
                            Tổng tiền hàng <IonIcon icon={chevronDownOutline} />
                            <span className="iro-summary-amount">{fmt(totalAmount)}</span>
                        </div>
                        <div className="iro-summary-sub">{orders.length} phiếu</div>
                    </div>
                </div>
            </IonHeader>

            <IonContent className="iro-content">
                {loading ? (
                    <div className="iro-loading"><IonSpinner name="crescent" color="primary" /></div>
                ) : orders.length === 0 ? (
                    <div className="iro-empty">Chưa có phiếu trả hàng nhập</div>
                ) : (
                    <div className="iro-list-card">
                        {Array.from(grouped.entries()).map(([label, items]) => (
                            <div key={label}>
                                <div className="iro-date-label">{label}</div>
                                {items.map(o => {
                                    const d = o.createdAt ? new Date(o.createdAt) : new Date();
                                    const pad = (v: number) => String(v).padStart(2, '0');
                                    const time = `${pad(d.getDate())}/${pad(d.getMonth() + 1)}/${d.getFullYear()} ${pad(d.getHours())}:${pad(d.getMinutes())}`;
                                    const code = `THN${String(o.id).padStart(6, '0')}`;
                                    const qtyTotal = o.items?.reduce((s, i) => s + i.quantity, 0) || 0;
                                    const itemCount = o.items?.length || 0;
                                    const itemNames = o.items?.map(i => `${i.productName || 'SP'} x${i.quantity}`).join(', ') || '';
                                    return (
                                        <div
                                            key={o.id}
                                            className="iro-list-item"
                                            onClick={() => ionRouter.push(`/import-return-orders/${o.id}`)}
                                        >
                                            <div className="iro-item-top">
                                                <div className="iro-item-supplier">{o.supplierName || 'Nhà cung cấp'}</div>
                                                <div className="iro-item-amount">{fmt(o.totalAmount)}</div>
                                            </div>
                                            <div className="iro-item-meta">
                                                {time} • {code}
                                            </div>
                                            <div className="iro-item-qty">
                                                {itemCount} mặt hàng • Số lượng: {fmt(qtyTotal)}
                                            </div>
                                            <div className="iro-item-names">{itemNames}</div>
                                        </div>
                                    );
                                })}
                            </div>
                        ))}
                    </div>
                )}
            </IonContent>
        </IonPage>
    );
};

export default ImportReturnOrdersPage;
