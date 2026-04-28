import React, { useMemo, useState } from 'react';
import {
    IonButton, IonButtons, IonContent, IonHeader, IonIcon,
    IonPage, IonSpinner, IonToast, IonToolbar,
    useIonRouter, useIonViewWillEnter
} from '@ionic/react';
import { chevronBackOutline, ellipsisHorizontalOutline, locationOutline } from 'ionicons/icons';
import { useParams } from 'react-router';
import type { InventoryAdjustment, InventoryLog } from '../api/types';
import { inventoryAdjustmentService } from '../services/inventoryAdjustment.service';
import './InventoryAdjustmentDetailPage.css';

// ─── Helpers ───────────────────────────────────────────────────────────────────

const fmt = (n: number | null | undefined) => (n ?? 0).toLocaleString('vi-VN');

const fmtDateTime = (s?: string) => {
    if (!s) return '';
    const d = new Date(s);
    return `${String(d.getDate()).padStart(2, '0')}/${String(d.getMonth() + 1).padStart(2, '0')}/${d.getFullYear()} ${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`;
};

// Per-log: actualQty = balanceAfter – quantityIn + quantityOut  (before the adjustment)
// Delta = quantityIn – quantityOut  (positive = increased, negative = decreased)
const getLogDelta = (log: InventoryLog) => (log.quantityIn ?? 0) - (log.quantityOut ?? 0);
const getLogActual = (log: InventoryLog) => {
    const balance = log.balanceAfter ?? log.currentStock ?? 0;
    const qIn = log.quantityIn ?? 0;
    const qOut = log.quantityOut ?? 0;
    return balance - qIn + qOut;
};
const getLogStock = (log: InventoryLog) => log.currentStock ?? log.balanceAfter ?? 0;

// Status derived from logs: all deltas === 0 → "Đã cân bằng", else "Đã điều chỉnh"
const getStatus = (adj: InventoryAdjustment): 'balanced' | 'adjusted' => {
    const logs = adj.logs ?? [];
    if (logs.length === 0) return 'balanced';
    return logs.every(l => getLogDelta(l) === 0) ? 'balanced' : 'adjusted';
};

// ─── Component ─────────────────────────────────────────────────────────────────

const InventoryAdjustmentDetailPage: React.FC = () => {
    const { id } = useParams<{ id: string }>();
    const ionRouter = useIonRouter();

    const [adjustment, setAdjustment] = useState<InventoryAdjustment | null>(null);
    const [loading, setLoading] = useState(true);
    const [toast, setToast] = useState<string | null>(null);

    useIonViewWillEnter(() => {
        setLoading(true);
        inventoryAdjustmentService.getById(id)
            .then(setAdjustment)
            .catch((e: any) => setToast(e.message || 'Không thể tải phiếu kiểm kho'))
            .finally(() => setLoading(false));
    });

    // ── Summary calculations ────────────────────────────────────────────────────

    const summary = useMemo(() => {
        const logs = adjustment?.logs ?? [];

        const totalActual = logs.reduce((s, l) => s + getLogActual(l), 0);

        const incLogs = logs.filter(l => getLogDelta(l) > 0);
        const decLogs = logs.filter(l => getLogDelta(l) < 0);

        const totalIncrease = incLogs.reduce((s, l) => s + getLogDelta(l), 0);
        const totalDecrease = decLogs.reduce((s, l) => s + getLogDelta(l), 0);
        const totalDelta = logs.reduce((s, l) => s + getLogDelta(l), 0);

        return { logs, totalActual, incLogs, decLogs, totalIncrease, totalDecrease, totalDelta };
    }, [adjustment]);

    // ── Code label ──────────────────────────────────────────────────────────────
    const code = adjustment ? `KK${String(adjustment.id).padStart(6, '0')}` : '';

    // ────────────────────────────────────────────────────────────────────────────

    if (loading) return (
        <IonPage className="iad-page">
            <IonContent>
                <div className="iad-loading"><IonSpinner name="crescent" color="primary" /></div>
            </IonContent>
        </IonPage>
    );

    if (!adjustment) return (
        <IonPage className="iad-page">
            <IonHeader className="iad-header ion-no-border">
                <IonToolbar className="iad-toolbar">
                    <IonButtons slot="start">
                        <IonButton color="dark" onClick={() => ionRouter.goBack()}>
                            <IonIcon icon={chevronBackOutline} style={{ fontSize: '26px' }} />
                        </IonButton>
                    </IonButtons>
                </IonToolbar>
            </IonHeader>
            <IonContent className="iad-content">
                <div className="iad-loading">Không tìm thấy phiếu kiểm kho</div>
            </IonContent>
        </IonPage>
    );

    const status = getStatus(adjustment);

    return (
        <IonPage className="iad-page">

            {/* ── Header ── */}
            <IonHeader className="iad-header ion-no-border">
                <IonToolbar className="iad-toolbar">
                    <IonButtons slot="start">
                        <IonButton color="dark" onClick={() => ionRouter.goBack()}>
                            <IonIcon icon={chevronBackOutline} style={{ fontSize: '26px' }} />
                        </IonButton>
                    </IonButtons>
                    <IonButtons slot="end">
                        <IonButton color="dark">
                            <IonIcon icon={ellipsisHorizontalOutline} style={{ fontSize: '22px' }} />
                        </IonButton>
                    </IonButtons>
                </IonToolbar>
            </IonHeader>

            <IonContent className="iad-content">

                {/* ── Hero card (code + date + status) ── */}
                <div className="iad-hero-card">
                    <div className="iad-hero-left">
                        <div className="iad-code">{code}</div>
                        <div className="iad-date">{fmtDateTime(adjustment.createdAt)}</div>
                    </div>
                    <div className={`iad-status-badge ${status}`}>
                        {status === 'balanced' ? 'Đã cân bằng' : 'Đã điều chỉnh'}
                    </div>
                </div>

                {/* ── Branch / actor card ── */}
                <div className="iad-info-card">
                    <div className="iad-branch-name">Chi nhánh trung tâm</div>
                    <div className="iad-actor-row">
                        <IonIcon icon={locationOutline} className="iad-loc-icon" />
                        <div className="iad-actor-lines">
                            <div className="iad-actor-line">
                                {fmtDateTime(adjustment.createdAt)}&nbsp;•&nbsp;
                                <span className="iad-actor-name">{adjustment.createdBy} cân bằng</span>
                            </div>
                        </div>
                    </div>
                </div>

                {/* ── Product table ── */}
                <div className="iad-table-card">
                    {/* Header */}
                    <div className="iad-table-header">
                        <span className="iad-th-product">HÀNG HOÁ</span>
                        <span className="iad-th-actual">THỰC TẾ</span>
                        <span className="iad-th-delta">LỆCH</span>
                    </div>

                    {/* Rows */}
                    {summary.logs.map((log, idx) => {
                        const actualQty = getLogActual(log);
                        const delta = getLogDelta(log);
                        const stock = getLogStock(log);

                        return (
                            <div key={log.id}
                                className={`iad-table-row${idx === summary.logs.length - 1 ? ' last' : ''}`}>
                                <div className="iad-tr-product">
                                    <div className="iad-tr-name">{log.productName || 'Sản phẩm'}</div>
                                    <div className="iad-tr-sku">{log.productSku || '---'}</div>
                                    <div className="iad-tr-stock">Tồn kho: {fmt(stock - delta)}</div>
                                </div>
                                <div className="iad-tr-actual">{fmt(actualQty)}</div>
                                <div className={`iad-tr-delta${delta < 0 ? ' neg' : delta > 0 ? ' pos' : ''}`}>
                                    {delta > 0 ? `+${fmt(delta)}` : fmt(delta)}
                                </div>
                            </div>
                        );
                    })}

                    {summary.logs.length === 0 && (
                        <div className="iad-table-empty">Không có sản phẩm</div>
                    )}
                </div>

                {/* ── Summary card ── */}
                <div className="iad-summary-card">
                    <div className="iad-sum-row">
                        <div>
                            <div className="iad-sum-label">Tổng thực tế</div>
                            <div className="iad-sum-sub">
                                {summary.logs.length} mặt hàng&nbsp;•&nbsp;Số lượng: {fmt(summary.totalActual)}
                            </div>
                        </div>
                        <div className="iad-sum-value">{fmt(summary.totalActual)}</div>
                    </div>

                    <div className="iad-sum-row">
                        <div>
                            <div className="iad-sum-label">Tổng lệch tăng</div>
                            <div className="iad-sum-sub">
                                {summary.incLogs.length} mặt hàng&nbsp;•&nbsp;Số lượng: {fmt(summary.totalIncrease)}
                            </div>
                        </div>
                        <div className={`iad-sum-value${summary.totalIncrease > 0 ? ' pos' : ''}`}>
                            {summary.totalIncrease > 0 ? `+${fmt(summary.totalIncrease)}` : 0}
                        </div>
                    </div>

                    <div className="iad-sum-row">
                        <div>
                            <div className="iad-sum-label">Tổng lệch giảm</div>
                            <div className="iad-sum-sub">
                                {summary.decLogs.length} mặt hàng&nbsp;•&nbsp;Số lượng: {fmt(summary.totalDecrease)}
                            </div>
                        </div>
                        <div className={`iad-sum-value${summary.totalDecrease < 0 ? ' neg' : ''}`}>
                            {summary.totalDecrease < 0 ? fmt(summary.totalDecrease) : 0}
                        </div>
                    </div>

                    <div className="iad-sum-row last">
                        <div>
                            <div className="iad-sum-label">Tổng chênh lệch</div>
                            <div className="iad-sum-sub">
                                {summary.logs.filter(l => getLogDelta(l) !== 0).length} mặt hàng&nbsp;•&nbsp;Số lượng: {fmt(summary.totalDelta)}
                            </div>
                        </div>
                        <div className={`iad-sum-value${summary.totalDelta < 0 ? ' neg' : summary.totalDelta > 0 ? ' pos' : ''}`}>
                            {summary.totalDelta > 0 ? `+${fmt(summary.totalDelta)}` : fmt(summary.totalDelta)}
                        </div>
                    </div>
                </div>

                {/* ── Note ── */}
                {(adjustment.reason || adjustment.note) && (
                    <div className="iad-note-card">
                        <div className="iad-note-label">Ghi chú</div>
                        <div className="iad-note-text">{adjustment.note || adjustment.reason}</div>
                    </div>
                )}

            </IonContent>

            <IonToast isOpen={toast !== null} message={toast ?? ''} duration={2000}
                onDidDismiss={() => setToast(null)} />
        </IonPage>
    );
};

export default InventoryAdjustmentDetailPage;
