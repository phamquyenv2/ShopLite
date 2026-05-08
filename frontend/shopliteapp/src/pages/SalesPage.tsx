import {
    IonActionSheet,
    IonAlert,
    IonButtons,
    IonContent,
    IonHeader,
    IonIcon,
    IonPage,
    IonRefresher,
    IonRefresherContent,
    IonSearchbar,
    IonSkeletonText,
    IonToast,
    IonToolbar,
    useIonRouter,
    useIonViewWillEnter,
} from '@ionic/react';
import {
    addOutline,
    arrowBackOutline,
    removeOutline,
    chevronForwardOutline,
    reorderThreeOutline,
    scanOutline,
    cubeOutline,
    notificationsOutline,
    cartOutline,
} from 'ionicons/icons';
import { useEffect, useMemo, useRef, useState } from 'react';
import { useHistory } from 'react-router-dom';
import type { Category, Product } from '../api/types';
import { CART_KEY } from '../constants/storage';
import { ApiError } from '../utils/Apis';
import { productService } from '../services/product.service';
import { useStorePermissions } from '../utils/useStorePermissions';
import './SalesPage.css';

type CartLine = {
    product: Product;
    quantity: number;
};

type CartMap = Record<number, CartLine>; // productId -> line
type SalesDraft = {
    customerId: number | null;
    items: CartLine[];
};

const toNumber = (v: unknown): number => {
    const n = typeof v === 'number' ? v : Number(String(v ?? ''));
    return Number.isFinite(n) ? n : 0;
};

const formatVnd = (amount: number): string => `${new Intl.NumberFormat('vi-VN').format(Math.max(0, Math.round(amount)))}đ`;

const normalize = (s: string): string => s.trim().toLowerCase();

const isProductActive = (p: Product): boolean => {
    if (p.deleted) return false;
    if (!p.status) return true;
    return p.status === 'ACTIVE' || p.status === 'OUT_OF_STOCK';
};

const pickPrice = (p: Product): number => {
    const sell = toNumber((p as unknown as { selling_price?: number; price?: number }).selling_price);
    if (sell > 0) return sell;
    const alt = toNumber((p as unknown as { sellingPrice?: number; price?: number }).sellingPrice);
    if (alt > 0) return alt;
    return toNumber((p as unknown as { price?: number }).price) || 0;
};

const SalesPage: React.FC = () => {
    const ionRouter = useIonRouter();
    const history = useHistory();
    const { can } = useStorePermissions();
    const canCreateOrder = can('/api/v1/orders', 'POST');

    const [loading, setLoading] = useState(false);
    const [error, setError] = useState<string | null>(null);
    const [toast, setToast] = useState<string | null>(null);

    const [products, setProducts] = useState<Product[]>([]);
    const [categories, setCategories] = useState<Category[]>([]);
    const [customerId, setCustomerId] = useState<number>(0);

    const [keyword, setKeyword] = useState('');
    const [debouncedKeyword, setDebouncedKeyword] = useState('');
    const [selectedCategoryId, setSelectedCategoryId] = useState<number>(0); // 0 = all

    const [cart, setCart] = useState<CartMap>({});

    const [menuOpen, setMenuOpen] = useState(false);
    const [scanOpen, setScanOpen] = useState(false);
    const [scanValue, setScanValue] = useState('');
    const [activeProductId, setActiveProductId] = useState<number | null>(null);
    const [submitting, setSubmitting] = useState(false);

    const chipsRef = useRef<HTMLDivElement | null>(null);


    const load = async () => {
        setLoading(true);
        setError(null);
        try {
            const [cats, prods] = await Promise.all([
                productService.getCategories().catch(() => []),
                productService.getProducts({ size: 200 }),
            ]);

            const safeProducts = (Array.isArray(prods) ? prods : []).filter(isProductActive);
            setProducts(safeProducts);

            const safeCats = Array.isArray(cats) ? cats : [];
            setCategories(safeCats);

            setCustomerId(0);
        } catch (err) {
            setProducts([]);
            setCategories([]);
            setError(err instanceof ApiError ? err.message : 'Không thể tải sản phẩm');
        } finally {
            setLoading(false);
        }
    };

    useIonViewWillEnter(() => {
        void load();
        setActiveProductId(null);
        try {
            const raw = sessionStorage.getItem(CART_KEY);
            if (!raw) {
                setCart({});
            } else {
                const draft = JSON.parse(raw) as SalesDraft;
                const newCart: CartMap = {};
                if (Array.isArray(draft.items)) {
                    draft.items.forEach(line => {
                        if (line.product && typeof line.product.id === 'number') {
                            newCart[line.product.id] = line;
                        }
                    });
                }
                setCart(newCart);
                if (draft.customerId) setCustomerId(draft.customerId);
            }
        } catch {
            setCart({});
        }
    });

    useEffect(() => {
        const t = globalThis.setTimeout(() => setDebouncedKeyword(keyword), 500);
        return () => globalThis.clearTimeout(t);
    }, [keyword]);

    const normalizedQuery = normalize(debouncedKeyword);

    const filteredProducts = useMemo(() => {
        const byCategory = selectedCategoryId > 0
            ? products.filter((p) => p.categoryId === selectedCategoryId)
            : products;

        if (!normalizedQuery) return byCategory;

        return byCategory.filter((p) => {
            const haystack = [p.name, p.sku ?? '', p.barcode ?? ''].join(' ').toLowerCase();
            return haystack.includes(normalizedQuery);
        });
    }, [products, selectedCategoryId, normalizedQuery]);

    const cartLines = useMemo(() => Object.values(cart), [cart]);

    const totalItems = useMemo(() => cartLines.reduce((acc, l) => acc + toNumber(l.quantity), 0), [cartLines]);

    const totalAmount = useMemo(
        () => cartLines.reduce((sum, l) => sum + toNumber(l.quantity) * pickPrice(l.product), 0),
        [cartLines],
    );

    const addToCart = (product: Product, qty = 1) => {
        if (toNumber(product.stock) <= 0 || product.status === 'INACTIVE') return;

        setCart((prev) => {
            const existing = prev[product.id];
            const nextQty = Math.min(toNumber(product.stock) || 0, (existing ? existing.quantity : 0) + qty);
            if (nextQty <= 0) return prev;
            return {
                ...prev,
                [product.id]: {
                    product,
                    quantity: nextQty,
                },
            };
        });

    };

    const setLineQty = (productId: number, quantity: number) => {
        setCart((prev) => {
            const line = prev[productId];
            if (!line) return prev;
            const max = Math.max(0, toNumber(line.product.stock));
            const nextQty = Math.max(0, Math.min(max, toNumber(quantity)));
            if (nextQty <= 0) {
                const { [productId]: removed, ...rest } = prev;
                void removed;
                return rest;
            }
            return { ...prev, [productId]: { ...line, quantity: nextQty } };
        });
    };

    const showContinue = totalItems > 0;

    const onContinue = async () => {
        if (submitting) return;
        if (!canCreateOrder) {
            setToast('Bạn không có quyền tạo đơn hàng');
            return;
        }
        if (cartLines.length === 0) {
            setToast('Chưa có sản phẩm');
            return;
        }

        setSubmitting(true);
        try {
            const draft: SalesDraft = {
                customerId: customerId > 0 ? customerId : null,
                items: cartLines.map((line) => ({
                    product: line.product,
                    quantity: toNumber(line.quantity) || 1,
                })),
            };
            sessionStorage.setItem(CART_KEY, JSON.stringify(draft));
            history.push('/orders/new', { salesDraft: draft });
        } catch (err) {
            setToast(err instanceof ApiError ? err.message : 'Không thể chuyển sang tạo đơn');
        } finally {
            setSubmitting(false);
        }
    };

    const onScanSubmit = async (barcode: string) => {
        const code = String(barcode ?? '').trim();
        setScanOpen(false);
        setScanValue('');

        if (!code) return;

        try {
            const p = await productService.searchByBarcode(code);
            if (!p) {
                setToast('Không tìm thấy sản phẩm');
                return;
            }
            if (!isProductActive(p)) {
                setToast('Không tìm thấy sản phẩm');
                return;
            }
            addToCart(p, 1);
        } catch (err) {
            setToast(err instanceof ApiError ? err.message : 'Không tìm thấy sản phẩm');
        }
    };

    const onBack = () => {
        if (ionRouter.canGoBack()) {
            ionRouter.goBack();
            return;
        }
        ionRouter.push('/home', 'back');
    };

    return (
        <IonPage>
            <IonHeader className="ion-no-border sales-header">
                <IonToolbar className="sales-toolbar">
                    <div className="sales-toolbar-left" slot="start">
                        <button className="sales-toolbar-icon" type="button" aria-label="Back" onClick={onBack}>
                            <IonIcon icon={arrowBackOutline} />
                        </button>
                        <div className="sales-toolbar-title">Bán hàng</div>
                    </div>

                    <IonButtons slot="end">
                        <IonButtons>
                            <button className="sales-toolbar-icon" type="button" aria-label="Menu" onClick={() => setMenuOpen(true)}>
                                <IonIcon icon={reorderThreeOutline} />
                            </button>
                            <button className="sales-toolbar-icon" type="button" aria-label="DraftOrders" onClick={() => history.push('/orders/draft')}>
                                <IonIcon icon={cartOutline} />
                            </button>
                            <button className="sales-toolbar-icon" type="button" aria-label="Scan" onClick={() => setScanOpen(true)}>
                                <IonIcon icon={scanOutline} />
                            </button>
                        </IonButtons>
                    </IonButtons>
                </IonToolbar>

                <div className="sales-header-body">
                    <IonSearchbar
                        className="sales-search"
                        value={keyword}
                        placeholder="Tìm sản phẩm hoặc quét mã..."
                        onIonInput={(e) => setKeyword(String(e.detail.value ?? ''))}
                        onKeyDown={(e) => {
                            if (e.key === 'Enter') setDebouncedKeyword(keyword);
                        }}
                        showClearButton="always"
                    />

                    <div className="sales-chips-wrap" role="tablist" aria-label="Danh mục">
                        <div className="sales-chips" ref={chipsRef}>
                            <button
                                type="button"
                                className={selectedCategoryId === 0 ? 'sales-chip is-active' : 'sales-chip'}
                                onClick={() => setSelectedCategoryId(0)}
                            >
                                Tất cả
                            </button>

                            {categories.map((c) => (
                                <button
                                    key={c.id}
                                    type="button"
                                    className={selectedCategoryId === c.id ? 'sales-chip is-active' : 'sales-chip'}
                                    onClick={() => setSelectedCategoryId(c.id)}
                                >
                                    {c.name}
                                </button>
                            ))}
                        </div>
                    </div>
                </div>
            </IonHeader>

            <IonContent className="sales-content">
                <IonRefresher slot="fixed" onIonRefresh={async (e) => { await load(); e.detail.complete(); }}>
                    <IonRefresherContent />
                </IonRefresher>

                {error && <div className="sales-error">{error}</div>}

                {!error && !loading && filteredProducts.length === 0 && <div className="sales-empty">Không có sản phẩm</div>}

                <div className="sales-grid">
                    {loading &&
                        Array.from({ length: 9 }).map((_, idx) => (
                            <div key={idx} className="product-card" aria-hidden="true">
                                <div className="product-thumb">
                                    <IonSkeletonText animated style={{ width: '40px', height: '40px', borderRadius: '12px' }} />
                                </div>
                                <div style={{ marginTop: 10 }}>
                                    <IonSkeletonText animated style={{ width: '90%', height: '10px' }} />
                                    <IonSkeletonText animated style={{ width: '60%', height: '10px' }} />
                                </div>
                                <div style={{ marginTop: 6 }}>
                                    <IonSkeletonText animated style={{ width: '50%', height: '12px' }} />
                                </div>
                            </div>
                        ))}

                    {!loading &&
                        filteredProducts.map((p) => {
                            const disabled = toNumber(p.stock) <= 0 || p.status === 'INACTIVE';
                            const line = cart[p.id];
                            const qty = line?.quantity ?? 0;
                            const showQty = activeProductId === p.id || qty > 0;
                            return (
                                <div
                                    key={p.id}
                                    role="button"
                                    tabIndex={disabled ? -1 : 0}
                                    aria-disabled={disabled}
                                    className={disabled ? 'product-card is-disabled' : 'product-card'}
                                    onClick={() => {
                                        if (!disabled) {
                                            addToCart(p, 1);
                                            setActiveProductId(p.id);
                                        }
                                    }}
                                    onKeyDown={(e) => {
                                        if (disabled) return;
                                        if (e.key === 'Enter' || e.key === ' ') {
                                            e.preventDefault();
                                            addToCart(p, 1);
                                            setActiveProductId(p.id);
                                        }
                                    }}
                                >
                                    {showQty && (
                                        <div className="product-qty-row" onClick={(e) => e.stopPropagation()}>
                                            <button
                                                type="button"
                                                className="qty-mini-btn"
                                                aria-label="Decrease"
                                                onClick={() => setLineQty(p.id, qty - 1)}
                                            >
                                                <IonIcon icon={removeOutline} />
                                            </button>
                                            <div className="qty-mini-value">{qty}</div>
                                            <button
                                                type="button"
                                                className="qty-mini-btn is-plus"
                                                aria-label="Increase"
                                                onClick={() => setLineQty(p.id, qty + 1)}
                                                disabled={qty >= toNumber(p.stock)}
                                            >
                                                <IonIcon icon={addOutline} />
                                            </button>
                                        </div>
                                    )}
                                    <div className="product-thumb" aria-hidden="true">
                                        {p.image ? (
                                            <img src={p.image} alt="" loading="lazy" />
                                        ) : (
                                            <IonIcon icon={cubeOutline} />
                                        )}
                                    </div>
                                    <div className="product-name">{p.name}</div>
                                    <div className="product-meta">
                                        <span className="product-sku">{p.sku ?? p.barcode ?? '---'}</span>
                                        <span className="product-stock">Tồn: {toNumber(p.stock)}</span>
                                    </div>
                                    <div className="product-price">{formatVnd(pickPrice(p))}</div>
                                </div>
                            );
                        })}
                </div>

                <div className="sales-bottom-spacer" />
            </IonContent>

            <IonActionSheet
                isOpen={menuOpen}
                onDidDismiss={() => setMenuOpen(false)}
                header="Tùy chọn"
                buttons={[
                    {
                        text: 'Làm mới sản phẩm',
                        handler: () => void load(),
                    },
                    {
                        text: 'Xóa tìm kiếm',
                        handler: () => {
                            setKeyword('');
                            setDebouncedKeyword('');
                        },
                    },
                    {
                        text: 'Hủy',
                        role: 'cancel',
                    },
                ]}
            />

            {showContinue && canCreateOrder && (
                <button
                    type="button"
                    className="sales-continue-btn"
                    onClick={() => void onContinue()}
                    disabled={submitting}
                >
                    <span className="continue-price">
                        <IonIcon icon={notificationsOutline} />
                        {formatVnd(totalAmount)}
                    </span>
                    <span className="continue-text">
                        Tiếp tục
                        <IonIcon icon={chevronForwardOutline} />
                    </span>
                </button>
            )}

            <IonAlert
                isOpen={scanOpen}
                header="Quét mã"
                inputs={[
                    {
                        name: 'barcode',
                        type: 'text',
                        placeholder: 'Nhập barcode',
                        value: scanValue,
                    },
                ]}
                buttons={[
                    {
                        text: 'Hủy',
                        role: 'cancel',
                        handler: () => {
                            setScanValue('');
                            setScanOpen(false);
                        },
                    },
                    {
                        text: 'Thêm',
                        handler: (data) => {
                            const code = String((data as { barcode?: string })?.barcode ?? '');
                            setScanValue(code);
                            void onScanSubmit(code);
                        },
                    },
                ]}
                onDidDismiss={() => {
                    setScanOpen(false);
                }}
            />


            <IonToast isOpen={toast !== null} message={toast ?? ''} duration={1800} onDidDismiss={() => setToast(null)} />
        </IonPage>
    );
};

export default SalesPage;

