import React, { useEffect, useState } from 'react';
import {
    IonPage, IonHeader, IonToolbar, IonContent, IonIcon, IonButtons, IonButton,
    IonSpinner, IonToast, useIonRouter, IonModal, IonAlert
} from '@ionic/react';
import {
    chevronBackOutline, ellipsisHorizontalOutline, documentTextOutline,
    personOutline, chevronDownOutline, returnUpBackOutline, printOutline,
    shareOutline, trashOutline
} from 'ionicons/icons';
import { useParams } from 'react-router';
import { authApis, endpoints } from '../utils/Apis';
import type { Order } from '../api/types';
import { useStorePermissions } from '../utils/useStorePermissions';
import './OrderDetailPage.css';

const fmt = (n?: number | null) => (n ?? 0).toLocaleString('vi-VN');

const fmtDateFull = (dStr?: string | null) => {
    if (!dStr) return '';
    const d = new Date(dStr);
    const time = `${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`;
    const date = `${String(d.getDate()).padStart(2, '0')}/${String(d.getMonth() + 1).padStart(2, '0')}/${d.getFullYear()}`;
    return `${date} ${time}`;
};

const PAYMENT_LABEL: Record<string, string> = {
    CASH: 'Tiền mặt',
    BANK: 'Chuyển khoản',
    MOMO: 'MoMo',
    SEPAY_QR: 'SePay QR',
    COD: 'COD',
    VNPAY: 'VNPay',
};

const STATUS_VI: Record<string, { label: string; cls: string }> = {
    DRAFT: { label: 'Nháp', cls: 'ord-status-pending' },
    PENDING: { label: 'Đang xử lý', cls: 'ord-status-pending' },
    PENDING_PAYMENT: { label: 'Chờ thanh toán', cls: 'ord-status-pending' },
    COMPLETED: { label: 'Hoàn thành', cls: 'ord-status-completed' },
    FAIL: { label: 'Thất bại', cls: 'ord-status-cancelled' },
    CANCELLED: { label: 'Đã huỷ', cls: 'ord-status-cancelled' },
};

const OrderDetailPage: React.FC = () => {
    const { id } = useParams<{ id: string }>();
    const ionRouter = useIonRouter();
    const { can } = useStorePermissions();
    const canUpdateOrder = can('/api/v1/orders/{id}', 'PUT');
    const canCancelOrder = can('/api/v1/orders/{id}', 'DELETE');
    const [order, setOrder] = useState<Order | null>(null);
    const [loading, setLoading] = useState(false);
    const [toast, setToast] = useState<string | null>(null);
    const [showActionSheet, setShowActionSheet] = useState(false);
    const [showCancelAlert, setShowCancelAlert] = useState(false);
    const [showPaymentSheet, setShowPaymentSheet] = useState(false);

    const loadOrder = async () => {
        setLoading(true);
        try {
            const res = await authApis().get<any>(endpoints['order-detail'](id));
            const payload = res.data;
            setOrder(payload?.data || payload || null);
        } catch (e: any) {
            console.error(e);
            setToast(e.message || 'Không thể tải chi tiết hóa đơn');
        } finally {
            setLoading(false);
        }
    };

    const handleCancelOrder = async () => {
        if (!canCancelOrder) {
            setToast('Bạn không có quyền huỷ hoá đơn');
            return;
        }
        setShowCancelAlert(false);
        setLoading(true);
        try {
            await authApis().delete<any>(endpoints['order-detail'](id));
            setToast('Đã huỷ hoá đơn thành công');
            await loadOrder();
        } catch (e: any) {
            console.error(e);
            setToast(e.message || 'Không thể huỷ hoá đơn');
            setLoading(false); // only toggle off if failed, otherwise loadOrder handles it
        }
    };

    const handlePayment = async (method: string) => {
        if (!order) return;
        setShowPaymentSheet(false);
        setLoading(true);
        try {
            await authApis().post(endpoints['order-payments'](id), {
                paymentMethod: method,
                amount: order.totalAmount
            });
            setToast('Đã thu tiền thành công');
            setTimeout(() => {
                ionRouter.goBack();
            }, 500);
        } catch (e: any) {
            console.error(e);
            setToast(e.message || 'Không thể tạo thanh toán');
            setLoading(false);
        }
    };

    useEffect(() => {
        void loadOrder();
    }, [id]);

    if (loading) {
        return (
            <IonPage className="ord-detail-page">
                <IonContent><div className="ord-loading-container"><IonSpinner name="crescent" color="primary" /></div></IonContent>
            </IonPage>
        );
    }

    if (!order) {
        return (
            <IonPage className="ord-detail-page">
                <IonHeader className="ord-detail-header ion-no-border">
                    <IonToolbar className="ord-detail-toolbar">
                        <IonButtons slot="start">
                            <IonButton color="dark" onClick={() => ionRouter.goBack()}>
                                <IonIcon icon={chevronBackOutline} style={{ fontSize: '26px' }} />
                            </IonButton>
                        </IonButtons>
                    </IonToolbar>
                </IonHeader>
                <IonContent><div className="ord-loading-container" style={{ color: '#666' }}>Không tìm thấy hóa đơn</div></IonContent>
            </IonPage>
        );
    }

    const code = order.code || `HD${String(order.id).padStart(6, '0')}`;
    const st = STATUS_VI[order.status ?? ''] || STATUS_VI.PENDING;
    const totalQty = order.items?.reduce((s, i) => s + i.quantity, 0) || 0;
    const itemsTotal = order.items?.reduce((s, i) => s + (i.totalPrice ?? (i.price * i.quantity)), 0) || 0;

    return (
        <IonPage className="ord-detail-page">
            <IonHeader className="ord-detail-header ion-no-border">
                <IonToolbar className="ord-detail-toolbar">
                    <IonButtons slot="start">
                        <IonButton color="dark" onClick={() => ionRouter.goBack()}>
                            <IonIcon icon={chevronBackOutline} style={{ fontSize: '26px' }} />
                        </IonButton>
                    </IonButtons>
                    <IonButtons slot="end">
                        <IonButton color="dark" onClick={() => setShowActionSheet(true)}>
                            <IonIcon icon={ellipsisHorizontalOutline} style={{ fontSize: '24px' }} />
                        </IonButton>
                    </IonButtons>
                </IonToolbar>
            </IonHeader>

            <IonContent className="ord-detail-content">
                <div className="ord-detail-container">
                    {/* Status Card */}
                    <div className="ord-detail-card">
                        <div className="ord-info-header">
                            <div className="ord-info-code">{code}</div>
                            <div className={`ord-status-badge ${st.cls}`}>
                                {st.label}
                            </div>
                        </div>
                        <div className="ord-info-time">{fmtDateFull(order.createdAt)}</div>
                    </div>

                    {/* Customer Card */}
                    <div className="ord-detail-card ord-customer-card">
                        <div className="ord-customer-avatar">
                            <IonIcon icon={personOutline} />
                        </div>
                        <div className="ord-customer-name">
                            {order.customerName || 'Khách lẻ'}
                        </div>
                    </div>

                    {/* Products List */}
                    {order.items && order.items.length > 0 && (
                        <div className="ord-detail-card ord-products-card">
                            {order.items.map(item => (
                                <div key={item.id} className="ord-product-item">
                                    <div className="ord-product-img-placeholder">
                                        <IonIcon icon={documentTextOutline} style={{ fontSize: '24px' }} />
                                    </div>
                                    <div className="ord-product-info">
                                        <div className="ord-product-name">{item.productName}</div>
                                        <div className="ord-product-sku">SP{String(item.productId).padStart(6, '0')}</div>
                                        <div className="ord-product-price-row">
                                            <div className="ord-product-qty">
                                                {fmt(item.price)} x {item.quantity}
                                            </div>
                                            <div className="ord-product-total">
                                                {fmt(item.totalPrice ?? (item.price * item.quantity))}
                                            </div>
                                        </div>
                                    </div>
                                </div>
                            ))}
                        </div>
                    )}

                    {/* Financial Summary */}
                    <div className="ord-detail-card ord-summary-card">
                        <div className="ord-summary-row">
                            <div className="ord-summary-label">
                                Tổng tiền hàng <span className="ord-summary-qty-badge">{totalQty}</span>
                            </div>
                            <div className="ord-summary-value">{fmt(itemsTotal)}</div>
                        </div>
                        <div className="ord-summary-row">
                            <div className="ord-summary-label">Giảm giá</div>
                            <div className="ord-summary-value">{fmt(order.discount)}</div>
                        </div>
                        <div className="ord-summary-row total-pay">
                            <div className="ord-summary-label">Khách cần trả</div>
                            <div className="ord-summary-value">{fmt(order.totalAmount)}</div>
                        </div>
                        <div className="ord-summary-row paid">
                            <div className="ord-summary-label">Khách đã trả</div>
                            <div className="ord-summary-value">{fmt(order.totalAmount)}</div>
                        </div>
                        <div className="ord-summary-row payment-method">
                            <div className="ord-payment-method-text">
                                <IonIcon icon={chevronDownOutline} />
                                {order.paymentMethod ? PAYMENT_LABEL[order.paymentMethod] || order.paymentMethod : 'Chưa thanh toán'}
                            </div>
                        </div>
                    </div>

                    {/* Note Section */}
                    <div className="ord-detail-card ord-note-card">
                        <div className="ord-note-header">
                            <div className="ord-note-title">GHI CHÚ</div>
                        </div>
                        <div className="ord-note-content">Chưa có ghi chú...</div>
                    </div>
                </div>
            </IonContent>

            {order.status === 'PENDING_PAYMENT' && (
                <div className="ord-detail-footer" style={{ display: 'flex', gap: '12px', padding: '16px', background: '#fff', boxShadow: '0 -4px 16px rgba(0,0,0,0.05)' }}>
                    {canCancelOrder && (
                        <button 
                            style={{ flex: 1, padding: '14px', borderRadius: '12px', background: '#fee2e2', color: '#ef4444', fontWeight: 600, border: 'none', fontSize: '15px' }}
                            onClick={() => setShowCancelAlert(true)}
                        >
                            Huỷ Đơn
                        </button>
                    )}
                    <button 
                        style={{ flex: 1.5, padding: '14px', borderRadius: '12px', background: '#0066FF', color: '#fff', fontWeight: 600, border: 'none', fontSize: '15px' }}
                        onClick={() => setShowPaymentSheet(true)}
                    >
                        Thu Tiền
                    </button>
                </div>
            )}

            {canUpdateOrder && order.status === 'DRAFT' && (
                <div className="ord-detail-footer">
                    <IonButton className="ord-btn-update" expand="block" fill="clear" onClick={() => setToast('Tính năng đang phát triển')}>
                        <IonIcon icon={documentTextOutline} />
                        Cập nhật
                    </IonButton>
                </div>
            )}

            <IonModal 
                isOpen={showActionSheet} 
                onDidDismiss={() => setShowActionSheet(false)}
                initialBreakpoint={0.35}
                breakpoints={[0, 0.35]}
                className="ord-action-modal"
            >
                <div className="ord-action-sheet-content">
                    <button className="ord-action-item" onClick={() => { setShowActionSheet(false); setToast('Tính năng đang phát triển'); }}>
                        <IonIcon icon={returnUpBackOutline} /> Trả hàng
                    </button>
                    <button className="ord-action-item" onClick={() => { setShowActionSheet(false); setToast('Tính năng đang phát triển'); }}>
                        <IonIcon icon={printOutline} /> In
                    </button>
                    <button className="ord-action-item" onClick={() => { setShowActionSheet(false); setToast('Tính năng đang phát triển'); }}>
                        <IonIcon icon={shareOutline} /> Chia sẻ
                    </button>
                    {canCancelOrder && order.status !== 'CANCELLED' && order.status !== 'FAIL' && (
                        <button className="ord-action-item" onClick={() => { setShowActionSheet(false); setShowCancelAlert(true); }}>
                            <IonIcon icon={trashOutline} /> Huỷ hoá đơn
                        </button>
                    )}
                </div>
            </IonModal>

            <IonModal 
                isOpen={showPaymentSheet} 
                onDidDismiss={() => setShowPaymentSheet(false)}
                initialBreakpoint={0.35}
                breakpoints={[0, 0.35]}
                className="ord-action-modal"
            >
                <div className="ord-action-sheet-content">
                    <div style={{ padding: '16px', fontWeight: 600, fontSize: '16px', borderBottom: '1px solid #f1f5f9' }}>
                        Chọn phương thức thanh toán
                    </div>
                    <button className="ord-action-item" onClick={() => handlePayment('CASH')}>
                        Tiền mặt
                    </button>
                    <button className="ord-action-item" onClick={() => handlePayment('BANK_TRANSFER')}>
                        Chuyển khoản
                    </button>
                    <button className="ord-action-item" onClick={() => handlePayment('EWALLET')}>
                        Ví điện tử (MoMo, ZaloPay)
                    </button>
                </div>
            </IonModal>

            <IonAlert
                isOpen={showCancelAlert}
                onDidDismiss={() => setShowCancelAlert(false)}
                header={'Xác nhận'}
                message={'Bạn có chắc chắn muốn huỷ hoá đơn này không?'}
                buttons={[
                    { text: 'Không', role: 'cancel', cssClass: 'secondary' },
                    { text: 'Có, huỷ hoá đơn', handler: handleCancelOrder }
                ]}
            />

            <IonToast isOpen={toast !== null} message={toast ?? ''} duration={2500}
                onDidDismiss={() => setToast(null)} />
        </IonPage>
    );
};

export default OrderDetailPage;
