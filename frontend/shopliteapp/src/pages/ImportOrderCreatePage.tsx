import React, { useState, useEffect, useRef } from 'react';
import {
    IonPage, IonHeader, IonToolbar, IonContent, IonIcon, IonButtons, IonButton,
    IonSpinner, IonToast, IonFooter, IonModal, useIonRouter, useIonViewWillEnter
} from '@ionic/react';
import {
    closeOutline, searchOutline, addOutline, informationCircleOutline, chevronBackOutline,
    chevronDownOutline, removeCircleOutline, addCircleOutline, trashOutline
} from 'ionicons/icons';
import { useParams } from 'react-router';
import { importOrderService } from '../services/importOrder.service';
import { supplierService } from '../services/supplier.service';
import { productService } from '../services/product.service';
import { authApis, endpoints } from '../utils/Apis';
import type { Product, Supplier, ImportOrderUpsert } from '../api/types';
import SupplierPickerModal from './SupplierPickerModal';
import './ImportOrderCreatePage.css';

type CartItem = {
    productId: number;
    name: string;
    sku: string;
    quantity: number;
    importPrice: number;
    stock: number;
    imageUrl?: string;
};

const fmt = (n: number) => n.toLocaleString('vi-VN');

const ImportOrderCreatePage: React.FC = () => {
    const { id } = useParams<{ id: string }>();
    const isEditMode = Boolean(id);
    const ionRouter = useIonRouter();

    const [suppliers, setSuppliers] = useState<Supplier[]>([]);
    const [selectedSupplier, setSelectedSupplier] = useState<Supplier | null>(null);
    const [showSupplierPicker, setShowSupplierPicker] = useState(false);

    // Product search — gọi API trực tiếp với debounce 500ms
    const [searchText, setSearchText] = useState('');
    const [searchResults, setSearchResults] = useState<Product[]>([]);
    const [searching, setSearching] = useState(false);
    const [showProductSearch, setShowProductSearch] = useState(false);
    const debounceRef = useRef<ReturnType<typeof setTimeout> | null>(null);

    const [cart, setCart] = useState<CartItem[]>([]);
    const [saving, setSaving] = useState(false);
    const [toast, setToast] = useState<string | null>(null);

    // Checkout states
    const [checkoutMode, setCheckoutMode] = useState(false);
    const [discount, setDiscount] = useState<number>(0);
    const [paidAmount, setPaidAmount] = useState<number>(0);
    const [paymentMethod, setPaymentMethod] = useState<'CASH' | 'TRANSFER' | 'CARD'>('CASH');
    const [note, setNote] = useState('');

    // Trạng thái thực tế của order từ server (dùng để xác định retry flow)
    const [orderStatus, setOrderStatus] = useState<string | null>(null);

    useIonViewWillEnter(() => {
        loadInitial();
    });

    const loadInitial = async () => {
        try {
            const [s] = await Promise.all([
                supplierService.getAll(),
            ]);
            setSuppliers(s);

            if (isEditMode) {
                const orderData = await importOrderService.getById(id);
                const sup = s.find(sup => sup.id === orderData.supplierId);
                if (sup) setSelectedSupplier(sup);

                // Lưu status thực tế — dùng để phân nhánh retry flow
                setOrderStatus(orderData.status ?? null);

                setCart(orderData.items?.map(item => ({
                    productId: item.productId,
                    name: item.productName || 'Sản phẩm',
                    sku: item.productSku || '',
                    quantity: item.quantity,
                    importPrice: item.importPrice,
                    stock: 0,
                    imageUrl: ''
                })) || []);
                setDiscount(orderData.discount || 0);
                if (orderData.note && orderData.note !== 'Lưu tạm') setNote(orderData.note);
            }
        } catch { /* */ }
    };

    // Debounce search — gọi API mỗi khi người dùng gõ, sau 500ms
    const handleSearchChange = (value: string) => {
        setSearchText(value);
        if (debounceRef.current) clearTimeout(debounceRef.current);
        if (!value.trim()) { setSearchResults([]); return; }
        setSearching(true);
        debounceRef.current = setTimeout(async () => {
            try {
                const results = await productService.searchForImport(value.trim());
                setSearchResults(results);
            } catch { setSearchResults([]); }
            finally { setSearching(false); }
        }, 500);
    };

    const addToCart = (prod: Product) => {
        const existing = cart.find(c => c.productId === prod.id);
        if (existing) {
            setCart(cart.map(c => c.productId === prod.id ? { ...c, quantity: c.quantity + 1 } : c));
        } else {
            setCart([...cart, {
                productId: prod.id,
                name: prod.name,
                sku: prod.sku || '',
                quantity: 1,
                importPrice: prod.costPrice || 0,
                stock: prod.stock || 0,
                imageUrl: prod.image || ''
            }]);
        }
        setShowProductSearch(false);
        setSearchText('');
        setSearchResults([]);
    };

    const updateQty = (productId: number, delta: number) => {
        setCart(prev => {
            const copy = prev.map(c => {
                if (c.productId !== productId) return c;
                return { ...c, quantity: c.quantity + delta };
            });
            return copy.filter(c => c.quantity > 0);
        });
    };

    const removeItem = (productId: number) => {
        setCart(prev => prev.filter(c => c.productId !== productId));
    };

    const updatePrice = (productId: number, price: number) => {
        setCart(prev => prev.map(c => c.productId === productId ? { ...c, importPrice: price } : c));
    };

    const totalAmount = cart.reduce((s, c) => s + c.quantity * c.importPrice, 0);
    const totalQty = cart.reduce((s, c) => s + c.quantity, 0);
    const needToPay = Math.max(0, totalAmount - (discount || 0));
    const debtAmount = (paidAmount || 0) - needToPay; // âm là công nợ

    const handleSave = async (asDraft: boolean) => {
        if (!selectedSupplier) { setToast('Vui lòng chọn nhà cung cấp'); return; }
        if (cart.length === 0) { setToast('Vui lòng thêm sản phẩm'); return; }

        setSaving(true);
        try {
            if (asDraft) {
                // Lưu tạm: luôn update hoặc create bình thường
                const payload: ImportOrderUpsert = {
                    supplierId: selectedSupplier.id,
                    items: cart.map(c => ({
                        productId: c.productId,
                        quantity: c.quantity,
                        importPrice: c.importPrice
                    })),
                    note: 'Lưu tạm',
                    discount: discount,
                    paidAmount: 0,
                    status: 'PENDING'
                };
                if (isEditMode && id) {
                    await importOrderService.update(id, payload);
                } else {
                    await importOrderService.create(payload);
                }
                setToast('Lưu tạm thành công');
            } else {
                // Hoàn thành: phân nhánh theo trạng thái hiện tại
                //
                // PENDING_PAYMENT: bước confirm trước đó đã thành công, chỉ cần thực hiện
                //   payment để hoàn tất. TUYỆT ĐỐI KHÔNG gọi update/confirm lại.
                //
                // DRAFT / PENDING: chưa confirm, thực hiện đầy đủ cả 2 bước.
                if (isEditMode && id && orderStatus === 'PENDING_PAYMENT') {
                    // Retry: chỉ gọi payment
                    await importOrderService.payOnly(id, {
                        paidAmount,
                        paymentMethod,
                        note,
                    });
                    setToast('Thanh toán phệu nhập thành công');
                } else {
                    // Lần đầu: update (nếu edit) + hoàn thành
                    const payload: ImportOrderUpsert = {
                        supplierId: selectedSupplier.id,
                        items: cart.map(c => ({
                            productId: c.productId,
                            quantity: c.quantity,
                            importPrice: c.importPrice
                        })),
                        note,
                        discount,
                        paidAmount,
                        paymentMethod,
                        status: 'COMPLETED'
                    };
                    if (isEditMode && id) {
                        await importOrderService.update(id, payload);
                        setToast('Cập nhật phiếu nhập thành công');
                    } else {
                        await importOrderService.create(payload);
                        setToast('Tạo phiếu nhập thành công');
                    }
                }
            }
            setTimeout(() => ionRouter.goBack(), 500);
        } catch (err: any) {
            setToast(err.message || 'Lỗi xử lý phiếu nhập');
        } finally {
            setSaving(false);
        }
    };

    // Chỉ filter khi người dùng đã nhập tìm kiếm
    const trimmedSearch = searchText.trim();


    if (checkoutMode) {
        return (
            <IonPage className="ioc-page checkout-page">
                <IonHeader className="ioc-header ion-no-border">
                    <IonToolbar className="ioc-toolbar">
                        <IonButtons slot="start">
                            <IonButton color="dark" onClick={() => setCheckoutMode(false)}>
                                <IonIcon icon={chevronBackOutline} style={{ fontSize: '26px'}} />
                            </IonButton>
                        </IonButtons>
                    </IonToolbar>
                </IonHeader>

                <IonContent className="ioc-content">
                    <div className="ioc-checkout-view-cart" onClick={() => setCheckoutMode(false)}>
                        <div className="ioc-cvc-icon">
                            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                                <path d="M21 16V8a2 2 0 0 0-1-1.73l-7-4a2 2 0 0 0-2 0l-7 4A2 2 0 0 0 3 8v8a2 2 0 0 0 1 1.73l7 4a2 2 0 0 0 2 0l7-4A2 2 0 0 0 21 16z"></path><polyline points="3.27 6.96 12 12.01 20.73 6.96"></polyline><line x1="12" y1="22.08" x2="12" y2="12"></line>
                            </svg>
                        </div>
                        <span>Xem hàng trong phiếu</span>
                        <IonIcon icon={chevronBackOutline} style={{transform: 'rotate(180deg)', marginLeft: 'auto', color: '#94a3b8'}} />
                    </div>

                    <div className="ioc-checkout-card">
                        <div className="ioc-co-row">
                            <div className="ioc-co-label-block">
                                <div className="ioc-co-label">Tổng tiền hàng</div>
                                <div className="ioc-co-sub">{cart.length} Mặt hàng • Số lượng {totalQty}</div>
                            </div>
                            <div className="ioc-co-value-box read-only">{fmt(totalAmount)}</div>
                        </div>

                        <div className="ioc-co-row">
                            <div className="ioc-co-label">Giảm giá</div>
                            <input type="number" className="ioc-co-input" value={discount === 0 ? '' : discount} onChange={e => setDiscount(Number(e.target.value) || 0)} placeholder="0" />
                        </div>

                        <div className="ioc-co-row">
                            <div className="ioc-co-label">Cần trả NCC</div>
                            <div className="ioc-co-value-box highlight-box">{fmt(needToPay)}</div>
                        </div>

                        <div className="ioc-co-row">
                            <div className="ioc-co-label">Tiền trả NCC</div>
                            <input type="number" className="ioc-co-input" value={paidAmount === 0 ? '' : paidAmount} onChange={e => setPaidAmount(Number(e.target.value) || 0)} placeholder="0" />
                        </div>

                        <div className="ioc-co-methods">
                            <div className={`ioc-co-method ${paymentMethod === 'CASH' ? 'active' : ''}`} onClick={() => setPaymentMethod('CASH')}>Tiền mặt</div>
                            <div className={`ioc-co-method ${paymentMethod === 'TRANSFER' ? 'active' : ''}`} onClick={() => setPaymentMethod('TRANSFER')}>Chuyển khoản</div>
                            <div className={`ioc-co-method ${paymentMethod === 'CARD' ? 'active' : ''}`} onClick={() => setPaymentMethod('CARD')}>Thẻ</div>
                        </div>

                        <div className="ioc-co-row ioc-co-debt">
                            <div className="ioc-co-label">Tính vào công nợ</div>
                            <div className="ioc-co-debt-value">{fmt(debtAmount)}</div>
                        </div>
                    </div>

                    <div className="ioc-checkout-card">
                        <input type="text" className="ioc-co-note" placeholder="Thêm ghi chú" value={note} onChange={e => setNote(e.target.value)} />
                    </div>
                </IonContent>

                <IonFooter className="ioc-footer ion-no-border">
                    <div className="ioc-footer-actions">
                        <button className="ioc-btn-draft" onClick={() => handleSave(true)} disabled={saving}>Lưu tạm</button>
                        <button className="ioc-btn-save" onClick={() => handleSave(false)} disabled={saving}>
                            {saving ? <IonSpinner name="dots" /> : 'Hoàn thành'}
                        </button>
                    </div>
                </IonFooter>
            </IonPage>
        );
    }

    return (
        <IonPage className="ioc-page">
            <IonHeader className="ioc-header ion-no-border">
                <div className="ioc-top-card">
                    <IonToolbar className="ioc-toolbar">
                        <IonButtons slot="start">
                            <IonButton color="dark" onClick={() => ionRouter.goBack()}>
                                <IonIcon icon={chevronBackOutline} style={{ fontSize: '26px'}} />
                            </IonButton>
                        </IonButtons>
                        <div className="ioc-title">Phiếu nhập hàng mới</div>
                        <IonButtons slot="end">
                            <IonButton color="dark">
                                <IonIcon icon={informationCircleOutline} style={{ fontSize: '24px'}} />
                            </IonButton>
                        </IonButtons>
                    </IonToolbar>
                    
                    <div className="ioc-actions-row">
                        <div className="ioc-search-bar" onClick={() => setShowProductSearch(true)}>
                            <IonIcon icon={searchOutline} className="ioc-s-icon" />
                            <span className="ioc-search-placeholder">Tên, mã hàng, mã...</span>
                            <div className="ioc-s-actions">
                                <IonIcon 
                                    icon={addOutline} 
                                    className="ioc-s-add-icon" 
                                    onClick={(e) => {
                                        e.stopPropagation();
                                        ionRouter.push('/product/new'); // Default to Add Product, or /orders/new if you strictly meant Sales Order.
                                    }}
                                />
                                <div className="ioc-s-scan">
                                    <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                                        <path d="M4 7V4h3M20 7V4h-3M4 17v3h3M20 17v3h-3M9 8h2v8H9zM13 8h2v8h-2z"/>
                                    </svg>
                                </div>
                            </div>
                        </div>
                        <div className="ioc-category-btn">
                            <svg width="20" height="20" viewBox="0 0 24 24" fill="currentColor">
                                <path d="M3 13h2v-2H3v2zm0 4h2v-2H3v2zm0-8h2V7H3v2zm4 4h14v-2H7v2zm0 4h14v-2H7v2zM7 7v2h14V7H7z"/>
                            </svg>
                            <span>Nhóm hàng</span>
                        </div>
                    </div>
                    
                    <div className="ioc-supplier-row" onClick={() => setShowSupplierPicker(true)}>
                        <div className="ioc-supplier-icon">
                            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="#475569" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                                <path d="M3 21h18M5 21V8l7-5 7 5v13M9 21v-5h6v5M5 11h14"/>
                            </svg>
                        </div>
                        <span className="ioc-supplier-text">
                            {selectedSupplier ? selectedSupplier.name : 'Chọn nhà cung cấp'}
                        </span>
                        <IonIcon icon={chevronDownOutline} className="ioc-chevron" />
                    </div>
                </div>
            </IonHeader>

            <IonContent className="ioc-content">
                {cart.length === 0 ? (
                    <div className="ioc-empty">
                        <div className="ioc-empty-circle">
                            <svg width="60" height="60" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1">
                                <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8l-6-6z" fill="#f8fafc" stroke="#3b82f6" strokeWidth="1.5" />
                                <path d="M14 2v6h6M8 10h5M8 14h8M8 18h4" stroke="#3b82f6" strokeWidth="1.5" strokeLinecap="round" />
                                <path d="M4 12v.01M20 12v.01M12 4v.01M12 20v.01" stroke="#bfdbfe" strokeWidth="2" strokeLinecap="round" />
                            </svg>
                        </div>
                        <p>Chưa có hàng trong phiếu</p>
                    </div>
                ) : (
                    <div className="ioc-cart">
                        {cart.map((item, idx) => (
                            <div key={item.productId} className={`ioc-item ${idx === cart.length - 1 ? 'last-item' : ''}`}>
                                <div className="ioc-item-thumb">
                                    {item.imageUrl ? <img src={item.imageUrl} alt={item.name} /> : <div className="ioc-thumb-ph"></div>}
                                </div>
                                <div className="ioc-item-content">
                                    <div className="ioc-item-name">{item.name}</div>
                                    <div className="ioc-item-sku">{item.sku}</div>
                                    <div className="ioc-item-stockprice">
                                        Tồn kho: {item.stock} • Giá: {fmt(item.importPrice)}
                                    </div>
                                    <div className="ioc-item-controls">
                                        <div className="ioc-qty-capsule">
                                            <div className="ioc-qty-btn" onClick={() => updateQty(item.productId, -1)}>−</div>
                                            <div className="ioc-qty-val">{item.quantity}</div>
                                            <div className="ioc-qty-btn" onClick={() => updateQty(item.productId, 1)}>+</div>
                                        </div>
                                        <div className="ioc-item-subtotal">{fmt(item.quantity * item.importPrice)}</div>
                                    </div>
                                </div>
                            </div>
                        ))}
                    </div>
                )}
            </IonContent>

            <IonFooter className="ioc-footer ion-no-border">
                {cart.length > 0 && (
                    <div className="ioc-total-section">
                        <div className="ioc-total-row">
                            <span>Tổng tiền hàng</span>
                            <span className="ioc-total-val">{fmt(totalAmount)}</span>
                        </div>
                        <div className="ioc-total-sub">
                            {cart.length} mặt hàng • Số lượng: {totalQty}
                        </div>
                    </div>
                )}
                <div className="ioc-footer-actions">
                    <button className="ioc-btn-draft" onClick={() => handleSave(true)} disabled={saving || cart.length === 0}>Lưu tạm</button>
                    <button className="ioc-btn-save" onClick={() => {
                        if (!selectedSupplier) { setToast('Vui lòng chọn nhà cung cấp'); return; }
                        setCheckoutMode(true);
                    }} disabled={cart.length === 0}>
                        Tiếp tục
                    </button>
                </div>
            </IonFooter>

            <IonModal isOpen={showProductSearch} onDidDismiss={() => { setShowProductSearch(false); setSearchText(''); setSearchResults([]); }} className="ioc-modal">
                <IonHeader>
                    <IonToolbar>
                        <IonButtons slot="start">
                            <IonButton onClick={() => setShowProductSearch(false)}>
                                <IonIcon icon={closeOutline} />
                            </IonButton>
                        </IonButtons>
                        <div style={{ padding: '0 8px' }}>
                            <input
                                className="ioc-modal-search"
                                placeholder="Tìm sản phẩm..."
                                value={searchText}
                                onChange={e => handleSearchChange(e.target.value)}
                                autoFocus
                            />
                        </div>
                    </IonToolbar>
                </IonHeader>
                <IonContent>
                    {searching && (
                        <div style={{ display: 'flex', justifyContent: 'center', padding: '32px' }}>
                            <IonSpinner name="crescent" color="primary" />
                        </div>
                    )}
                    <div className="ioc-product-list">
                        {!searching && !trimmedSearch && (
                            <div className="ioc-empty-search ioc-search-hint">
                                Nhập tên, mã hàng hoặc barcode để tìm kiếm
                            </div>
                        )}
                        {!searching && trimmedSearch && searchResults.length === 0 && (
                            <div className="ioc-empty-search">Không tìm thấy sản phẩm</div>
                        )}
                        {!searching && searchResults.map(p => (
                            <div key={p.id} className="ioc-product-item" onClick={() => addToCart(p)}>
                                <div>
                                    <div className="ioc-prod-name">{p.name}</div>
                                    <div className="ioc-prod-sku">{p.sku || '---'} · Tồn: {p.stock}</div>
                                </div>
                                <div className="ioc-prod-price">{fmt(p.costPrice)}</div>
                            </div>
                        ))}
                    </div>
                </IonContent>
            </IonModal>

            {/* Supplier Picker Modal */}
            <SupplierPickerModal
                isOpen={showSupplierPicker}
                selected={selectedSupplier?.id || ''}
                onClose={() => setShowSupplierPicker(false)}
                onSelect={(supplier) => {
                    setSelectedSupplier(supplier);
                    setShowSupplierPicker(false);
                    // Add the new supplier to the local list if it's not there
                    setSuppliers(prev => {
                        if (!prev.find(s => s.id === supplier.id)) {
                            return [supplier, ...prev];
                        }
                        return prev;
                    });
                }}
            />

            <IonToast isOpen={toast !== null} message={toast ?? ''} duration={2000} onDidDismiss={() => setToast(null)} />
        </IonPage>
    );
};

export default ImportOrderCreatePage;
