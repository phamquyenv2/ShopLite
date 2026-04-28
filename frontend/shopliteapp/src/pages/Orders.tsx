import React, { useMemo, useState } from 'react';
import {
    IonPage, IonHeader, IonToolbar, IonContent, IonIcon, IonButtons, IonButton,
    IonSpinner, IonFab, IonFabButton, IonModal, useIonRouter, useIonViewWillEnter, IonToast
} from '@ionic/react';
import {
    searchOutline, addOutline, chevronDownOutline, chevronBackOutline, chevronForwardOutline,
    checkmarkOutline, closeOutline
} from 'ionicons/icons';
import { authApis, endpoints } from '../utils/Apis';
import type { Order } from '../api/types';
import './Orders.css';

// ─── Helpers ───────────────────────────────────────────────────────────────────

const fmt = (n?: number | null) => (n ?? 0).toLocaleString('vi-VN');

const DAY_NAMES = ['CHỦ NHẬT', 'THỨ HAI', 'THỨ BA', 'THỨ TƯ', 'THỨ NĂM', 'THỨ SÁU', 'THỨ BẢY'];

const fmtDate = (d: Date) =>
    `${String(d.getDate()).padStart(2, '0')}/${String(d.getMonth() + 1).padStart(2, '0')}/${d.getFullYear()}`;
const fmtTime = (d: Date) =>
    `${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`;
const fmtDayLabel = (d: Date) => `${DAY_NAMES[d.getDay()]}, ${fmtDate(d)}`;

const PAYMENT_LABEL: Record<string, string> = {
    CASH: 'Tiền mặt',
    BANK: 'Chuyển khoản',
    MOMO: 'MoMo',
    SEPAY_QR: 'SePay QR',
    COD: 'COD',
    VNPAY: 'VNPay',
};

const STATUS_VI: Record<string, string> = {
    DRAFT: 'Nháp',
    PENDING: 'Đang xử lý',
    PENDING_PAYMENT: 'Chờ thanh toán',
    COMPLETED: 'Hoàn thành',
    FAIL: 'Thất bại',
    CANCELLED: 'Đã huỷ',
};

// ─── Filter Types ───────────────────────────────────────────────────────────────

type OrderStatus = 'DRAFT' | 'PENDING_PAYMENT' | 'COMPLETED' | 'CANCELLED';
type PeriodFilter = 'all' | 'today' | 'yesterday' | '7days' | '30days';

interface FilterState {
    statuses: OrderStatus[];
    period: PeriodFilter;
}

const DEFAULT_FILTER: FilterState = {
    statuses: ['PENDING_PAYMENT', 'COMPLETED'],
    period: 'all',
};

const PERIOD_LABELS: { value: PeriodFilter; label: string }[] = [
    { value: 'all', label: 'Toàn thời gian' },
    { value: 'today', label: 'Hôm nay' },
    { value: 'yesterday', label: 'Hôm qua' },
    { value: '7days', label: '7 ngày qua' },
    { value: '30days', label: '30 ngày qua' },
];

const STATUS_FILTER_OPTS: { value: OrderStatus; label: string }[] = [
    { value: 'PENDING_PAYMENT', label: 'Đang xử lý' },
    { value: 'COMPLETED', label: 'Hoàn thành' },
    { value: 'DRAFT', label: 'Nháp' },
    { value: 'CANCELLED', label: 'Đã huỷ' },
];

// ── Build date range from PeriodFilter ─────────────────────────────────────────
function buildDateRange(period: PeriodFilter): { from?: string; to?: string } {
    if (period === 'all') return {};
    const now = new Date();
    const startOfDay = (d: Date) => new Date(d.getFullYear(), d.getMonth(), d.getDate());
    const today = startOfDay(now);
    const toISO = (d: Date) => d.toISOString();

    if (period === 'today') {
        return { from: toISO(today), to: toISO(now) };
    }
    if (period === 'yesterday') {
        const yest = new Date(today); yest.setDate(yest.getDate() - 1);
        return { from: toISO(yest), to: toISO(today) };
    }
    if (period === '7days') {
        const s = new Date(today); s.setDate(s.getDate() - 7);
        return { from: toISO(s), to: toISO(now) };
    }
    if (period === '30days') {
        const s = new Date(today); s.setDate(s.getDate() - 30);
        return { from: toISO(s), to: toISO(now) };
    }
    return {};
}

function filterOrders(orders: Order[], f: FilterState): Order[] {
    let result = orders;
    if (f.statuses.length > 0) {
        result = result.filter(o => f.statuses.includes((o.status as OrderStatus)));
    }
    // Client-side time fallback (when API doesn't support query params)
    if (f.period !== 'all') {
        const { from, to } = buildDateRange(f.period);
        const fromDate = from ? new Date(from) : null;
        const toDate = to ? new Date(to) : null;
        result = result.filter(o => {
            if (!o.createdAt) return false;
            const d = new Date(o.createdAt);
            if (fromDate && d < fromDate) return false;
            if (toDate && d > toDate) return false;
            return true;
        });
    }
    return result;
}

// ─── Filter Modal ───────────────────────────────────────────────────────────────

interface FilterModalProps {
    isOpen: boolean;
    initial: FilterState;
    onApply: (f: FilterState) => void;
    onClose: () => void;
}

const FilterModal: React.FC<FilterModalProps> = ({ isOpen, initial, onApply, onClose }) => {
    const [local, setLocal] = useState<FilterState>(initial);
    const [statusOpen, setStatusOpen] = useState(true);
    const [periodOpen, setPeriodOpen] = useState(true);

    React.useEffect(() => { if (isOpen) setLocal(initial); }, [isOpen, initial]);

    const toggleStatus = (s: OrderStatus) => {
        setLocal(prev => ({
            ...prev,
            statuses: prev.statuses.includes(s)
                ? prev.statuses.filter(x => x !== s)
                : [...prev.statuses, s],
        }));
    };

    return (
        <IonModal isOpen={isOpen} onDidDismiss={onClose} className="ord-filter-modal">
            <IonHeader className="ord-filter-header ion-no-border">
                <IonToolbar className="ord-filter-toolbar">
                    <IonButtons slot="start">
                        <IonButton color="dark" onClick={onClose}>
                            <IonIcon icon={chevronBackOutline} style={{ fontSize: '26px' }} />
                        </IonButton>
                    </IonButtons>
                    <div className="ord-filter-title">Bộ lọc</div>
                </IonToolbar>
            </IonHeader>

            <IonContent className="ord-filter-content">
                <div className="ord-filter-body">

                    {/* Trạng thái hóa đơn */}
                    <div className="ord-filter-section">
                        <div className="ord-filter-section-header" onClick={() => setStatusOpen(p => !p)}>
                            <span>Trạng thái hóa đơn</span>
                            <IonIcon icon={statusOpen ? chevronDownOutline : chevronForwardOutline} />
                        </div>
                        {statusOpen && STATUS_FILTER_OPTS.map(opt => (
                            <div key={opt.value} className="ord-filter-option" onClick={() => toggleStatus(opt.value)}>
                                <span>{opt.label}</span>
                                <div className={`ord-checkbox${local.statuses.includes(opt.value) ? ' checked' : ''}`}>
                                    {local.statuses.includes(opt.value) && <IonIcon icon={checkmarkOutline} />}
                                </div>
                            </div>
                        ))}
                    </div>

                    {/* Thời gian */}
                    <div className="ord-filter-section">
                        <div className="ord-filter-section-header" onClick={() => setPeriodOpen(p => !p)}>
                            <span>Thời gian giao hàng</span>
                            <IonIcon icon={periodOpen ? chevronDownOutline : chevronForwardOutline} />
                        </div>
                        {periodOpen && PERIOD_LABELS.map(opt => (
                            <div key={opt.value} className="ord-filter-option" onClick={() => setLocal(p => ({ ...p, period: opt.value }))}>
                                <span>{opt.label}</span>
                                <div className={`ord-radio${local.period === opt.value ? ' checked' : ''}`} />
                            </div>
                        ))}
                    </div>
                </div>
            </IonContent>

            <div className="ord-filter-footer">
                <button className="ord-filter-reset" onClick={() => setLocal(DEFAULT_FILTER)}>Đặt lại</button>
                <button className="ord-filter-apply" onClick={() => { onApply(local); onClose(); }}>Áp dụng</button>
            </div>
        </IonModal>
    );
};

// ─── Main Component ─────────────────────────────────────────────────────────────

const Orders: React.FC = () => {
    const ionRouter = useIonRouter();
    const [orders, setOrders] = useState<Order[]>([]);
    const [loading, setLoading] = useState(false);
    const [toast, setToast] = useState<string | null>(null);
    const [filter, setFilter] = useState<FilterState>(DEFAULT_FILTER);
    const [showFilter, setShowFilter] = useState(false);
    const [showPeriod, setShowPeriod] = useState(false);

    const loadData = async (f?: FilterState) => {
        const activeFilter = f ?? filter;
        setLoading(true);
        try {
            // Build query string: status + date range
            const params = new URLSearchParams();
            if (activeFilter.statuses.length > 0) {
                activeFilter.statuses.forEach(s => params.append('status', s));
            }
            const { from, to } = buildDateRange(activeFilter.period);
            if (from) params.set('from', from);
            if (to) params.set('to', to);
            const url = `${endpoints.orders}${params.toString() ? `?${params.toString()}` : ''}`;
            const res = await authApis().get<any>(url);
            const payload = res.data;
            const list = Array.isArray(payload?.data) ? payload.data
                : Array.isArray(payload) ? payload : [];
            setOrders(list);
        } catch {
            setToast('Không thể tải danh sách hóa đơn');
        } finally {
            setLoading(false);
        }
    };

    useIonViewWillEnter(() => { void loadData(); });

    const filtered = useMemo(() => filterOrders(orders, filter), [orders, filter]);

    const grouped = useMemo(() => {
        const map = new Map<string, Order[]>();
        const sorted = [...filtered].sort((a, b) =>
            new Date(b.createdAt || 0).getTime() - new Date(a.createdAt || 0).getTime()
        );
        sorted.forEach(o => {
            const d = o.createdAt ? new Date(o.createdAt) : new Date();
            const label = fmtDayLabel(d);
            if (!map.has(label)) map.set(label, []);
            map.get(label)!.push(o);
        });
        return map;
    }, [filtered]);

    const totalAmount = filtered.reduce((s, o) => s + (o.totalAmount ?? 0), 0);

    const activePeriodLabel = PERIOD_LABELS.find(p => p.value === filter.period)?.label ?? 'Toàn thời gian';

    return (
        <IonPage className="ord-page">
            <IonHeader className="ord-header ion-no-border">
                <div className="ord-top-card">
                    <IonToolbar className="ord-toolbar">
                        <IonButtons slot="start">
                            <IonButton color="dark" onClick={() => ionRouter.goBack()}>
                                <IonIcon icon={chevronBackOutline} style={{ fontSize: '26px' }} />
                            </IonButton>
                        </IonButtons>
                        <div className="ord-title">Hóa đơn</div>
                        <IonButtons slot="end">
                            <IonButton color="dark">
                                <IonIcon icon={searchOutline} style={{ fontSize: '22px' }} />
                            </IonButton>
                            <IonButton color="dark">
                                {/* Sort icon */}
                                <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                                    <path d="M7 16V4M7 4L3 8M7 4L11 8M17 8V20M17 20L21 16M17 20L13 16" />
                                </svg>
                            </IonButton>
                        </IonButtons>
                    </IonToolbar>

                    {/* Filter bar */}
                    <div className="ord-filter-bar">
                        <button className="ord-filter-icon-btn" onClick={() => setShowFilter(true)}>
                            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                                <line x1="4" y1="6" x2="20" y2="6" />
                                <line x1="8" y1="12" x2="16" y2="12" />
                                <line x1="10" y1="18" x2="14" y2="18" />
                            </svg>
                        </button>
                        <button className="ord-period-btn" onClick={() => setShowPeriod(p => !p)}>
                            {activePeriodLabel} <IonIcon icon={chevronDownOutline} />
                        </button>
                    </div>

                    {/* Period quick-select dropdown */}
                    {showPeriod && (
                        <div className="ord-period-dropdown">
                            {PERIOD_LABELS.map(opt => (
                                <div key={opt.value} className={`ord-period-item${filter.period === opt.value ? ' active' : ''}`}
                                    onClick={() => { setFilter(f => ({ ...f, period: opt.value })); setShowPeriod(false); }}>
                                    {opt.label}
                                    {filter.period === opt.value && <IonIcon icon={checkmarkOutline} style={{ color: '#0066FF' }} />}
                                </div>
                            ))}
                        </div>
                    )}

                    {/* Summary */}
                    <div className="ord-summary">
                        <div className="ord-summary-title">
                            Tổng tiền hàng <IonIcon icon={chevronDownOutline} />
                            <span className="ord-summary-amount">{fmt(totalAmount)}</span>
                        </div>
                        <div className="ord-summary-sub">{filtered.length} hóa đơn</div>
                    </div>
                </div>
            </IonHeader>

            <IonContent className="ord-content">
                {loading ? (
                    <div className="ord-loading"><IonSpinner name="crescent" color="primary" /></div>
                ) : filtered.length === 0 ? (
                    <div className="ord-empty">Chưa có hóa đơn nào</div>
                ) : (
                    <div className="ord-list-card">
                        {Array.from(grouped.entries()).map(([label, items]) => (
                            <div key={label} className="ord-list-group">
                                <div className="ord-date-label">{label}</div>
                                {items.map((o, idx) => {
                                    const d = o.createdAt ? new Date(o.createdAt) : new Date();
                                    const timeStr = `${fmtDate(d)} ${fmtTime(d)}`;
                                    const code = `HD${String(o.id).padStart(6, '0')}`;
                                    const firstItem = o.items?.[0];
                                    const extraCount = (o.items?.length ?? 0) - 1;

                                    return (
                                        <div key={o.id}
                                            className={`ord-list-item${idx === items.length - 1 ? ' last-item' : ''}`}
                                            onClick={() => ionRouter.push(`/orders/${o.id}`)}>
                                            <div className="ord-item-top">
                                                <div className="ord-item-customer">
                                                    {o.customerName || 'Khách lẻ'}
                                                </div>
                                                <div className="ord-item-amount">{fmt(o.totalAmount)}</div>
                                            </div>
                                            <div className="ord-item-meta">
                                                <span>{timeStr} · {code}</span>
                                                <span className="ord-item-payment">
                                                    {o.paymentMethod ? PAYMENT_LABEL[o.paymentMethod] ?? o.paymentMethod : 'Chưa thanh toán'}
                                                </span>
                                            </div>
                                            {firstItem && (
                                                <div className="ord-item-products">
                                                    {firstItem.productName} x{firstItem.quantity}
                                                    {extraCount > 0 && (
                                                        <div className="ord-item-extra">+{extraCount} mặt hàng khác</div>
                                                    )}
                                                </div>
                                            )}
                                        </div>
                                    );
                                })}
                            </div>
                        ))}
                    </div>
                )}
            </IonContent>

            {/* FAB → Sales page */}
            <IonFab vertical="bottom" horizontal="end" slot="fixed" style={{ marginBottom: '16px' }}>
                <IonFabButton className="ord-fab" onClick={() => ionRouter.push('/sales')}>
                    <IonIcon icon={addOutline} />
                </IonFabButton>
            </IonFab>

            {/* Filter modal */}
            <FilterModal
                isOpen={showFilter}
                initial={filter}
                onApply={(f) => { setFilter(f); void loadData(f); }}
                onClose={() => setShowFilter(false)}
            />

            {/* Close period dropdown on outside click */}
            {showPeriod && (
                <div className="ord-overlay" onClick={() => setShowPeriod(false)} />
            )}

            <IonToast isOpen={toast !== null} message={toast ?? ''} duration={2500}
                onDidDismiss={() => setToast(null)} />
        </IonPage>
    );
};

export default Orders;
