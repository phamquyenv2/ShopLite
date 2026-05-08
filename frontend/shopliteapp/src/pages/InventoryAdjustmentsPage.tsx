import React, { useMemo, useState } from 'react';
import {
    IonButton, IonButtons, IonContent, IonFab, IonFabButton,
    IonHeader, IonIcon, IonPage, IonSpinner, IonToolbar,
    useIonRouter, useIonViewWillEnter
} from '@ionic/react';
import { addOutline, chevronBackOutline, chevronDownOutline, searchOutline } from 'ionicons/icons';
import type { InventoryAdjustment } from '../api/types';
import { inventoryAdjustmentService } from '../services/inventoryAdjustment.service';
import { useStorePermissions } from '../utils/useStorePermissions';
import './InventoryAdjustmentsPage.css';

// ─── Helpers ───────────────────────────────────────────────────────────────────

const fmt = (n?: number | null) => (n ?? 0).toLocaleString('vi-VN');

const formatDateTime = (iso?: string) => {
    const d = iso ? new Date(iso) : new Date();
    const dd = String(d.getDate()).padStart(2, '0');
    const mm = String(d.getMonth() + 1).padStart(2, '0');
    const yyyy = d.getFullYear();
    const hh = String(d.getHours()).padStart(2, '0');
    const min = String(d.getMinutes()).padStart(2, '0');
    return `${dd}/${mm}/${yyyy} ${hh}:${min}`;
};

const formatGroupLabel = (iso?: string) => {
    const d = iso ? new Date(iso) : new Date();
    const today = new Date();
    const dd = String(d.getDate()).padStart(2, '0');
    const mm = String(d.getMonth() + 1).padStart(2, '0');
    const yyyy = d.getFullYear();
    const datePart = `${dd}/${mm}/${yyyy}`;
    const isToday =
        d.getDate() === today.getDate() &&
        d.getMonth() === today.getMonth() &&
        d.getFullYear() === today.getFullYear();
    return isToday ? `HÔM NAY ${datePart}` : datePart;
};

/** Số lượng thực kiểm: currentStock trước điều chỉnh = balanceAfter - (in - out) */
const getTotalActualQty = (adj: InventoryAdjustment): number =>
    (adj.logs || []).reduce((sum, log) => {
        const actual = (log.currentStock ?? log.balanceAfter ?? 0) - (log.quantityIn ?? 0) + (log.quantityOut ?? 0);
        return sum + actual;
    }, 0);

const getStatus = (adj: InventoryAdjustment): { label: string; cls: string } => {
    const logs = adj.logs || [];
    if (logs.length === 0) return { label: 'Chưa có dữ liệu', cls: 'ia-status-pending' };
    const hasDiff = logs.some(l => (l.quantityIn ?? 0) > 0 || (l.quantityOut ?? 0) > 0);
    return hasDiff
        ? { label: 'Đã điều chỉnh', cls: 'ia-status-adjusted' }
        : { label: 'Đã cân bằng', cls: 'ia-status-balanced' };
};

const buildItemNames = (adj: InventoryAdjustment): string =>
    (adj.logs || [])
        .map(log => {
            const actual = (log.currentStock ?? log.balanceAfter ?? 0) - (log.quantityIn ?? 0) + (log.quantityOut ?? 0);
            return `${log.productName || 'Sản phẩm'} x${fmt(actual)}`;
        })
        .join(', ');

// ─── Component ─────────────────────────────────────────────────────────────────

const InventoryAdjustmentsPage: React.FC = () => {
    const ionRouter = useIonRouter();
    const { can } = useStorePermissions();
    const canCreateAdjustment = can('/api/v1/inventory-adjustments', 'POST');
    const [adjustments, setAdjustments] = useState<InventoryAdjustment[]>([]);
    const [loading, setLoading] = useState(false);

    const loadData = async () => {
        setLoading(true);
        try {
            const data = await inventoryAdjustmentService.getAll();
            setAdjustments(data);
        } catch {
            setAdjustments([]);
        } finally {
            setLoading(false);
        }
    };

    useIonViewWillEnter(() => { void loadData(); });

    // Group by date label, newest first
    const grouped = useMemo(() => {
        const map = new Map<string, InventoryAdjustment[]>();
        const sorted = [...adjustments].sort(
            (a, b) => new Date(b.createdAt || 0).getTime() - new Date(a.createdAt || 0).getTime()
        );
        sorted.forEach(item => {
            const label = formatGroupLabel(item.createdAt);
            if (!map.has(label)) map.set(label, []);
            map.get(label)!.push(item);
        });
        return map;
    }, [adjustments]);

    const totalLogs = adjustments.reduce((s, item) => s + (item.logs?.length || 0), 0);

    return (
        <IonPage className="ia-page">
            {/* ── Header ── */}
            <IonHeader className="ia-header ion-no-border">
                <div className="ia-top-card">
                    <IonToolbar className="ia-toolbar">
                        <IonButtons slot="start">
                            <IonButton color="dark" onClick={() => ionRouter.goBack()}>
                                <IonIcon icon={chevronBackOutline} style={{ fontSize: '26px' }} />
                            </IonButton>
                        </IonButtons>

                        <div className="ia-title">Kiểm kho</div>

                        <IonButtons slot="end">
                            <IonButton color="dark">
                                <IonIcon icon={searchOutline} style={{ fontSize: '22px' }} />
                            </IonButton>
                            <IonButton color="dark">
                                {/* Sort icon */}
                                <svg width="22" height="22" viewBox="0 0 24 24" fill="none"
                                    stroke="currentColor" strokeWidth="2"
                                    strokeLinecap="round" strokeLinejoin="round">
                                    <path d="M7 16V4M7 4L3 8M7 4L11 8M17 8V20M17 20L21 16M17 20L13 16" />
                                </svg>
                            </IonButton>
                        </IonButtons>
                    </IonToolbar>

                    {/* Filter bar */}
                    <div className="ia-filter-bar">
                        <button className="ia-filter-btn" type="button" aria-label="Bộ lọc">
                            <svg width="18" height="18" viewBox="0 0 24 24" fill="none"
                                stroke="currentColor" strokeWidth="2"
                                strokeLinecap="round" strokeLinejoin="round">
                                <line x1="4" y1="6" x2="20" y2="6" />
                                <line x1="8" y1="12" x2="16" y2="12" />
                                <line x1="10" y1="18" x2="14" y2="18" />
                            </svg>
                        </button>
                        <button className="ia-period-btn" type="button">
                            Tháng này <IonIcon icon={chevronDownOutline} />
                        </button>
                    </div>

                    {/* Summary */}
                    <div className="ia-summary">
                        <div className="ia-summary-count">{adjustments.length} phiếu</div>
                    </div>
                </div>
            </IonHeader>

            {/* ── Content ── */}
            <IonContent className="ia-content">
                {loading ? (
                    <div className="ia-loading">
                        <IonSpinner name="crescent" color="primary" />
                    </div>
                ) : adjustments.length === 0 ? (
                    <div className="ia-empty">Chưa có phiếu kiểm kho</div>
                ) : (
                    <div className="ia-list-card">
                        {Array.from(grouped.entries()).map(([label, items]) => (
                            <div key={label} className="ia-list-group">
                                <div className="ia-date-label">{label}</div>

                                {items.map((item, idx) => {
                                    const status = getStatus(item);
                                    const totalQty = getTotalActualQty(item);
                                    const itemCount = item.logs?.length || 0;
                                    const code = `KK${String(item.id).padStart(6, '0')}`;
                                    const names = buildItemNames(item);
                                    const isLast = idx === items.length - 1;

                                    return (
                                        <div
                                            key={item.id}
                                            className={`ia-list-item${isLast ? ' last-item' : ''}`}
                                            onClick={() => ionRouter.push(`/inventory-adjustments/${item.id}`)}
                                        >
                                            {/* Row 1: Summary + Status badge */}
                                            <div className="ia-item-head">
                                                <div className="ia-item-title">
                                                    {itemCount} mặt hàng - Số lượng: {fmt(totalQty)}
                                                </div>
                                                <span className={`ia-status ${status.cls}`}>
                                                    {status.label}
                                                </span>
                                            </div>

                                            {/* Row 2: Code (left) + DateTime (right) */}
                                            <div className="ia-item-meta">
                                                <span className="ia-item-code">{code}</span>
                                                <span className="ia-item-time">
                                                    {formatDateTime(item.createdAt)}
                                                </span>
                                            </div>

                                            {/* Row 3: Product names */}
                                            <div className="ia-item-names">{names || item.reason}</div>
                                        </div>
                                    );
                                })}
                            </div>
                        ))}
                    </div>
                )}
            </IonContent>

            {/* ── FAB ── */}
            {canCreateAdjustment && (
                <IonFab vertical="bottom" horizontal="end" slot="fixed" className="ia-fab-wrap">
                    <IonFabButton className="ia-fab" onClick={() => ionRouter.push('/inventory-adjustment/new')}>
                        <IonIcon icon={addOutline} />
                    </IonFabButton>
                </IonFab>
            )}
        </IonPage>
    );
};

export default InventoryAdjustmentsPage;
