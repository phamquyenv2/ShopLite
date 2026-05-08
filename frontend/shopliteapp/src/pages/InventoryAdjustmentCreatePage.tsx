import React, { useRef, useState } from 'react';
import {
    IonPage, IonHeader, IonToolbar, IonContent, IonIcon, IonButtons, IonButton,
    IonSpinner, IonToast, IonFooter, IonModal, useIonRouter, useIonViewWillEnter,
    IonItemSliding, IonItem, IonItemOptions, IonItemOption
} from '@ionic/react';
import {
    closeOutline, searchOutline, addOutline, informationCircleOutline,
    checkmarkCircleOutline, trashOutline, chevronBackOutline
} from 'ionicons/icons';
import { useAuth } from '../auth/useAuth';
import type { Category, Product, InventoryAdjustmentUpsert } from '../api/types';
import { productService } from '../services/product.service';
import { inventoryAdjustmentService } from '../services/inventoryAdjustment.service';
import { useStorePermissions } from '../utils/useStorePermissions';
import './InventoryAdjustmentCreatePage.css';

// ─── Types ─────────────────────────────────────────────────────────────────────

type CheckItem = {
    productId: number;
    name: string;
    sku: string;
    stock: number;
    actualQty: number | null;
    imageUrl?: string;
};

type FilterTab = 'all' | 'matched' | 'different' | 'unchecked';

// ─── Helpers ───────────────────────────────────────────────────────────────────

const fmt = (n: number) => n.toLocaleString('vi-VN');

const getStatus = (item: CheckItem): 'matched' | 'different' | 'unchecked' => {
    if (item.actualQty === null) return 'unchecked';
    return item.actualQty === item.stock ? 'matched' : 'different';
};

const getDelta = (item: CheckItem) =>
    item.actualQty === null ? 0 : item.actualQty - item.stock;

// ─── Component ─────────────────────────────────────────────────────────────────

const InventoryAdjustmentCreatePage: React.FC = () => {
    const ionRouter = useIonRouter();
    const { user } = useAuth();
    const { can } = useStorePermissions();
    const canCreateAdjustment = can('/api/v1/inventory-adjustments', 'POST');

    // Cart
    const [cart, setCart] = useState<CheckItem[]>([]);
    const [activeTab, setActiveTab] = useState<FilterTab>('all');
    const [checkoutMode, setCheckoutMode] = useState(false);

    // Product search modal
    const [showProductSearch, setShowProductSearch] = useState(false);
    const [searchText, setSearchText] = useState('');
    const [searchResults, setSearchResults] = useState<Product[]>([]);
    const [searching, setSearching] = useState(false);
    const debounceRef = useRef<ReturnType<typeof setTimeout> | null>(null);

    // Category modal
    const [showCategoryModal, setShowCategoryModal] = useState(false);
    const [categories, setCategories] = useState<Category[]>([]);
    const [loadingCats, setLoadingCats] = useState(false);
    const [addingCategory, setAddingCategory] = useState<number | null>(null);

    // Note
    const [note, setNote] = useState('');

    // Save
    const [saving, setSaving] = useState(false);
    const [toast, setToast] = useState<string | null>(null);

    // ── Data loading ────────────────────────────────────────────────────────────

    const loadCategories = async () => {
        if (categories.length > 0) return;
        setLoadingCats(true);
        try { setCategories(await productService.getCategories()); }
        catch { setCategories([]); }
        finally { setLoadingCats(false); }
    };

    useIonViewWillEnter(() => { void loadCategories(); });

    // ── Tab counts ──────────────────────────────────────────────────────────────

    const counts = {
        all: cart.length,
        matched: cart.filter(i => getStatus(i) === 'matched').length,
        different: cart.filter(i => getStatus(i) === 'different').length,
        unchecked: cart.filter(i => getStatus(i) === 'unchecked').length,
    };

    const visibleCart = activeTab === 'all'
        ? cart
        : cart.filter(i => getStatus(i) === activeTab);

    // ── Checkout calculations ───────────────────────────────────────────────────

    const checkedItems = cart.filter(i => i.actualQty !== null);
    const increaseItems = checkedItems.filter(i => getDelta(i) > 0);
    const decreaseItems = checkedItems.filter(i => getDelta(i) < 0);

    const totalActual = checkedItems.reduce((s, i) => s + (i.actualQty ?? 0), 0);
    const totalIncrease = increaseItems.reduce((s, i) => s + getDelta(i), 0);
    const totalDecrease = decreaseItems.reduce((s, i) => s + getDelta(i), 0);
    const totalDelta = checkedItems.reduce((s, i) => s + getDelta(i), 0);

    // ── Cart actions ────────────────────────────────────────────────────────────

    const addToCart = (p: Product) => {
        setCart(prev => {
            if (prev.find(i => i.productId === p.id)) return prev;
            return [...prev, {
                productId: p.id, name: p.name, sku: p.sku || '',
                stock: p.stock, actualQty: null, imageUrl: p.image || '',
            }];
        });
        setShowProductSearch(false);
        setSearchText('');
        setSearchResults([]);
    };

    const setQty = (productId: number, qty: number) =>
        setCart(prev => prev.map(i =>
            i.productId === productId ? { ...i, actualQty: Math.max(0, qty) } : i
        ));

    const clearQty = (productId: number) =>
        setCart(prev => prev.map(i =>
            i.productId === productId ? { ...i, actualQty: null } : i
        ));

    const markMatched = (productId: number) =>
        setCart(prev => prev.map(i =>
            i.productId === productId ? { ...i, actualQty: i.stock } : i
        ));

    const removeItem = (productId: number) =>
        setCart(prev => prev.filter(i => i.productId !== productId));

    // ── Search with debounce ────────────────────────────────────────────────────

    const handleSearchChange = (value: string) => {
        setSearchText(value);
        if (debounceRef.current) clearTimeout(debounceRef.current);
        if (!value.trim()) { setSearchResults([]); return; }
        setSearching(true);
        debounceRef.current = setTimeout(async () => {
            try {
                const data = await productService.getProducts({ keyword: value.trim(), size: 40 });
                setSearchResults(data);
            } catch { setSearchResults([]); }
            finally { setSearching(false); }
        }, 500);
    };

    // ── Category quick-select ───────────────────────────────────────────────────

    const addByCategory = async (categoryId: number) => {
        setAddingCategory(categoryId);
        try {
            const data = await productService.getProducts({ categoryId, size: 500 });
            setCart(prev => {
                const ids = new Set(prev.map(i => i.productId));
                const news = data.filter(p => !ids.has(p.id)).map(p => ({
                    productId: p.id, name: p.name, sku: p.sku || '',
                    stock: p.stock, actualQty: null, imageUrl: p.image || '',
                }));
                return [...prev, ...news];
            });
            setShowCategoryModal(false);
            setActiveTab('all');
        } catch { setToast('Không thể tải sản phẩm theo danh mục'); }
        finally { setAddingCategory(null); }
    };

    // ── Save ────────────────────────────────────────────────────────────────────

    const buildPayload = (reason: string): InventoryAdjustmentUpsert => ({
        reason: note.trim() || reason,
        createdBy: user?.username || 'shoplite',
        items: cart
            .filter(i => i.actualQty !== null)
            .map(i => ({ productId: i.productId, actualQuantity: i.actualQty as number })),
    });

    const handleSave = async (asDraft: boolean) => {
        if (!canCreateAdjustment) return setToast('Bạn không có quyền tạo phiếu kiểm kho');
        if (cart.length === 0) return setToast('Chưa có sản phẩm trong phiếu');
        if (!asDraft && checkedItems.length === 0) return setToast('Chưa nhập số lượng thực tế');
        setSaving(true);
        try {
            await inventoryAdjustmentService.create(
                buildPayload(asDraft ? 'Kiểm kho tạm' : 'Kiểm kho định kỳ')
            );
            setToast(asDraft ? 'Đã lưu tạm phiếu kiểm kho' : 'Tạo phiếu kiểm kho thành công');
            setTimeout(() => ionRouter.goBack(), 500);
        } catch (e: any) {
            setToast(e.message || 'Không thể tạo phiếu kiểm kho');
        } finally { setSaving(false); }
    };

    // ──────────────────────────────────────────────────────────────────────────
    // CHECKOUT MODE – review screen
    // ──────────────────────────────────────────────────────────────────────────

    if (checkoutMode) {
        return (
            <IonPage className="ioc-page checkout-page">
                <IonHeader className="ioc-header ion-no-border">
                    <IonToolbar className="ioc-toolbar">
                        <IonButtons slot="start">
                            <IonButton color="dark" onClick={() => setCheckoutMode(false)}>
                                <IonIcon icon={chevronBackOutline} style={{ fontSize: '26px' }} />
                            </IonButton>
                        </IonButtons>
                    </IonToolbar>
                </IonHeader>

                <IonContent className="ioc-content">
                    {/* Table */}
                    <div className="iac-review-card">
                        {/* Table header */}
                        <div className="iac-table-header">
                            <span className="iac-th-product">MẶT HÀNG</span>
                            <span className="iac-th-actual">THỰC TẾ</span>
                            <span className="iac-th-delta">LỆCH</span>
                        </div>

                        {/* Table rows */}
                        {checkedItems.map((item, idx) => {
                            const delta = getDelta(item);
                            return (
                                <div key={item.productId}
                                    className={`iac-table-row${idx === checkedItems.length - 1 ? ' last' : ''}`}>
                                    <div className="iac-tr-product">
                                        <div className="iac-tr-name">{item.name}</div>
                                        <div className="iac-tr-sku">{item.sku || '---'}</div>
                                        <div className="iac-tr-stock">Tồn: {fmt(item.stock)}</div>
                                    </div>
                                    <div className="iac-tr-actual">{fmt(item.actualQty ?? 0)}</div>
                                    <div className={`iac-tr-delta${delta < 0 ? ' negative' : delta > 0 ? ' positive' : ''}`}>
                                        {delta > 0 ? `+${fmt(delta)}` : fmt(delta)}
                                    </div>
                                </div>
                            );
                        })}

                        {checkedItems.length === 0 && (
                            <div className="ioc-empty-tab">Chưa có sản phẩm nào được kiểm</div>
                        )}
                    </div>

                    {/* Summary */}
                    <div className="iac-summary-card">
                        <div className="iac-summary-row">
                            <div>
                                <div className="iac-sum-label">Tổng thực tế</div>
                                <div className="iac-sum-sub">
                                    {checkedItems.length} mặt hàng&nbsp;•&nbsp;Số lượng: {fmt(totalActual)}
                                </div>
                            </div>
                            <div className="iac-sum-value">{fmt(totalActual)}</div>
                        </div>

                        <div className="iac-summary-row">
                            <div>
                                <div className="iac-sum-label">Tổng lệch tăng</div>
                                <div className="iac-sum-sub">
                                    {increaseItems.length} mặt hàng&nbsp;•&nbsp;Số lượng: {fmt(totalIncrease)}
                                </div>
                            </div>
                            <div className="iac-sum-value positive">{totalIncrease > 0 ? `+${fmt(totalIncrease)}` : 0}</div>
                        </div>

                        <div className="iac-summary-row">
                            <div>
                                <div className="iac-sum-label">Tổng lệch giảm</div>
                                <div className="iac-sum-sub">
                                    {decreaseItems.length} mặt hàng&nbsp;•&nbsp;Số lượng: {fmt(totalDecrease)}
                                </div>
                            </div>
                            <div className="iac-sum-value negative">{totalDecrease < 0 ? fmt(totalDecrease) : 0}</div>
                        </div>

                        <div className="iac-summary-row last">
                            <div>
                                <div className="iac-sum-label">Tổng chênh lệch</div>
                                <div className="iac-sum-sub">
                                    {checkedItems.filter(i => getDelta(i) !== 0).length} mặt hàng&nbsp;•&nbsp;Số lượng: {fmt(totalDelta)}
                                </div>
                            </div>
                            <div className={`iac-sum-value${totalDelta < 0 ? ' negative' : totalDelta > 0 ? ' positive' : ''}`}>
                                {totalDelta > 0 ? `+${fmt(totalDelta)}` : fmt(totalDelta)}
                            </div>
                        </div>
                    </div>

                    {/* Note card */}
                    <div className="iac-note-card">
                        <input
                            type="text"
                            className="iac-note-input"
                            placeholder="Thêm ghi chú..."
                            value={note}
                            onChange={e => setNote(e.target.value)}
                        />
                    </div>
                </IonContent>

                <IonFooter className="ioc-footer ion-no-border">
                    <div className="ioc-footer-actions">
                        <button className="ioc-btn-draft"
                            onClick={() => handleSave(true)} disabled={saving || !canCreateAdjustment}>
                            Lưu tạm
                        </button>
                        <button className="ioc-btn-save"
                            onClick={() => handleSave(false)} disabled={saving || !canCreateAdjustment}>
                            {saving ? <IonSpinner name="dots" /> : 'Hoàn thành'}
                        </button>
                    </div>
                </IonFooter>
            </IonPage>
        );
    }

    // ──────────────────────────────────────────────────────────────────────────
    // MAIN PAGE – create screen
    // ──────────────────────────────────────────────────────────────────────────

    return (
        <IonPage className="ioc-page">
            <IonHeader className="ioc-header ion-no-border">
                <div className="ioc-top-card">
                    <IonToolbar className="ioc-toolbar">
                        <IonButtons slot="start">
                            <IonButton color="dark" onClick={() => ionRouter.goBack()}>
                                <IonIcon icon={closeOutline} style={{ fontSize: '26px' }} />
                            </IonButton>
                        </IonButtons>
                        <div className="ioc-title">Phiếu kiểm kho mới</div>
                        <IonButtons slot="end">
                            <IonButton color="dark">
                                <IonIcon icon={informationCircleOutline} style={{ fontSize: '24px' }} />
                            </IonButton>
                        </IonButtons>
                    </IonToolbar>

                    {/* Search + Nhóm hàng */}
                    <div className="ioc-actions-row">
                        <div className="ioc-search-bar"
                            onClick={() => { setSearchText(''); setSearchResults([]); setShowProductSearch(true); }}>
                            <IonIcon icon={searchOutline} className="ioc-s-icon" />
                            <span className="ioc-search-placeholder">Tên, mã hàng, m...</span>
                            <div className="ioc-s-actions">
                                <IonIcon icon={addOutline} className="ioc-s-add-icon" />
                                <div className="ioc-s-scan">
                                    <svg width="20" height="20" viewBox="0 0 24 24" fill="none"
                                        stroke="currentColor" strokeWidth="2"
                                        strokeLinecap="round" strokeLinejoin="round">
                                        <path d="M4 7V4h3M20 7V4h-3M4 17v3h3M20 17v3h-3M9 8h2v8H9zM13 8h2v8h-2z" />
                                    </svg>
                                </div>
                            </div>
                        </div>
                        <div className="ioc-category-btn"
                            onClick={() => { void loadCategories(); setShowCategoryModal(true); }}>
                            <svg width="20" height="20" viewBox="0 0 24 24" fill="currentColor">
                                <path d="M3 13h2v-2H3v2zm0 4h2v-2H3v2zm0-8h2V7H3v2zm4 4h14v-2H7v2zm0 4h14v-2H7v2zM7 7v2h14V7H7z" />
                            </svg>
                            <span>Nhóm hàng</span>
                        </div>
                    </div>

                    {/* Filter tabs */}
                    {cart.length > 0 && (
                        <div className="ioc-tabs-row">
                            {([
                                ['all', `Tất cả ${counts.all}`],
                                ['matched', `Khớp ${counts.matched}`],
                                ['different', `Lệch ${counts.different}`],
                                ['unchecked', `Chưa kiểm ${counts.unchecked}`],
                            ] as [FilterTab, string][]).map(([tab, label]) => (
                                <button key={tab} type="button"
                                    className={`ioc-tab${activeTab === tab ? ' active' : ''}`}
                                    onClick={() => setActiveTab(tab)}>
                                    {label}
                                </button>
                            ))}
                        </div>
                    )}
                </div>
            </IonHeader>

            <IonContent className="ioc-content">
                {cart.length === 0 ? (
                    <div className="ioc-empty">
                        <div className="ioc-empty-circle">
                            <svg width="60" height="60" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1">
                                <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8l-6-6z"
                                    fill="#f8fafc" stroke="#3b82f6" strokeWidth="1.5" />
                                <path d="M14 2v6h6M8 10h5M8 14h8M8 18h4" stroke="#3b82f6"
                                    strokeWidth="1.5" strokeLinecap="round" />
                            </svg>
                        </div>
                        <p>Chưa có hàng trong phiếu kiểm kho</p>
                    </div>
                ) : (
                    <div className="ioc-cart">
                        {visibleCart.map((item, idx) => {
                            const status = getStatus(item);
                            const delta = getDelta(item);
                            const isMatched = status === 'matched';

                            return (
                                <IonItemSliding key={item.productId}
                                    className={`ioc-item-sliding${idx === visibleCart.length - 1 ? ' last-item' : ''}`}>
                                    <IonItem lines="none" className="ioc-item-inner">
                                        <div className="ioc-item-row">
                                            <div className="ioc-item-thumb">
                                                {item.imageUrl
                                                    ? <img src={item.imageUrl} alt={item.name} />
                                                    : <div className="ioc-thumb-ph" />
                                                }
                                            </div>
                                            <div className="ioc-item-content">
                                                <div className="ioc-item-name">{item.name}</div>
                                                <div className="ioc-item-sku">{item.sku || '---'}</div>
                                                <div className="ioc-item-stock-row">
                                                    <span className="ioc-item-stock">Tồn: {fmt(item.stock)}</span>
                                                    {status === 'different' && delta !== 0 && (
                                                        <span className="ioc-delta-badge">
                                                            Lệch: {delta > 0 ? `+${fmt(delta)}` : fmt(delta)}
                                                        </span>
                                                    )}
                                                </div>

                                                <div className="ioc-item-controls">
                                                    <div className="ioc-qty-capsule">
                                                        <div className="ioc-qty-btn"
                                                            onClick={e => { e.stopPropagation(); setQty(item.productId, Math.max(0, (item.actualQty ?? item.stock) - 1)); }}>
                                                            −
                                                        </div>
                                                        {item.actualQty === null ? (
                                                            <input
                                                                className="ioc-qty-input ioc-qty-placeholder"
                                                                type="number"
                                                                placeholder="Nhập SL"
                                                                onFocus={() => setQty(item.productId, item.stock)}
                                                            />
                                                        ) : (
                                                            <input
                                                                className="ioc-qty-input"
                                                                type="text"
                                                                inputMode="numeric"
                                                                value={fmt(item.actualQty)}
                                                                onChange={e => {
                                                                    const valStr = e.target.value.replace(/\D/g, '');
                                                                    if (!valStr) { clearQty(item.productId); return; }
                                                                    const v = parseInt(valStr, 10);
                                                                    if (!isNaN(v)) setQty(item.productId, v);
                                                                }}
                                                            />
                                                        )}
                                                        <div className="ioc-qty-btn"
                                                            onClick={e => { e.stopPropagation(); setQty(item.productId, (item.actualQty ?? item.stock) + 1); }}>
                                                            +
                                                        </div>
                                                    </div>

                                                    <button type="button"
                                                        className={`ioc-match-btn${isMatched ? ' matched' : ''}`}
                                                        onClick={e => { e.stopPropagation(); markMatched(item.productId); }}>
                                                        <IonIcon icon={checkmarkCircleOutline} style={{ fontSize: '18px' }} />
                                                        Khớp
                                                    </button>
                                                </div>
                                            </div>
                                        </div>
                                    </IonItem>
                                    <IonItemOptions side="end">
                                        <IonItemOption color="danger" onClick={() => removeItem(item.productId)}>
                                            <IonIcon slot="icon-only" icon={trashOutline} style={{ fontSize: '24px' }} />
                                        </IonItemOption>
                                    </IonItemOptions>
                                </IonItemSliding>
                            );
                        })}

                        {visibleCart.length === 0 && (
                            <div className="ioc-empty-tab">Không có sản phẩm nào</div>
                        )}
                    </div>
                )}
            </IonContent>

            <IonFooter className="ioc-footer ion-no-border">
                {cart.length > 0 && (
                    <div className="ioc-total-section">
                        <div className="ioc-total-row">
                            <span>Tổng kiểm</span>
                            <span className="ioc-total-val">{fmt(counts.all)} mặt hàng</span>
                        </div>
                        <div className="ioc-total-sub">
                            Khớp: {counts.matched} · Lệch: {counts.different} · Chưa kiểm: {counts.unchecked}
                        </div>
                    </div>
                )}
                <div className="ioc-footer-actions">
                    <button className="ioc-btn-draft"
                        onClick={() => handleSave(true)}
                        disabled={saving || cart.length === 0 || !canCreateAdjustment}>
                        Lưu tạm
                    </button>
                    <button className="ioc-btn-save"
                        onClick={() => setCheckoutMode(true)}
                        disabled={cart.length === 0 || !canCreateAdjustment}>
                        Tiếp tục
                    </button>
                </div>
            </IonFooter>

            {/* ── Product search modal ── */}
            <IonModal isOpen={showProductSearch}
                onDidDismiss={() => { setShowProductSearch(false); setSearchText(''); setSearchResults([]); }}
                className="ioc-modal">
                <IonHeader>
                    <IonToolbar>
                        <IonButtons slot="start">
                            <IonButton onClick={() => setShowProductSearch(false)}>
                                <IonIcon icon={closeOutline} />
                            </IonButton>
                        </IonButtons>
                        <div style={{ padding: '0 8px' }}>
                            <input className="ioc-modal-search" placeholder="Tìm sản phẩm..."
                                value={searchText} onChange={e => handleSearchChange(e.target.value)} autoFocus />
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
                        {!searching && !searchText.trim() && (
                            <div className="ioc-empty-search">Nhập tên hoặc mã hàng để tìm kiếm</div>
                        )}
                        {!searching && searchText.trim() && searchResults.length === 0 && (
                            <div className="ioc-empty-search">Không tìm thấy sản phẩm</div>
                        )}
                        {searchResults.map(p => {
                            const inCart = cart.some(i => i.productId === p.id);
                            return (
                                <div key={p.id} className={`ioc-product-item${inCart ? ' in-cart' : ''}`}
                                    onClick={() => !inCart && addToCart(p)}>
                                    <div>
                                        <div className="ioc-prod-name">{p.name}</div>
                                        <div className="ioc-prod-sku">{p.sku || '---'} · Tồn: {fmt(p.stock)}</div>
                                    </div>
                                    <div className="ioc-prod-price">
                                        {inCart
                                            ? <span className="ioc-incart-badge">Đã thêm</span>
                                            : <IonIcon icon={addOutline} style={{ fontSize: '20px' }} />
                                        }
                                    </div>
                                </div>
                            );
                        })}
                    </div>
                </IonContent>
            </IonModal>

            {/* ── Category modal ── */}
            <IonModal isOpen={showCategoryModal} onDidDismiss={() => setShowCategoryModal(false)}
                className="ioc-modal" initialBreakpoint={0.75} breakpoints={[0, 0.75, 1]}>
                <IonHeader>
                    <IonToolbar>
                        <IonButtons slot="start">
                            <IonButton onClick={() => setShowCategoryModal(false)}>
                                <IonIcon icon={closeOutline} />
                            </IonButton>
                        </IonButtons>
                    </IonToolbar>
                </IonHeader>
                <IonContent>
                    <div className="ioc-supplier-list">
                        <div className="ioc-supplier-list-title">Chọn nhóm hàng</div>
                        {loadingCats
                            ? <div style={{ display: 'flex', justifyContent: 'center', padding: '32px' }}>
                                <IonSpinner name="crescent" color="primary" />
                              </div>
                            : categories.map(cat => (
                                <div key={cat.id} className="ioc-supplier-item"
                                    onClick={() => addByCategory(cat.id)}
                                    style={{ opacity: addingCategory !== null ? 0.6 : 1 }}>
                                    <div className="ioc-supplier-name">{cat.name}</div>
                                    {addingCategory === cat.id && <IonSpinner name="dots" style={{ width: 20, height: 20 }} />}
                                </div>
                            ))
                        }
                        {!loadingCats && categories.length === 0 && (
                            <div className="ioc-empty-search">Không có danh mục nào</div>
                        )}
                    </div>
                </IonContent>
            </IonModal>

            <IonToast isOpen={toast !== null} message={toast ?? ''} duration={2000}
                onDidDismiss={() => setToast(null)} />
        </IonPage>
    );
};

export default InventoryAdjustmentCreatePage;
