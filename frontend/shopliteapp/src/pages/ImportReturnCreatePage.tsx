import React, { useState, useEffect } from 'react';
import {
    IonPage, IonHeader, IonToolbar, IonContent, IonIcon, IonButtons, IonButton,
    IonSpinner, IonToast, IonFooter, IonActionSheet, useIonRouter, useIonViewWillEnter
} from '@ionic/react';
import { chevronBackOutline, informationCircleOutline, person } from 'ionicons/icons';
import { useParams } from 'react-router';
import { importOrderService } from '../services/importOrder.service';
import { importReturnOrderService } from '../services/importReturnOrder.service';
import { getStoredUser } from '../utils/Apis';
import { useStorePermissions } from '../utils/useStorePermissions';
import type { ImportReturnOrderUpsert } from '../api/types';
import './ImportReturnCreatePage.css';

type ReturnItem = {
    productId: number;
    name: string;
    sku: string;
    returnQty: number;
    maxQty: number;
    returnPrice: number;
};

const fmt = (n: number) => n.toLocaleString('vi-VN');

const ImportReturnCreatePage: React.FC = () => {
    const { importOrderId } = useParams<{ importOrderId: string }>();
    const ionRouter = useIonRouter();
    const { can } = useStorePermissions();
    const canCreateImportReturn = can('/api/v1/import-return-orders', 'POST');

    const [loading, setLoading] = useState(false);
    const [supplierId, setSupplierId] = useState<number>(0);
    const [supplierName, setSupplierName] = useState<string>('');
    const [supplierPhone, setSupplierPhone] = useState<string>('');
    const [importDate, setImportDate] = useState<string>('');
    
    const [items, setItems] = useState<ReturnItem[]>([]);
    
    // Checkout state
    const [checkoutMode, setCheckoutMode] = useState(false);
    const [discountType, setDiscountType] = useState<'VND' | '%'>('VND');
    const [discountValue, setDiscountValue] = useState<number>(0);
    const [fee, setFee] = useState<number>(0); // Chi phí nhập hoàn lại (chưa có trong backend, có thể bỏ qua hoặc lưu vào note/giảm giá)
    const [paidAmount, setPaidAmount] = useState<number>(0); // NCC đã trả
    const [paymentMethod, setPaymentMethod] = useState<'CASH' | 'TRANSFER' | 'CARD'>('CASH');
    const [showBankPicker, setShowBankPicker] = useState(false);
    const [selectedBank, setSelectedBank] = useState<string>('');
    const [saving, setSaving] = useState(false);
    const [toast, setToast] = useState<string | null>(null);

    const handlePaymentMethodChange = (method: 'CASH' | 'TRANSFER' | 'CARD') => {
        setPaymentMethod(method);
        if (method === 'TRANSFER') {
            setShowBankPicker(true);
        } else {
            setSelectedBank('');
        }
    };

    useIonViewWillEnter(() => {
        if (importOrderId) {
            loadImportOrder();
        }
    });

    const loadImportOrder = async () => {
        setLoading(true);
        try {
            const data = await importOrderService.getById(importOrderId);
            setSupplierId(data.supplierId);
            setSupplierName(data.supplierName || 'Nhà cung cấp');
            setSupplierPhone(data.supplierPhone || '');
            setImportDate(data.createdAt || '');
            
            const returnItems: ReturnItem[] = (data.items || [])
                .filter(item => item.quantity - (item.returnedQuantity || 0) > 0)
                .map(item => ({
                    productId: item.productId,
                    name: item.productName || 'Sản phẩm',
                    sku: item.productSku || '',
                    returnQty: 0,
                    maxQty: item.quantity - (item.returnedQuantity || 0),
                    returnPrice: item.importPrice
                }));
            setItems(returnItems);
        } catch (err: any) {
            setToast('Không thể tải thông tin phiếu nhập');
        } finally {
            setLoading(false);
        }
    };

    const updateQty = (productId: number, delta: number) => {
        setItems(prev => prev.map(item => {
            if (item.productId === productId) {
                const newQty = Math.max(0, Math.min(item.maxQty, item.returnQty + delta));
                return { ...item, returnQty: newQty };
            }
            return item;
        }));
    };

    const setQtyDirectly = (productId: number, val: string) => {
        const num = parseInt(val, 10);
        if (isNaN(num)) return;
        setItems(prev => prev.map(item => {
            if (item.productId === productId) {
                const newQty = Math.max(0, Math.min(item.maxQty, num));
                return { ...item, returnQty: newQty };
            }
            return item;
        }));
    };

    const returningItems = items.filter(i => i.returnQty > 0);
    const totalAmount = returningItems.reduce((sum, item) => sum + (item.returnQty * item.returnPrice), 0);
    const totalQty = returningItems.reduce((sum, item) => sum + item.returnQty, 0);
    
    const actualDiscount = discountType === 'VND' ? discountValue : (totalAmount * discountValue / 100);
    const needToPay = Math.max(0, totalAmount - actualDiscount - fee); // NCC cần trả
    const debtAmount = paidAmount - needToPay; // Tính vào công nợ (âm là NCC nợ mình)

    const handleSave = async (asDraft: boolean) => {
        if (!canCreateImportReturn) {
            setToast('Bạn không có quyền tạo phiếu trả hàng nhập');
            return;
        }
        if (returningItems.length === 0) {
            setToast('Vui lòng chọn sản phẩm để trả');
            return;
        }

        setSaving(true);
        try {
            const payload: ImportReturnOrderUpsert = {
                supplierId: supplierId,
                importOrderId: parseInt(importOrderId),
                items: returningItems.map(item => ({
                    productId: item.productId,
                    quantity: item.returnQty,
                    returnPrice: item.returnPrice
                })),
                discount: actualDiscount,
                amountPaid: asDraft ? 0 : paidAmount,
                note: asDraft ? 'Lưu tạm' : '',
                createdByUsername: (getStoredUser() as any)?.username ?? 'Unknown',
                receivedByUsername: supplierName,
            };

            await importReturnOrderService.create(payload);
            setToast('Tạo phiếu trả hàng thành công');
            setTimeout(() => {
                ionRouter.goBack();
            }, 1000);
        } catch (err: any) {
            setToast(err.message || 'Lỗi khi tạo phiếu trả hàng');
        } finally {
            setSaving(false);
        }
    };

    const pad = (v: number) => String(v).padStart(2, '0');
    const formatDate = (dateStr: string) => {
        if (!dateStr) return '';
        const d = new Date(dateStr);
        return `${pad(d.getDate())}/${pad(d.getMonth() + 1)}/${d.getFullYear()} ${pad(d.getHours())}:${pad(d.getMinutes())}`;
    };

    if (loading) {
        return (
            <IonPage className="iro-create-page">
                <IonContent><div style={{display: 'flex', justifyContent: 'center', marginTop: '50px'}}><IonSpinner name="crescent" color="primary"/></div></IonContent>
            </IonPage>
        );
    }

    if (checkoutMode) {
        return (
            <IonPage className="iro-create-page">
                <IonHeader className="iro-create-header ion-no-border">
                    <IonToolbar className="iro-create-toolbar">
                        <IonButtons slot="start">
                            <IonButton color="dark" onClick={() => setCheckoutMode(false)}>
                                <IonIcon icon={chevronBackOutline} style={{ fontSize: '24px'}} />
                            </IonButton>
                        </IonButtons>
                        <div className="iro-create-title">Thanh toán trả hàng nhập</div>
                    </IonToolbar>
                </IonHeader>

                <IonContent>
                    <div className="iro-co-row" style={{ marginTop: '12px' }}>
                        <div className="iro-co-label-block">
                            <div className="iro-co-label">Tổng tiền hàng</div>
                            <div className="iro-co-sub" style={{ border: '1px solid #e2e8f0', borderRadius: '4px', padding: '2px 6px', display: 'inline-block', width: 'fit-content' }}>{totalQty}</div>
                        </div>
                        <div className="iro-co-value-box">{fmt(totalAmount)}</div>
                    </div>

                    <div className="iro-co-row">
                        <div className="iro-co-label">Giảm giá</div>
                        <div className="iro-co-input-group">
                            <div className="iro-co-type-toggle">
                                <div className={`iro-co-type-btn ${discountType === 'VND' ? 'active' : ''}`} onClick={() => setDiscountType('VND')}>VND</div>
                                <div className={`iro-co-type-btn ${discountType === '%' ? 'active' : ''}`} onClick={() => setDiscountType('%')}>%</div>
                            </div>
                            <input 
                                type="number" 
                                className="iro-co-input" 
                                value={discountValue === 0 ? '' : discountValue} 
                                onChange={(e) => setDiscountValue(Number(e.target.value) || 0)} 
                                placeholder="0" 
                            />
                        </div>
                    </div>

                    <div className="iro-co-row">
                        <div className="iro-co-label">Chi phí nhập hoàn lại</div>
                        <input 
                            type="number" 
                            className="iro-co-input" 
                            value={fee === 0 ? '' : fee} 
                            onChange={(e) => setFee(Number(e.target.value) || 0)} 
                            placeholder="0" 
                        />
                    </div>

                    <div className="iro-co-row">
                        <div className="iro-co-label" style={{ fontWeight: 600 }}>NCC cần trả</div>
                        <div className="iro-co-value-box iro-co-highlight">{fmt(needToPay)}</div>
                    </div>

                    <div className="iro-co-row">
                        <div className="iro-co-label" style={{ display: 'flex', alignItems: 'center', gap: '8px'}}>
                            NCC đã trả
                            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="#94a3b8" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                                <rect x="2" y="6" width="20" height="12" rx="2" />
                                <circle cx="12" cy="12" r="2" />
                            </svg>
                        </div>
                        <input 
                            type="number" 
                            className="iro-co-input" 
                            value={paidAmount === 0 ? '' : paidAmount} 
                            onChange={(e) => setPaidAmount(Number(e.target.value) || 0)} 
                            placeholder="0" 
                        />
                    </div>

                    <div className="iro-co-methods">
                        <div className={`iro-co-method ${paymentMethod === 'CASH' ? 'active' : ''}`} onClick={() => handlePaymentMethodChange('CASH')}>
                            <div className="iro-co-method-radio"></div> Tiền mặt
                        </div>
                        <div className={`iro-co-method ${paymentMethod === 'TRANSFER' ? 'active' : ''}`} onClick={() => handlePaymentMethodChange('TRANSFER')}>
                            <div className="iro-co-method-radio"></div> Chuyển khoản
                        </div>
                        <div className={`iro-co-method ${paymentMethod === 'CARD' ? 'active' : ''}`} onClick={() => handlePaymentMethodChange('CARD')}>
                            <div className="iro-co-method-radio"></div> Thẻ
                        </div>
                    </div>

                    {paymentMethod === 'TRANSFER' && selectedBank && (
                        <div className="iro-co-row" style={{ padding: '8px 16px', margin: '0 16px 20px', background: '#f8fafc', borderRadius: '8px', cursor: 'pointer' }} onClick={() => setShowBankPicker(true)}>
                            <div className="iro-co-label" style={{ fontSize: '13px', color: '#64748b' }}>Tài khoản:</div>
                            <div className="iro-co-value-box" style={{ padding: 0, minWidth: 'auto', fontSize: '14px', fontWeight: 500, color: '#0066FF' }}>{selectedBank}</div>
                        </div>
                    )}

                    <div className="iro-co-debt-row">
                        <div className="iro-co-label">Tính vào công nợ</div>
                        <div className="iro-co-value-box">{fmt(debtAmount)}</div>
                    </div>
                </IonContent>

                <IonFooter className="ion-no-border">
                    <div className="iro-co-actions">
                        <button className="iro-co-btn-draft" onClick={() => handleSave(true)} disabled={saving || !canCreateImportReturn}>Lưu tạm</button>
                        <button className="iro-co-btn-complete" onClick={() => handleSave(false)} disabled={saving || !canCreateImportReturn}>
                            {saving ? <IonSpinner name="dots" /> : 'Hoàn thành'}
                        </button>
                    </div>
                </IonFooter>
                
                <IonActionSheet
                    isOpen={showBankPicker}
                    onDidDismiss={(e) => {
                        setShowBankPicker(false);
                        const role = e.detail.role;
                        if (role === 'cancel' || role === 'backdrop') {
                            if (!selectedBank) setPaymentMethod('CASH');
                        }
                    }}
                    header="Chọn tài khoản ngân hàng"
                    buttons={[
                        {
                            text: 'Vietcombank - 123456789',
                            handler: () => { setSelectedBank('Vietcombank - 123456789'); }
                        },
                        {
                            text: 'MB Bank - 987654321',
                            handler: () => { setSelectedBank('MB Bank - 987654321'); }
                        },
                        {
                            text: 'Hủy',
                            role: 'cancel',
                            handler: () => { 
                                if (!selectedBank) setPaymentMethod('CASH'); 
                            }
                        }
                    ]}
                />
                
                <IonToast isOpen={toast !== null} message={toast ?? ''} duration={2000} onDidDismiss={() => setToast(null)} />
            </IonPage>
        );
    }

    return (
        <IonPage className="iro-create-page">
            <IonHeader className="iro-create-header ion-no-border">
                <IonToolbar className="iro-create-toolbar">
                    <IonButtons slot="start">
                        <IonButton color="dark" onClick={() => ionRouter.goBack()}>
                            <IonIcon icon={chevronBackOutline} style={{ fontSize: '24px'}} />
                        </IonButton>
                    </IonButtons>
                    <div className="iro-create-title">
                        Trả hàng nhập
                    </div>
                    <IonButtons slot="end">
                        <IonButton color="dark">
                            <IonIcon icon={informationCircleOutline} style={{ color: '#8e9fb8ff', fontSize: '22px' }} />
                        </IonButton>
                    </IonButtons>
                </IonToolbar>
            </IonHeader>

            <IonContent className="iro-create-content">
                <div className="iro-create-info-card">
                    <div className="iro-create-supplier-row">
                        <div className="iro-create-supplier-name">
                            <IonIcon icon={person} style={{color: '#94a3b8'}} /> {supplierName}
                        </div>
                        <div className="iro-create-supplier-phone">{supplierPhone}</div>
                    </div>
                    <div className="iro-create-ref-row">
                        <span className="iro-create-ref-code">PN{String(importOrderId).padStart(6, '0')}</span>
                        <span>{formatDate(importDate)}</span>
                    </div>
                </div>

                <div className="iro-create-items-card">
                    {items.map(item => (
                        <div key={item.productId} className="iro-create-item">
                            <div className="iro-create-item-name">{item.name}</div>
                            <div className="iro-create-item-sku">{item.sku || '---'}</div>
                            <div className="iro-create-item-controls">
                                <div className="iro-create-item-price">
                                    {fmt(item.returnPrice)} <span className="iro-create-item-price-x">x</span>
                                </div>
                                <div className="iro-create-qty-wrapper">
                                    <input 
                                        type="number" 
                                        className="iro-create-qty-input" 
                                        value={item.returnQty === 0 ? '' : item.returnQty}
                                        onChange={(e) => setQtyDirectly(item.productId, e.target.value)}
                                        placeholder="0"
                                    />
                                    <span className="iro-create-qty-max">/{item.maxQty}</span>
                                    <div className="iro-create-qty-btn" onClick={() => updateQty(item.productId, -1)}>−</div>
                                    <div className="iro-create-qty-btn" onClick={() => updateQty(item.productId, 1)}>+</div>
                                </div>
                            </div>
                        </div>
                    ))}
                </div>
            </IonContent>

            <IonFooter className="ion-no-border">
                <div className="iro-create-footer">
                    <button 
                        className="iro-create-btn-pay" 
                        onClick={() => {
                            if (returningItems.length === 0) {
                                setToast('Vui lòng chọn số lượng để trả');
                                return;
                            }
                            setCheckoutMode(true);
                        }}
                        disabled={!canCreateImportReturn}
                    >
                        Thanh toán
                    </button>
                </div>
            </IonFooter>

            <IonToast isOpen={toast !== null} message={toast ?? ''} duration={2000} onDidDismiss={() => setToast(null)} />
        </IonPage>
    );
};

export default ImportReturnCreatePage;
