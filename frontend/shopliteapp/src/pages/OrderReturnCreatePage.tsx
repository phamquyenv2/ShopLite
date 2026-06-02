import React, { useState } from 'react';
import {
    IonPage, IonHeader, IonToolbar, IonContent, IonIcon, IonButtons, IonButton,
    IonInput, IonSpinner, IonToast, useIonRouter
} from '@ionic/react';
import { chevronBackOutline, searchOutline, checkmarkCircleOutline, closeCircleOutline } from 'ionicons/icons';
import { authApis, endpoints } from '../utils/Apis';
import type { Order } from '../api/types';
import './OrderReturnCreatePage.css';

const fmt = (n?: number | null) => (n ?? 0).toLocaleString('vi-VN');

interface ReturnItemState {
    productId: number;
    productName: string;
    maxQty: number;      // from order item quantity
    returnQty: number;   // currently selected to return
    refundPrice: number; // unit price
    isResellable: boolean; // true = nhập kho, false = hàng lỗi
}

const OrderReturnCreatePage: React.FC = () => {
    const ionRouter = useIonRouter();
    const [searchCode, setSearchCode] = useState('');
    const [loading, setLoading] = useState(false);
    const [order, setOrder] = useState<Order | null>(null);
    const [errorMsg, setErrorMsg] = useState('');
    const [toast, setToast] = useState<string | null>(null);

    // State for return items
    const [returnItems, setReturnItems] = useState<ReturnItemState[]>([]);

    const handleSearch = async () => {
        if (!searchCode.trim()) return;
        setLoading(true);
        setErrorMsg('');
        setOrder(null);
        setReturnItems([]);
        
        try {
            // Simulated search by code, falling back to ID if it's numeric for API
            let searchParam = searchCode.trim();
            if (searchParam.toUpperCase().startsWith('HD')) {
                searchParam = searchParam.substring(2);
            }
            const res = await authApis().get<any>(`${endpoints.orders}/${Number(searchParam)}`);
            const fetchedOrder = res.data?.data || res.data;
            
            if (!fetchedOrder || fetchedOrder.status !== 'COMPLETED') {
                setErrorMsg('Không tìm thấy hóa đơn, hoặc hóa đơn chưa hoàn thành!');
                return;
            }

            setOrder(fetchedOrder);
            
            // Map items
            if (fetchedOrder.items) {
                const initItems = fetchedOrder.items.map((it: any) => ({
                    productId: it.productId,
                    productName: it.productName || 'Sản phẩm',
                    maxQty: it.quantity || 0,
                    returnQty: 0,
                    refundPrice: it.unitPrice || 0,
                    isResellable: true // default to resellable
                }));
                setReturnItems(initItems);
            }

        } catch (e) {
            setErrorMsg('Lỗi khi tìm hóa đơn! Vui lòng nhập đúng mã HD (vd: HD000006 hoặc 6)');
        } finally {
            setLoading(false);
        }
    };

    const updateQty = (idx: number, delta: number) => {
        setReturnItems(prev => {
            const next = [...prev];
            const item = next[idx];
            let newQty = item.returnQty + delta;
            if (newQty < 0) newQty = 0;
            if (newQty > item.maxQty) newQty = item.maxQty;
            item.returnQty = newQty;
            return next;
        });
    };

    const updateCondition = (idx: number, isResellable: boolean) => {
        setReturnItems(prev => {
            const next = [...prev];
            next[idx].isResellable = isResellable;
            return next;
        });
    };

    const submitReturn = async () => {
        const itemsToReturn = returnItems.filter(i => i.returnQty > 0);
        if (itemsToReturn.length === 0) {
            setToast('Vui lòng chọn ít nhất 1 sản phẩm để trả!');
            return;
        }

        const payload = {
            orderId: order?.id,
            items: itemsToReturn.map(i => ({
                productId: i.productId,
                quantity: i.returnQty,
                refundPrice: i.refundPrice,
                isResellable: i.isResellable
            }))
        };

        try {
            // Mocking the backend call to order-returns POST
            // await authApis().post('/api/v1/order-returns', payload);
            console.log('Submitted payload:', payload);
            setToast('Tạo phiếu trả hàng thành công!');
            setTimeout(() => {
                ionRouter.goBack();
            }, 1000);
        } catch (e) {
            setToast('Lỗi khi trả hàng!');
        }
    };

    const totalRefund = returnItems.reduce((sum, item) => sum + (item.returnQty * item.refundPrice), 0);
    const hasItemsToReturn = returnItems.some(i => i.returnQty > 0);

    return (
        <IonPage className="ret-create-page">
            <IonHeader className="ret-create-header ion-no-border">
                <div className="ret-top-card">
                    <IonToolbar className="ret-create-toolbar">
                        <IonButtons slot="start">
                            <IonButton color="dark" onClick={() => ionRouter.goBack()}>
                                <IonIcon icon={chevronBackOutline} style={{ fontSize: '26px' }} />
                            </IonButton>
                        </IonButtons>
                        <div className="ret-title">Trả hàng</div>
                    </IonToolbar>

                    <div className="ret-search-box">
                        <IonIcon icon={searchOutline} style={{ marginLeft: '12px', color: '#64748b' }} />
                        <IonInput 
                            placeholder="Nhập mã hóa đơn (VD: HD000006)"
                            value={searchCode}
                            onIonInput={e => setSearchCode(e.detail.value!)}
                            onKeyDown={e => e.key === 'Enter' && handleSearch()}
                        />
                        <button className="ret-search-btn" onClick={handleSearch}>Tìm</button>
                    </div>
                </div>
            </IonHeader>

            <IonContent className="ret-create-content">
                {loading && <div style={{ textAlign: 'center', marginTop: '40px' }}><IonSpinner name="crescent" /></div>}

                {!loading && errorMsg && <div className="ret-error-msg">{errorMsg}</div>}
                
                {!loading && !order && !errorMsg && (
                    <div className="ret-info-msg">Vui lòng quét hoặc nhập mã hóa đơn để tiếp tục</div>
                )}

                {order && (
                    <>
                        <div className="ret-order-info">
                            <div className="ret-info-row">
                                <span className="ret-info-label">Mã hóa đơn:</span>
                                <span className="ret-info-val">HD{String(order.id).padStart(6, '0')}</span>
                            </div>
                            <div className="ret-info-row">
                                <span className="ret-info-label">Khách hàng:</span>
                                <span className="ret-info-val">{order.customerName || 'Khách lẻ'}</span>
                            </div>
                            <div className="ret-info-row">
                                <span className="ret-info-label">Tổng tiền HĐ:</span>
                                <span className="ret-info-val">{fmt(order.totalAmount)}</span>
                            </div>
                            <div className="ret-info-row">
                                <span className="ret-info-label">Trạng thái:</span>
                                <span className="ret-info-val" style={{ color: '#16a34a' }}>Đã hoàn thành</span>
                            </div>
                        </div>

                        <div className="ret-section-title">Chọn sản phẩm trả</div>
                        <div className="ret-items-list">
                            {returnItems.map((item, idx) => (
                                <div className="ret-item" key={idx}>
                                    <div className="ret-item-top">
                                        <div>
                                            <div className="ret-item-name">{item.productName}</div>
                                            <div className="ret-item-max">Đã mua: {item.maxQty}</div>
                                        </div>
                                        <div style={{ textAlign: 'right' }}>
                                            <div className="ret-item-price">{fmt(item.refundPrice)} đ</div>
                                            <div className="ret-qty-control">
                                                <button className="ret-qty-btn" onClick={() => updateQty(idx, -1)}>-</button>
                                                <input className="ret-qty-input" type="number" readOnly value={item.returnQty} />
                                                <button className="ret-qty-btn" onClick={() => updateQty(idx, 1)}>+</button>
                                            </div>
                                        </div>
                                    </div>

                                    {item.returnQty > 0 && (
                                        <div className="ret-condition-wrap">
                                            <button 
                                                className={`ret-cond-btn ${item.isResellable ? 'active-resell' : ''}`}
                                                onClick={() => updateCondition(idx, true)}
                                            >
                                                <IonIcon icon={checkmarkCircleOutline} style={{ verticalAlign: 'middle', marginRight: '4px' }} />
                                                Nhập lại kho
                                            </button>
                                            <button 
                                                className={`ret-cond-btn ${!item.isResellable ? 'active-defect' : ''}`}
                                                onClick={() => updateCondition(idx, false)}
                                            >
                                                <IonIcon icon={closeCircleOutline} style={{ verticalAlign: 'middle', marginRight: '4px' }} />
                                                Hàng lỗi
                                            </button>
                                        </div>
                                    )}
                                </div>
                            ))}
                        </div>
                    </>
                )}
            </IonContent>

            {order && (
                <div className="ret-footer" slot="fixed" style={{ position: 'absolute', bottom: 0, width: '100%' }}>
                    <div className="ret-total-row">
                        <span className="ret-total-label">Tiền hoàn khách:</span>
                        <span className="ret-total-val">{fmt(totalRefund)} ₫</span>
                    </div>
                    <button 
                        className="ret-submit-btn" 
                        disabled={!hasItemsToReturn}
                        onClick={submitReturn}
                    >
                        Xác nhận hoàn tiền
                    </button>
                </div>
            )}

            <IonToast isOpen={toast !== null} message={toast ?? ''} duration={2000} onDidDismiss={() => setToast(null)} />
        </IonPage>
    );
};

export default OrderReturnCreatePage;
