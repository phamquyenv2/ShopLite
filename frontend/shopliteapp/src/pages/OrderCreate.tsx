import {
    IonActionSheet,
    IonButtons,
    IonContent,
    IonHeader,
    IonIcon,
    IonInput,
    IonPage,
    IonToast,
    IonToolbar,
    useIonViewWillEnter,
} from '@ionic/react';
import {
    addOutline,
    arrowBackOutline,
    cardOutline,
    chevronForwardOutline,
    pricetagOutline,
    readerOutline,
    personOutline,
    refreshOutline,
} from 'ionicons/icons';
import { useEffect, useMemo, useState, useRef } from 'react';
import { useHistory, useLocation } from 'react-router-dom';
import { useAuth } from '../auth/useAuth';
import type { Customer, OrderUpsert, Product } from '../api/types';
import { CART_KEY } from '../constants/storage';
import { ApiError, authApis, endpoints } from '../utils/Apis';
import { useStorePermissions } from '../utils/useStorePermissions';
import './OrderCreate.css';

const toNumber = (v: unknown): number => {
    const n = typeof v === 'number' ? v : Number(String(v ?? ''));
    return Number.isFinite(n) ? n : 0;
};

const formatVnd = (amount: number): string =>
    `${new Intl.NumberFormat('vi-VN').format(Math.max(0, Math.round(amount)))} đ`;

const isRecord = (v: unknown): v is Record<string, unknown> => typeof v === 'object' && v !== null;

const pickSellingPrice = (p: Product): number => toNumber(p.sellingPrice ?? 0);

type SaleCartLine = {
    product: Product;
    quantity: number;
};

type SalesDraft = {
    customerId: number | null;
    items: SaleCartLine[];
};

type OrderCreateLocationState = {
    salesDraft?: unknown;
    draftOrderId?: number;
};

type PaymentMethodUi = 'cash' | 'transfer' | 'sepay_qr' | 'card' | 'momo';
type ScreenMode = 'create' | 'payment' | 'qr_payment';

const normalizeItems = (value: unknown): SaleCartLine[] => {
    if (!Array.isArray(value)) return [];
    return value
        .map((it) => {
            if (!it || typeof it !== 'object') return null;
            const obj = it as { product?: unknown; quantity?: unknown };
            if (!obj.product || typeof obj.product !== 'object') return null;
            return {
                product: obj.product as Product,
                quantity: Math.max(1, toNumber(obj.quantity) || 1),
            };
        })
        .filter((it): it is SaleCartLine => it !== null);
};

const parseDraft = (value: unknown): SalesDraft => {
    if (Array.isArray(value)) {
        return { customerId: null, items: normalizeItems(value) };
    }
    if (value && typeof value === 'object') {
        const obj = value as { customerId?: unknown; items?: unknown };
        return {
            customerId: toNumber(obj.customerId) > 0 ? toNumber(obj.customerId) : null,
            items: normalizeItems(obj.items),
        };
    }
    return { customerId: null, items: [] };
};

const readDraft = (): SalesDraft => {
    try {
        const raw = sessionStorage.getItem(CART_KEY);
        if (!raw) return { customerId: null, items: [] };
        return parseDraft(JSON.parse(raw));
    } catch {
        return { customerId: null, items: [] };
    }
};

const pickData = <T,>(payload: unknown): T | null => {
    if (isRecord(payload) && 'data' in payload) {
        return (payload.data as T) ?? null;
    }
    return (payload as T) ?? null;
};

const OrderCreatePage: React.FC = () => {
    const history = useHistory();
    const location = useLocation<OrderCreateLocationState>();
    const { user } = useAuth();
    const { can } = useStorePermissions();
    const canCreateOrder = can('/api/v1/orders', 'POST');
    const canUpdateOrder = can('/api/v1/orders/{id}', 'PUT');
    const canConfirmOrder = can('/api/v1/orders/{id}/confirm', 'PATCH');
    const canPayOrder = can('/api/v1/orders/{id}/payments', 'POST');

    const [busy, setBusy] = useState(false);
    const [toast, setToast] = useState<string | null>(null);

    const [customers, setCustomers] = useState<Customer[]>([]);
    const [customerId, setCustomerId] = useState<number>(0);
    const [customerName, setCustomerName] = useState<string>('Khách lẻ');
    const [customerSheetOpen, setCustomerSheetOpen] = useState(false);

    const [items, setItems] = useState<SaleCartLine[]>([]);
    const [discountPercent, setDiscountPercent] = useState<number>(0);
    const [paymentMethod, setPaymentMethod] = useState<PaymentMethodUi>('cash');
    const [customerPaid, setCustomerPaid] = useState<number>(0);
    const [note, setNote] = useState('');

    const [screenMode, setScreenMode] = useState<ScreenMode>('create');
    const [draftOrderId, setDraftOrderId] = useState<number | null>(() => {
        const id = location.state?.draftOrderId;
        return typeof id === 'number' ? id : null;
    });
    const requestIdRef = useRef<string>(
        typeof crypto !== 'undefined' && 'randomUUID' in crypto
            ? crypto.randomUUID()
            : `order-${Date.now()}-${Math.random().toString(36).slice(2)}`,
    );
    const [isConfirmed, setIsConfirmed] = useState(false);
    // Trạng thái thực tế từ server — dùng để phân nhánh retry flow
    const [orderServerStatus, setOrderServerStatus] = useState<string | null>(null);
    const [qrData, setQrData] = useState<{ qrUrl?: string; transferContent?: string; amount?: number } | null>(null);
    const canSaveDraftOrder = draftOrderId ? canUpdateOrder : canCreateOrder;
    const canCompletePayment = orderServerStatus === 'PENDING_PAYMENT'
        ? canPayOrder
        : canUpdateOrder && canConfirmOrder && canPayOrder;

    useEffect(() => {
        let interval: ReturnType<typeof setInterval>;
        if (screenMode === 'qr_payment' && draftOrderId) {
            interval = setInterval(async () => {
                try {
                    const res = await authApis().get<any>(endpoints['payment-status'](draftOrderId));
                    const stat = pickData<{ status?: string }>(res.data);
                    if (stat?.status === 'PAID') {
                        clearInterval(interval);
                        sessionStorage.removeItem(CART_KEY);
                        setToast('Thanh toán thành công');
                        history.replace('/sales');
                    }
                } catch {
                    // ignore polling errors
                }
            }, 3000);
        }
        return () => {
            if (interval) clearInterval(interval);
        };
    }, [screenMode, draftOrderId, history]);

    const hasItems = items.length > 0;
    const itemCount = useMemo(() => items.reduce((sum, it) => sum + toNumber(it.quantity), 0), [items]);
    const subtotal = useMemo(
        () => items.reduce((sum, it) => sum + toNumber(it.quantity) * pickSellingPrice(it.product), 0),
        [items],
    );
    const safeDiscountPercent = Math.min(100, Math.max(0, toNumber(discountPercent)));
    const discountAmount = Math.round((subtotal * safeDiscountPercent) / 100);
    const total = Math.max(0, subtotal - discountAmount);
    const changeAmount = customerPaid - total;

    const persistDraft = (nextItems: SaleCartLine[], nextCustomerId: number) => {
        const draft: SalesDraft = {
            customerId: nextCustomerId > 0 ? nextCustomerId : null,
            items: nextItems,
        };
        if (draft.items.length === 0) {
            sessionStorage.removeItem(CART_KEY);
            return;
        }
        sessionStorage.setItem(CART_KEY, JSON.stringify(draft));
    };

    const loadLookups = async (preferredCustomerId?: number) => {
        try {
            const custRes = await authApis().get<unknown>(endpoints.customers);
            const list = pickData<unknown>(custRes.data);
            const custs = Array.isArray(list) ? (list as Customer[]) : [];
            setCustomers(custs);
            const preferred = toNumber(preferredCustomerId);
            const selected = preferred > 0 ? custs.find((c) => c.id === preferred) ?? null : null;
            setCustomerId(selected?.id ?? 0);
            setCustomerName(selected?.name ?? 'Khách lẻ');
        } catch (err) {
            setToast(err instanceof ApiError ? err.message : 'Không thể tải khách hàng');
        }
    };

    useIonViewWillEnter(() => {
        const stateDraft = parseDraft(location.state?.salesDraft);
        const savedDraft = readDraft();
        const draft = stateDraft.items.length > 0 ? stateDraft : savedDraft;

        if (stateDraft.items.length > 0) {
            sessionStorage.setItem(CART_KEY, JSON.stringify(stateDraft));
        }

        setItems(draft.items);
        setCustomerId(draft.customerId ?? 0);
        if (draft.items.length === 0) {
            setToast('Chưa thấy dữ liệu đơn tạm, hãy chọn món ở màn Bán hàng');
        }
        void loadLookups(draft.customerId ?? undefined);

        // Nếu được truyền draftOrderId từ màn DraftOrders (retry),
        // fetch trạng thái thực tế để phân nhánh đúng.
        const locOrderId = location.state?.draftOrderId;
        if (typeof locOrderId === 'number' && locOrderId > 0) {
            authApis().get<any>(endpoints['order-detail'](locOrderId))
                .then((res) => {
                    const order = pickData<{ status?: string }>(res.data);
                    const serverStatus = order?.status ?? null;
                    setOrderServerStatus(serverStatus);
                    // Nếu đƣn đã confirm (không còn là DRAFT) thì đánh dấu isConfirmed
                    if (serverStatus && serverStatus !== 'DRAFT') {
                        setIsConfirmed(true);
                    }
                })
                .catch(() => { /* ignore, flow vẫn hoạt động bình thường */ });
        }
    });

    const isFirstRender = useRef(true);

    useEffect(() => {
        if (isFirstRender.current) {
            isFirstRender.current = false;
            return;
        }
        persistDraft(items, customerId);
    }, [items, customerId]);

    const incQty = (id: number) => {
        setItems((prev) => prev.map((it) => (it.product.id === id ? { ...it, quantity: it.quantity + 1 } : it)));
    };

    const decQty = (id: number) => {
        setItems((prev) => prev.map((it) => (it.product.id === id ? { ...it, quantity: Math.max(1, it.quantity - 1) } : it)));
    };

    const buildOrderPayload = (): OrderUpsert => ({
        userId: toNumber(user?.id),
        requestId: requestIdRef.current,
        customerId: customerId > 0 ? customerId : null,
        discount: discountAmount,
        items: items.map((it) => ({
            productId: toNumber(it.product.id),
            quantity: toNumber(it.quantity) || 1,
            price: pickSellingPrice(it.product),
        })),
    });

    const createDraftOrder = async (): Promise<number> => {
        if (!canCreateOrder) {
            throw new ApiError('Bạn không có quyền tạo đơn hàng', { status: 403, data: null, headers: new Headers() });
        }
        if (!user?.id) {
            throw new ApiError('Missing userId', { status: 400, data: null, headers: new Headers() });
        }
        if (!hasItems) {
            throw new ApiError('Add at least one item', { status: 400, data: null, headers: new Headers() });
        }
        const payload = buildOrderPayload();
        const res = await authApis().post<unknown>(endpoints.orders, payload);
        const order = pickData<{ id?: unknown }>(res.data);
        const createdId = toNumber(order?.id);
        if (createdId <= 0) {
            throw new ApiError('Không lấy được mã đơn hàng vừa tạo', {
                status: 500,
                data: res.data,
                headers: res.headers,
            });
        }
        setDraftOrderId(createdId);
        return createdId;
    };

    const updateDraftOrder = async (orderId: number): Promise<void> => {
        if (!canUpdateOrder) {
            throw new ApiError('Bạn không có quyền cập nhật đơn hàng', { status: 403, data: null, headers: new Headers() });
        }
        const payload = buildOrderPayload();
        await authApis().put<any>(endpoints['order-detail'](orderId), payload);
    };

    const ensureDraftOrder = async (): Promise<number> => {
        if (draftOrderId && draftOrderId > 0) {
            await updateDraftOrder(draftOrderId);
            return draftOrderId;
        }
        return createDraftOrder();
    };

    const onSaveDraft = async () => {
        if (!canSaveDraftOrder) {
            setToast('Bạn không có quyền lưu đơn hàng');
            return;
        }
        if (isConfirmed) {
            setToast('Đơn hàng đã xác nhận, không thể lưu tạm');
            return;
        }
        setBusy(true);
        try {
            await ensureDraftOrder();
            sessionStorage.removeItem(CART_KEY);
            setToast('Đã lưu tạm đơn hàng');
            history.replace('/sales');
        } catch (err) {
            setToast(err instanceof ApiError ? err.message : 'Không thể lưu tạm đơn hàng');
        } finally {
            setBusy(false);
        }
    };

    const onPlaceOrder = async () => {
        if (!canSaveDraftOrder) {
            setToast('Bạn không có quyền lưu đơn hàng');
            return;
        }
        setBusy(true);
        try {
            await ensureDraftOrder();
            setScreenMode('payment');
        } catch (err) {
            setToast(err instanceof ApiError ? err.message : 'Không thể chuyển sang thanh toán');
        } finally {
            setBusy(false);
        }
    };

    const onCompletePayment = async () => {
        if (!canCompletePayment) {
            setToast('Bạn không có quyền chốt hoặc thanh toán đơn hàng');
            return;
        }
        if (!draftOrderId) {
            setToast('Đơn hàng chưa được tạo');
            return;
        }
        if (total <= 0) {
            setToast('Tổng thanh toán không hợp lệ');
            return;
        }

        setBusy(true);
        try {
            const isPendingPayment = orderServerStatus === 'PENDING_PAYMENT';

            if (!isPendingPayment) {
                // Lần đầu hoặc đơn vẫn ở DRAFT:
                // update → confirm → payment
                await updateDraftOrder(draftOrderId);
                if (!isConfirmed) {
                    await authApis().patch<any>(endpoints['order-confirm'](draftOrderId));
                    setIsConfirmed(true);
                    setOrderServerStatus('PENDING_PAYMENT');
                }
            }
            // Nếu isPendingPayment === true: bỏ qua update + confirm,
            // thực hiện payment ngay lập tức.

            // Map paymentMethod UI → backend PaymentMethodEnum
            const backendMethod = paymentMethod === 'cash'     ? 'CASH'
                                : paymentMethod === 'transfer' ? 'BANK_TRANSFER'
                                : paymentMethod === 'sepay_qr' ? 'BANK_QR'
                                : paymentMethod === 'momo'     ? 'EWALLET'
                                : 'CASH';

            const paymentPayload = {
                referenceType: 'ORDER',
                referenceId: draftOrderId,
                paymentMethod: backendMethod,
                amount: total,
            };

            if (paymentMethod === 'sepay_qr' || paymentMethod === 'momo') {
                const res = await authApis().post<any>(endpoints['order-payments'](draftOrderId), paymentPayload);
                const paymentData = pickData<{ qrUrl?: string; transferContent?: string; amount?: number }>(res.data);

                if (paymentData?.qrUrl) {
                    setQrData(paymentData);
                    setScreenMode('qr_payment');
                    return;
                } else {
                    setToast('Không thể tạo QR thanh toán');
                    return;
                }
            } else {
                await authApis().post<any>(endpoints['order-payments'](draftOrderId), paymentPayload);
                sessionStorage.removeItem(CART_KEY);
                setToast('Hoàn thành đơn hàng');
                history.replace('/sales');
            }
        } catch (err) {
            setToast(err instanceof ApiError ? err.message : 'Không thể hoàn thành thanh toán');
        } finally {
            setBusy(false);
        }
    };

    const customerButtons: Array<{ text: string; role?: 'cancel'; handler?: () => void }> = [
        {
            text: 'Khách lẻ (không chọn)',
            handler: () => {
                setCustomerId(0);
                setCustomerName('Khách lẻ');
            },
        },
        ...customers.map((c) => ({
            text: `${c.name} (${c.phone})`,
            handler: () => {
                setCustomerId(c.id);
                setCustomerName(c.name);
            },
        })),
        { text: 'Huy', role: 'cancel' },
    ];

    return (
        <IonPage>
            <IonHeader className="order-create-header">
                <IonToolbar className="order-create-toolbar">
                    <IonButtons slot="start">
                        <button
                            className="order-create-back"
                            type="button"
                            aria-label="Back"
                            onClick={() => {
                                if (screenMode === 'payment') {
                                    setScreenMode('create');
                                    return;
                                }
                                history.goBack();
                            }}
                        >
                            <IonIcon icon={arrowBackOutline} />
                        </button>
                    </IonButtons>
                    <div className="order-create-title">{screenMode === 'create' ? 'Tạo đơn hàng' : 'Thanh toán'}</div>
                    <IonButtons slot="end">
                        <button className="order-create-refresh" type="button" aria-label="Refresh" onClick={() => void loadLookups(customerId || undefined)}>
                            <IonIcon icon={refreshOutline} />
                        </button>
                    </IonButtons>
                </IonToolbar>
            </IonHeader>

            {screenMode === 'create' && (
                <>
                    <IonContent className="order-create-content">
                        <div className="order-create-shell">
                            <div className="order-section">
                                <div className="order-customer-card">
                                    <div className="order-customer-avatar">
                                        <IonIcon icon={personOutline} />
                                    </div>
                                    <div className="order-customer-meta">
                                        <div className="order-customer-label">Khách hàng</div>
                                        <div className="order-customer-name">{customerName || 'Khách lẻ'}</div>
                                    </div>
                                    <button className="order-customer-action" type="button" onClick={() => setCustomerSheetOpen(true)}>
                                        Thay đổi
                                    </button>
                                </div>
                            </div>

                            <div className="order-section">
                                <div className="order-section-header">
                                    <div className="order-section-title">Sản phẩm đã chọn</div>
                                    <button className="order-add-item" type="button" onClick={() => history.replace('/sales')}>
                                        + Thêm món
                                    </button>
                                </div>

                                {!hasItems && (
                                    <div className="order-empty">
                                        Chưa có sản phẩm. Hãy chọn ở màn hình Bán hàng.
                                    </div>
                                )}

                                {items.map((it) => (
                                    <div key={it.product.id} className="order-item-card">
                                        <div className="order-item-thumb">
                                            {it.product.image ? (
                                                <img src={it.product.image} alt="" loading="lazy" />
                                            ) : (
                                                <IonIcon icon={cardOutline} />
                                            )}
                                        </div>
                                        <div className="order-item-info">
                                            <div className="order-item-name">{it.product.name}</div>
                                            <div className="order-item-price-muted">{formatVnd(it.product.costPrice ?? 0)}</div>
                                            <div className="order-item-price">{formatVnd(pickSellingPrice(it.product))}</div>
                                        </div>
                                        <div className="order-item-qty">
                                            <div className="order-qty-pill">
                                                <button type="button" className="order-qty-btn" onClick={() => decQty(it.product.id)}>
                                                    -
                                                </button>
                                                <div className="order-qty-value">{it.quantity}</div>
                                                <button type="button" className="order-qty-plus" onClick={() => incQty(it.product.id)}>
                                                    <IonIcon icon={addOutline} />
                                                </button>
                                            </div>
                                        </div>
                                    </div>
                                ))}
                            </div>

                            <div className="order-section summary-card">
                                <div className="summary-row">
                                    <span>Tổng tiền hàng</span>
                                    <span>{formatVnd(subtotal)}</span>
                                </div>
                                <div className="summary-row muted discount-row">
                                    <span>
                                        <IonIcon icon={pricetagOutline} />
                                        Giảm giá ({safeDiscountPercent}%)
                                    </span>
                                    <span>-{formatVnd(discountAmount)}</span>
                                </div>
                                <div className="summary-row muted shipping-row">
                                    <span>Phí vận chuyển</span>
                                    <span>0</span>
                                </div>
                                <div className="summary-row total">
                                    <span>Tổng thanh toán</span>
                                    <span>{formatVnd(total)}</span>
                                </div>
                            </div>

                            <div className="order-section note-card">
                                <div className="order-section-title note-title">
                                    <IonIcon icon={readerOutline} />
                                    Ghi chú đơn hàng
                                </div>
                                <IonInput
                                    className="order-note"
                                    placeholder="Nhập ghi chú cho đơn hàng này..."
                                    value={note}
                                    onIonInput={(e) => setNote(String(e.detail.value ?? ''))}
                                />
                            </div>
                        </div>

                        <div className="order-bottom-spacer" />
                    </IonContent>

                    <div className="order-action-bar">
                        <div className="order-action-summary">
                            <span>Tổng tiền hàng</span>
                            <span className="order-action-count">{itemCount}</span>
                            <strong>{formatVnd(total)}</strong>
                        </div>
                        <div className="order-action-buttons">
                            <button className="order-btn-secondary" type="button" disabled={busy || !hasItems || isConfirmed || !canSaveDraftOrder} onClick={() => void onSaveDraft()}>
                                Lưu tạm
                            </button>
                            <button className="order-btn-primary" type="button" disabled={busy || !hasItems || !canSaveDraftOrder} onClick={() => void onPlaceOrder()}>
                                Đặt hàng
                            </button>
                        </div>
                    </div>
                </>
            )}

            {screenMode === 'payment' && (
                <>
                    <IonContent className="order-create-content">
                        <div className="payment-card payment-view-items" role="button" onClick={() => setScreenMode('create')}>
                            <span>Xem hàng trong đơn</span>
                            <IonIcon icon={chevronForwardOutline} />
                        </div>

                        <div className="payment-card">
                            <div className="summary-row">
                                <span>Tổng tiền hàng ({itemCount})</span>
                                <span>{formatVnd(subtotal)}</span>
                            </div>
                            <div className="payment-input-row">
                                <span>Giảm giá (%)</span>
                                <IonInput
                                    className="payment-input"
                                    inputmode="decimal"
                                    value={safeDiscountPercent}
                                    onIonInput={(e) => setDiscountPercent(toNumber(e.detail.value))}
                                />
                            </div>
                            <div className="summary-row total payment-row-blue">
                                <span>Khách cần trả</span>
                                <span>{formatVnd(total)}</span>
                            </div>

                            <div className="payment-input-row">
                                <span>Khách thanh toán</span>
                                <IonInput
                                    className="payment-input"
                                    inputmode="decimal"
                                    value={customerPaid}
                                    onIonInput={(e) => setCustomerPaid(toNumber(e.detail.value))}
                                />
                            </div>

                            <div className="payment-tab-row">
                                <button
                                    type="button"
                                    className={paymentMethod === 'cash' ? 'payment-tab is-active' : 'payment-tab'}
                                    onClick={() => setPaymentMethod('cash')}
                                >
                                    Tiền mặt
                                </button>
                                <button
                                    type="button"
                                    className={paymentMethod === 'transfer' ? 'payment-tab is-active' : 'payment-tab'}
                                    onClick={() => setPaymentMethod('transfer')}
                                >
                                    Chuyển khoản
                                </button>
                                <button
                                    type="button"
                                    className={paymentMethod === 'sepay_qr' ? 'payment-tab is-active' : 'payment-tab'}
                                    onClick={() => setPaymentMethod('sepay_qr')}
                                >
                                    SePay QR
                                </button>   
                                <button
                                    type="button"
                                    className={paymentMethod === 'momo' ? 'payment-tab is-active' : 'payment-tab'}
                                    onClick={() => setPaymentMethod('momo')}
                                >
                                    MoMo
                                </button>
                            </div>

                            <div className="payment-change-box">
                                <span>Tiền thừa trả khách</span>
                                <strong>{formatVnd(changeAmount)}</strong>
                            </div>
                        </div>

                        <div className="order-bottom-spacer" />
                    </IonContent>

                    <div className="order-action-bar payment-footer">
                        <button className="order-btn-primary payment-complete" type="button" disabled={busy || !draftOrderId || !canCompletePayment} onClick={() => void onCompletePayment()}>
                            {paymentMethod === 'sepay_qr' || paymentMethod === 'momo' ? 'Tạo QR thanh toán' : 'Hoàn thành'}
                        </button>
                    </div>
                </>
            )}

            {screenMode === 'qr_payment' && qrData && (
                <IonContent className="order-create-content" style={{ textAlign: 'center', padding: '20px' }}>
                    <div className="payment-card">
                        <h3>Quét mã QR để thanh toán</h3>
                        <p>Số tiền: <strong>{formatVnd(qrData.amount ?? 0)}</strong></p>
                        <p>Nội dung CK: <strong>{qrData.transferContent}</strong></p>
                        
                        <div style={{ margin: '20px auto', display: 'flex', justifyContent: 'center' }}>
                            <img src={qrData.qrUrl} alt="QR Code" style={{ maxWidth: '300px', width: '100%', borderRadius: '8px' }} />
                        </div>
                        
                        <p style={{ color: '#666', fontSize: '14px' }}>Hệ thống đang chờ thanh toán. Đơn hàng sẽ tự động hoàn thành khi nhận được tiền.</p>
                        
                        <button 
                            className="order-btn-secondary" 
                            style={{ marginTop: '20px', width: '100%' }}
                            onClick={() => {
                                navigator.clipboard.writeText(qrData.transferContent || '');
                                setToast('Đã copy nội dung CK');
                            }}
                        >
                            Copy nội dung CK
                        </button>
                        <button 
                            className="order-btn-primary payment-complete" 
                            style={{ marginTop: '10px' }}
                            onClick={() => {
                                sessionStorage.removeItem(CART_KEY);
                                history.replace('/sales');
                            }}
                        >
                            Bỏ qua chờ
                        </button>
                    </div>
                </IonContent>
            )}

            <IonActionSheet
                isOpen={customerSheetOpen}
                onDidDismiss={() => setCustomerSheetOpen(false)}
                header="Chọn khách hàng"
                buttons={customerButtons}
            />

            <IonToast isOpen={toast !== null} message={toast ?? ''} duration={2200} onDidDismiss={() => setToast(null)} />
        </IonPage>
    );
};

export default OrderCreatePage;
