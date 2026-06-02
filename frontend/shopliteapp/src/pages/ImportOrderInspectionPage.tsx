import React, { useState } from 'react';
import {
    IonButton, IonButtons, IonContent, IonHeader, IonIcon, IonPage,
    IonSpinner, IonToast, IonToolbar, useIonRouter, useIonViewWillEnter
} from '@ionic/react';
import { arrowBackOutline } from 'ionicons/icons';
import { useParams } from 'react-router';
import type { ImportOrder } from '../api/types';
import { importOrderService } from '../services/importOrder.service';
import { useStorePermissions } from '../utils/useStorePermissions';
import './ImportOrderInspectionPage.css';

const fmt = (value: number) => value.toLocaleString('vi-VN');

const ImportOrderInspectionPage: React.FC = () => {
    const { id } = useParams<{ id: string }>();
    const ionRouter = useIonRouter();
    const { can } = useStorePermissions();
    const canInspect = can('/api/v1/import-orders/{id}/inspect', 'POST');
    const [order, setOrder] = useState<ImportOrder | null>(null);
    const [received, setReceived] = useState<Record<number, number>>({});
    const [loading, setLoading] = useState(true);
    const [saving, setSaving] = useState(false);
    const [toast, setToast] = useState<string | null>(null);

    useIonViewWillEnter(() => {
        void (async () => {
            setLoading(true);
            try {
                const data = await importOrderService.getById(id);
                setOrder(data);
                setReceived(Object.fromEntries((data.items || []).map(item => [item.id, item.receivedQuantity ?? item.quantity])));
            } catch (e: any) {
                setToast(e.message || 'Không thể tải phiếu nhập');
            } finally {
                setLoading(false);
            }
        })();
    });

    const save = async () => {
        if (!order || !canInspect) return;
        if (order.status !== 'WAITING_FOR_INSPECTION') {
            setToast('Phiếu này không ở trạng thái chờ kiểm');
            return;
        }
        setSaving(true);
        try {
            const updated = await importOrderService.inspect(order.id, {
                items: (order.items || []).map(item => ({
                    importItemId: item.id,
                    receivedQuantity: received[item.id] ?? 0,
                })),
            });
            setToast(updated.status === 'COMPLETED'
                ? 'Kiểm hàng khớp, tồn kho đã được cập nhật'
                : 'Đã ghi nhận chênh lệch, đang chờ quản lý duyệt');
            setTimeout(() => ionRouter.goBack(), 700);
        } catch (e: any) {
            setToast(e.message || 'Không thể lưu kết quả kiểm hàng');
        } finally {
            setSaving(false);
        }
    };

    return (
        <IonPage className="io-insp-page">
            <IonHeader className="io-insp-header ion-no-border">
                <IonToolbar className="io-insp-toolbar">
                    <IonButtons slot="start">
                        <IonButton color="dark" onClick={() => ionRouter.goBack()}>
                            <IonIcon icon={arrowBackOutline} style={{ fontSize: '26px' }} />
                        </IonButton>
                    </IonButtons>
                    <div className="io-insp-title">Kiểm nhận hàng nhập</div>
                </IonToolbar>
            </IonHeader>
            <IonContent className="io-insp-content">
                {loading ? (
                    <div style={{ display: 'flex', justifyContent: 'center', padding: 40 }}><IonSpinner /></div>
                ) : !order ? (
                    <div style={{ padding: 20 }}>Không tìm thấy phiếu nhập.</div>
                ) : (
                    <div>
                        <div className="io-insp-subtitle">
                            PN{String(order.id).padStart(6, '0')} · {order.supplierName || 'Nhà cung cấp'}
                        </div>
                        {(order.items || []).map(item => {
                            const actual = received[item.id] ?? 0;
                            const delta = actual - item.quantity;
                            return (
                                <div key={item.id} className="io-insp-card">
                                    <div className="io-insp-item-name">{item.productName || 'Sản phẩm'}</div>
                                    <div className="io-insp-item-sku">{item.productSku || '---'}</div>
                                    <div className="io-insp-row">
                                        <span className="io-insp-label">SL đặt: <b>{fmt(item.quantity)}</b></span>
                                        <input
                                            type="number"
                                            min={0}
                                            value={actual}
                                            onChange={event => setReceived(prev => ({ ...prev, [item.id]: Math.max(0, Number(event.target.value) || 0) }))}
                                            className="io-insp-input"
                                        />
                                    </div>
                                    <div className={`io-insp-status ${delta === 0 ? 'match' : 'diff'}`}>
                                        {delta === 0 ? 'Khớp số lượng' : `Chênh lệch: ${delta > 0 ? '+' : ''}${fmt(delta)}`}
                                    </div>
                                </div>
                            );
                        })}
                    </div>
                )}
            </IonContent>
            {order && canInspect && order.status === 'WAITING_FOR_INSPECTION' && (
                <div className="io-insp-footer">
                    <button className="io-insp-btn" disabled={saving} onClick={save}>
                        {saving ? <IonSpinner name="dots" /> : 'Xác nhận kiểm tra hàng'}
                    </button>
                </div>
            )}
            <IonToast isOpen={toast !== null} message={toast ?? ''} duration={2400} onDidDismiss={() => setToast(null)} />
        </IonPage>
    );
};

export default ImportOrderInspectionPage;
