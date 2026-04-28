import React, { useState } from 'react';
import {
    IonPage, IonHeader, IonToolbar, IonContent, IonIcon,
    IonButtons, IonButton, IonSpinner, IonToast,
    useIonRouter, useIonViewWillEnter
} from '@ionic/react';
import { arrowBackOutline, ellipsisVerticalOutline, chevronForwardOutline } from 'ionicons/icons';
import { useParams } from 'react-router';
import { importReturnOrderService } from '../services/importReturnOrder.service';
import type { ImportReturnOrder } from '../api/types';
import './ImportReturnOrderDetailPage.css';

const fmt = (n?: number) => (n ?? 0).toLocaleString('vi-VN');

const ImportReturnOrderDetailPage: React.FC = () => {
    const { id } = useParams<{ id: string }>();
    const ionRouter = useIonRouter();
    const [order, setOrder] = useState<ImportReturnOrder | null>(null);
    const [loading, setLoading] = useState(false);
    const [toast, setToast] = useState<string | null>(null);

    const loadData = async () => {
        setLoading(true);
        try {
            const data = await importReturnOrderService.getById(id);
            setOrder(data);
        } catch (err: any) {
            setToast(err.message || 'Không thể tải phiếu trả hàng');
        } finally {
            setLoading(false);
        }
    };

    useIonViewWillEnter(() => { loadData(); });

    if (loading) {
        return (
            <IonPage className="irod-page">
                <IonContent><div className="irod-loading"><IonSpinner name="crescent" color="primary" /></div></IonContent>
            </IonPage>
        );
    }

    if (!order) {
        return (
            <IonPage className="irod-page">
                <IonContent><div className="irod-loading">Không tìm thấy phiếu trả hàng</div></IonContent>
            </IonPage>
        );
    }

    const pad = (v: number) => String(v).padStart(2, '0');
    const code = `THN${String(order.id).padStart(6, '0')}`;
    const importCode = order.importOrderId ? `PN${String(order.importOrderId).padStart(6, '0')}` : null;
    const supplierCode = `NCC${String(order.supplierId || 0).padStart(4, '0')}`;
    const d = order.createdAt ? new Date(order.createdAt) : new Date();
    const dateStr = `${pad(d.getDate())}/${pad(d.getMonth() + 1)}/${d.getFullYear()} ${pad(d.getHours())}:${pad(d.getMinutes())}`;
    const totalQty = order.items?.reduce((s, i) => s + i.quantity, 0) || 0;
    const itemCount = order.items?.length || 0;
    const discount = order.discount || 0;
    const totalAmount = order.totalAmount || 0;
    const needToReturn = totalAmount - discount;  // NCC cần trả lại
    const amountPaid = order.amountPaid || 0;     // NCC đã trả lại

    return (
        <IonPage className="irod-page">
            <IonHeader className="irod-header ion-no-border">
                <IonToolbar className="irod-toolbar">
                    <IonButtons slot="start">
                        <IonButton color="dark" onClick={() => ionRouter.goBack()}>
                            <IonIcon icon={arrowBackOutline} style={{ fontSize: '24px' }} />
                        </IonButton>
                    </IonButtons>
                    <IonButtons slot="end">
                        <IonButton color="dark">
                            <IonIcon icon={ellipsisVerticalOutline} style={{ fontSize: '22px' }} />
                        </IonButton>
                    </IonButtons>
                </IonToolbar>
            </IonHeader>

            <IonContent className="irod-content">
                {/* Code + Status */}
                <div className="irod-info-section">
                    <div className="irod-info-top">
                        <div className="irod-code">{code}</div>
                        <span className="irod-status-badge">Đã trả hàng</span>
                    </div>
                    <div className="irod-date">{dateStr}</div>
                </div>

                {/* Supplier info */}
                <div className="irod-card">
                    <div className="irod-supplier-row">
                        <div className="irod-icon-circle">
                            <svg width="24" height="24" viewBox="0 0 24 24" fill="none"
                                stroke="#64748b" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                                <rect x="2" y="7" width="20" height="14" rx="2" />
                                <path d="M16 21V5a2 2 0 0 0-2-2h-4a2 2 0 0 0-2 2v16" />
                            </svg>
                        </div>
                        <div>
                            <div className="irod-supplier-name">{order.supplierName || 'Nhà cung cấp'}</div>
                            <div className="irod-supplier-sub">
                                {supplierCode}{order.supplierPhone ? ` • ${order.supplierPhone}` : ''}
                            </div>
                        </div>
                    </div>
                </div>

                {/* Link to original import order */}
                {importCode && (
                    <div className="irod-card">
                        <div className="irod-ref-row">
                            <div className="irod-icon-circle">
                                <svg width="22" height="22" viewBox="0 0 24 24" fill="none"
                                    stroke="#64748b" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                                    <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z" />
                                    <polyline points="14 2 14 8 20 8" />
                                    <line x1="16" y1="13" x2="8" y2="13" />
                                    <line x1="16" y1="17" x2="8" y2="17" />
                                    <polyline points="10 9 9 9 8 9" />
                                </svg>
                            </div>
                            <div
                                className="irod-ref-content"
                                onClick={() => ionRouter.push(`/import-orders/${order.importOrderId}`)}
                                style={{ cursor: 'pointer' }}
                            >
                                <div className="irod-ref-label">Phiếu nhập hàng</div>
                                <div className="irod-ref-code">{importCode}</div>
                            </div>
                            <IonIcon icon={chevronForwardOutline} className="irod-chevron" />
                        </div>
                    </div>
                )}

                {/* Product items */}
                <div className="irod-card irod-items-card">
                    {order.items?.map((item, idx) => (
                        <div key={item.id ?? idx} className="irod-item-row">
                            <div className="irod-item-thumb">
                                {item.productImage
                                    ? <img src={item.productImage} alt={item.productName} />
                                    : <div className="irod-thumb-ph" />
                                }
                            </div>
                            <div className="irod-item-content">
                                <div className="irod-item-name">{item.productName || 'Sản phẩm'}</div>
                                <div className="irod-item-sku">{item.productSku || '---'}</div>
                                <div className="irod-item-calc">
                                    <span>{fmt(item.returnPrice)} x {item.quantity}</span>
                                    <span className="irod-item-total">{fmt(item.subTotal ?? item.returnPrice * item.quantity)}</span>
                                </div>
                            </div>
                        </div>
                    ))}
                </div>

                {/* Totals */}
                <div className="irod-card irod-totals-card">
                    <div className="irod-total-row">
                        <div className="irod-t-block">
                            <div className="irod-t-label-main">Tổng tiền hàng</div>
                            <div className="irod-t-sub">{itemCount} mặt hàng • Số lượng: {totalQty}</div>
                        </div>
                        <span className="irod-t-val-main">{fmt(totalAmount)}</span>
                    </div>

                    <div className="irod-total-row">
                        <span>Giảm giá</span>
                        <span>{fmt(discount)}</span>
                    </div>

                    <div className="irod-total-row" style={{ fontWeight: 600 }}>
                        <span>NCC cần trả</span>
                        <span>{fmt(needToReturn)}</span>
                    </div>

                    <div className="irod-total-row" style={{ fontWeight: 600 }}>
                        <span>NCC đã trả</span>
                        <span className="irod-val-blue">{fmt(amountPaid)}</span>
                    </div>
                </div>

                {/* People */}
                <div className="irod-card irod-people-card">
                    <div className="irod-people-row">
                        <div>
                            <div className="irod-people-label">Người tạo</div>
                            <div className="irod-people-name">{order.createdByUsername || '---'}</div>
                        </div>
                        <div>
                            <div className="irod-people-label">Người nhận trả</div>
                            <div className="irod-people-name">{order.receivedByUsername || order.createdByUsername || '---'}</div>
                        </div>
                    </div>
                </div>
            </IonContent>

            <IonToast
                isOpen={toast !== null}
                message={toast ?? ''}
                duration={2000}
                onDidDismiss={() => setToast(null)}
            />
        </IonPage>
    );
};

export default ImportReturnOrderDetailPage;
