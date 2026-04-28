import React, { useState } from 'react';
import {
    IonPage, IonHeader, IonToolbar, IonContent, IonIcon, IonButtons, IonButton,
    IonSpinner, IonToast, useIonRouter, useIonViewWillEnter, IonFooter
} from '@ionic/react';
import { arrowBackOutline, ellipsisVertical, shareOutline } from 'ionicons/icons';
import { useParams } from 'react-router';
import { importOrderService } from '../services/importOrder.service';
import type { ImportOrder } from '../api/types';
import './ImportOrderDetailPage.css';

const statusMap: Record<string, { label: string; cls: string }> = {
    PENDING: { label: 'Phiếu tạm', cls: 'status-pending' },
    COMPLETED: { label: 'Đã nhập hàng', cls: 'status-completed' },
    CANCELLED: { label: 'Đã hủy', cls: 'status-cancelled' },
};

const fmt = (n?: number) => (n ?? 0).toLocaleString('vi-VN');

const ImportOrderDetailPage: React.FC = () => {
    const { id } = useParams<{ id: string }>();
    const ionRouter = useIonRouter();
    const [order, setOrder] = useState<ImportOrder | null>(null);
    const [loading, setLoading] = useState(false);
    const [toast, setToast] = useState<string | null>(null);

    const loadData = async () => {
        setLoading(true);
        try {
            const data = await importOrderService.getById(id);
            setOrder(data);
        } catch (err: any) {
            setToast(err.message || 'Không thể tải phiếu nhập');
        } finally {
            setLoading(false);
        }
    };

    useIonViewWillEnter(() => { loadData(); });

    if (loading) {
        return (
            <IonPage className="iod-page">
                <IonContent><div className="iod-loading"><IonSpinner name="crescent" color="primary" /></div></IonContent>
            </IonPage>
        );
    }

    if (!order) {
        return (
            <IonPage className="iod-page">
                <IonContent><div className="iod-loading">Không tìm thấy phiếu nhập</div></IonContent>
            </IonPage>
        );
    }

    const code = `PN${String(order.id).padStart(6, '0')}`;
    const d = order.createdAt ? new Date(order.createdAt) : new Date();
    const dateStr = `${String(d.getDate()).padStart(2, '0')}/${String(d.getMonth() + 1).padStart(2, '0')}/${d.getFullYear()} ${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`;
    const st = statusMap[order.status] || statusMap.PENDING;
    const totalQty = order.items?.reduce((s, i) => s + i.quantity, 0) || 0;
    const itemCount = order.items?.length || 0;
    const needToPay = (order.totalAmount || 0) - (order.discount || 0);

    return (
        <IonPage className="iod-page">
            <IonHeader className="iod-header ion-no-border">
                <IonToolbar className="iod-toolbar">
                    <IonButtons slot="start">
                        <IonButton color="dark" onClick={() => ionRouter.goBack()}>
                            <IonIcon icon={arrowBackOutline} style={{ fontSize: '26px' }} />
                        </IonButton>
                    </IonButtons>
                    <IonButtons slot="end">
                        {order.status === 'PENDING' && (
                            <IonButton color="primary" style={{ fontWeight: 600, fontSize: '16px', marginRight: '8px' }} onClick={() => ionRouter.push(`/import-order/edit/${order.id}`)}>Sửa</IonButton>
                        )}
                    </IonButtons>
                </IonToolbar>
            </IonHeader>

            <IonContent className="iod-content">
                {/* Header info */}
                <div className="iod-info-section">
                    <div className="iod-info-top">
                        <div className="iod-code">{code}</div>
                        <span className={`iod-status ${st.cls}`}>{st.label}</span>
                    </div>
                    <div className="iod-date">{dateStr}</div>
                </div>

                {/* Supplier */}
                <div className="iod-card">
                    <div className="iod-supplier-row">
                        <div className="iod-supplier-icon">
                            <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="#64748b" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                                <path d="M3 21h18M5 21V8l7-5 7 5v13M9 21v-5h6v5M5 11h14"/>
                            </svg>
                        </div>
                        <div>
                            <div className="iod-supplier-name">{order.supplierName || 'NCC'}</div>
                            <div className="iod-supplier-code">NCC{String(order.supplierId || 0).padStart(4, '0')}</div>
                        </div>
                    </div>
                </div>

                {/* Items */}
                <div className="iod-card iod-items-card">
                    {order.items?.map((item, idx) => (
                        <div key={item.id || item.productId} className={`iod-item-row ${idx === (order.items?.length || 0) - 1 ? 'last-item' : ''}`}>
                            <div className="iod-item-thumb">
                                <div className="iod-thumb-ph"></div>
                            </div>
                            <div className="iod-item-content">
                                <div className="iod-item-name">{item.productName || 'Sản phẩm'}</div>
                                <div className="iod-item-sku">{item.productSku || '---'}</div>
                                <div className="iod-item-calc">
                                    <span>{fmt(item.importPrice)} x {fmt(item.quantity)}</span>
                                    <span className="iod-item-total">{fmt(item.subTotal || item.importPrice * item.quantity)}</span>
                                </div>
                            </div>
                        </div>
                    ))}
                </div>

                {/* Totals */}
                <div className="iod-card iod-totals">
                    <div className="iod-total-row">
                        <div className="iod-t-block">
                            <div className="iod-t-label-main">Tổng tiền hàng</div>
                            <div className="iod-t-sub">{itemCount} mặt hàng • Số lượng: {totalQty}</div>
                        </div>
                        <span className="iod-t-val-main">{fmt(order.totalAmount)}</span>
                    </div>

                    <div className="iod-total-row">
                        <span>Giảm giá</span>
                        <span>{fmt(order.discount)}</span>
                    </div>
                    <div className="iod-total-row bold">
                        <span>Cần trả NCC</span>
                        <span>{fmt(needToPay)}</span>
                    </div>
                    <div className="iod-total-row bold">
                        <span>Đã trả NCC</span>
                        <span className="iod-paid">{fmt(order.amountPaid)}</span>
                    </div>
                </div>

                {/* Footer info */}
                <div className="iod-card iod-people">
                    <div className="iod-people-row">
                        <div>
                            <div className="iod-people-label">Người tạo</div>
                            <div className="iod-people-name">{order.username || 'Phạm Anh Quyền'}</div>
                        </div>
                        <div>
                            <div className="iod-people-label">Người nhập hàng</div>
                            <div className="iod-people-name">{order.username || 'Phạm Anh Quyền'}</div>
                        </div>
                    </div>
                    <div className="iod-people-row mt-12">
                        <div>
                            <div className="iod-people-label">Chi nhánh</div>
                            <div className="iod-people-name">Chi nhánh trung tâm</div>
                        </div>
                    </div>
                </div>
            </IonContent>

            {order.status === 'COMPLETED' && order.returnStatus !== 'FULL_RETURNED' && (
                <IonFooter className="iod-action-footer ion-no-border">
                    <div className="iod-action-section" onClick={() => ionRouter.push(`/import-return-orders/create/${order.id}`)} style={{ cursor: 'pointer' }}>
                        <IonIcon icon={shareOutline} className="iod-action-icon" />
                        <span className="iod-action-text">Trả hàng nhập</span>
                    </div>
                </IonFooter>
            )}

            <IonToast isOpen={toast !== null} message={toast ?? ''} duration={2000} onDidDismiss={() => setToast(null)} />
        </IonPage>
    );
};

export default ImportOrderDetailPage;
