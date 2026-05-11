import React, { useState } from 'react';
import {
    IonPage,
    IonHeader,
    IonToolbar,
    IonContent,
    IonIcon,
    IonButtons,
    IonButton,
    IonSpinner,
    IonToast,
    IonActionSheet,
    useIonRouter,
    useIonViewWillEnter,
} from '@ionic/react';
import { useParams } from 'react-router-dom';
import {
    chevronBackOutline,
    ellipsisHorizontalOutline,
    chevronForwardOutline,
    createOutline,
    trashOutline,
} from 'ionicons/icons';
import { supplierService } from '../services/supplier.service';
import { importOrderService } from '../services/importOrder.service';
import type { Supplier, ImportOrder } from '../api/types';
import './SupplierDetailPage.css';

const fmt = (n: number) => n.toLocaleString('vi-VN');

const SupplierDetailPage: React.FC = () => {
    const ionRouter = useIonRouter();
    const { id } = useParams<{ id: string }>();

    const [supplier, setSupplier] = useState<Supplier | null>(null);
    const [orders, setOrders] = useState<ImportOrder[]>([]);
    const [loading, setLoading] = useState(false);
    const [toast, setToast] = useState<string | null>(null);
    const [actionOpen, setActionOpen] = useState(false);

    const totalImportAmount = orders.reduce((sum, o) => sum + (o.totalAmount || 0), 0);
    const totalDebt = 0; // Placeholder like CustomerDetailPage

    const loadData = async () => {
        setLoading(true);
        try {
            const [s, allOrders] = await Promise.all([
                supplierService.getById(id),
                importOrderService.getAll()
            ]);
            setSupplier(s);
            setOrders(allOrders.filter(o => o.supplierId === Number(id)));
        } catch (err: any) {
            setToast(err.message || 'Không thể tải thông tin nhà cung cấp');
        } finally {
            setLoading(false);
        }
    };

    useIonViewWillEnter(() => { void loadData(); });

    const handleDelete = async () => {
        try {
            await supplierService.delete(id);
            setToast('Đã xóa nhà cung cấp');
            setTimeout(() => ionRouter.goBack(), 1200);
        } catch (err: any) {
            setToast(err.message || 'Không thể xóa');
        }
    };

    const supplierCode = supplier
        ? `NCC${String(supplier.id).padStart(4, '0')}`
        : '---';

    if (loading) {
        return (
            <IonPage className="sd-page">
                <IonHeader className="sd-header ion-no-border">
                    <IonToolbar className="sd-toolbar">
                        <IonButtons slot="start">
                            <IonButton color="dark" onClick={() => ionRouter.goBack()}>
                                <IonIcon icon={chevronBackOutline} style={{ fontSize: '26px' }} />
                            </IonButton>
                        </IonButtons>
                    </IonToolbar>
                </IonHeader>
                <IonContent className="sd-content">
                    <div className="sd-loading"><IonSpinner name="crescent" color="primary" /></div>
                </IonContent>
            </IonPage>
        );
    }

    return (
        <IonPage className="sd-page">
            <IonHeader className="sd-header ion-no-border">
                <IonToolbar className="sd-toolbar">
                    <IonButtons slot="start">
                        <IonButton color="dark" onClick={() => ionRouter.goBack()}>
                            <IonIcon icon={chevronBackOutline} style={{ fontSize: '26px' }} />
                        </IonButton>
                    </IonButtons>
                    <IonButtons slot="end">
                        <IonButton color="dark" onClick={() => setActionOpen(true)}>
                            <IonIcon icon={ellipsisHorizontalOutline} style={{ fontSize: '24px' }} />
                        </IonButton>
                    </IonButtons>
                </IonToolbar>
            </IonHeader>

            <IonContent className="sd-content">
                {/* THÔNG TIN CƠ BẢN */}
                <div className="sd-section-header">
                    <span className="sd-section-label">THÔNG TIN CƠ BẢN</span>
                    <button className="sd-edit-btn" onClick={() => ionRouter.push(`/suppliers/${id}/edit`)}>Sửa</button>
                </div>

                <div className="sd-card">
                    <div className="sd-field">
                        <div className="sd-field-label">Tên nhà cung cấp</div>
                        <div className="sd-field-value">{supplier?.name ?? '---'}</div>
                    </div>
                    <div className="sd-divider" />
                    <div className="sd-field">
                        <div className="sd-field-label">Mã nhà cung cấp</div>
                        <div className="sd-field-value">{supplierCode}</div>
                    </div>
                    <div className="sd-divider" />
                    <div className="sd-field">
                        <div className="sd-field-label">Chi nhánh</div>
                        <div className="sd-field-value">Chi nhánh trung tâm</div>
                    </div>
                </div>

                {/* GIAO DỊCH & CÔNG NỢ */}
                <div className="sd-card">
                    <div className="sd-row" onClick={() => ionRouter.push(`/suppliers/${id}/orders`)}>
                        <div className="sd-row-left">Lịch sử giao dịch</div>
                        <div className="sd-row-right">
                            <span className="sd-row-value">{fmt(totalImportAmount)}</span>
                            <IonIcon icon={chevronForwardOutline} className="sd-row-arrow" />
                        </div>
                    </div>
                    <div className="sd-divider" />
                    <div className="sd-row" onClick={() => ionRouter.push(`/suppliers/${id}/debt`)}>
                        <div className="sd-row-left">Công nợ</div>
                        <div className="sd-row-right">
                            <span className="sd-row-value">{fmt(totalDebt)}</span>
                            <IonIcon icon={chevronForwardOutline} className="sd-row-arrow" />
                        </div>
                    </div>
                </div>

                {/* ĐỊA CHỈ */}
                <div className="sd-card">
                    {supplier?.address ? (
                        <div className="sd-field" onClick={() => ionRouter.push(`/suppliers/${id}/edit`)}>
                            <div className="sd-field-label">Địa chỉ</div>
                            <div className="sd-field-value">{supplier.address}</div>
                        </div>
                    ) : (
                        <div className="sd-row" onClick={() => ionRouter.push(`/suppliers/${id}/edit`)}>
                            <div className="sd-blue-text">Địa chỉ</div>
                        </div>
                    )}
                </div>

                {/* EMAIL */}
                <div className="sd-card">
                    {supplier?.email ? (
                        <div className="sd-field" onClick={() => ionRouter.push(`/suppliers/${id}/edit`)}>
                            <div className="sd-field-label">Email</div>
                            <div className="sd-field-value">{supplier.email}</div>
                        </div>
                    ) : (
                        <div className="sd-row" onClick={() => ionRouter.push(`/suppliers/${id}/edit`)}>
                            <div className="sd-blue-text">Email</div>
                        </div>
                    )}
                </div>

                <div style={{ height: 32 }} />
            </IonContent>

            <IonActionSheet
                isOpen={actionOpen}
                onDidDismiss={() => setActionOpen(false)}
                header="Tùy chọn"
                buttons={[
                    {
                        text: 'Chỉnh sửa',
                        icon: createOutline,
                        handler: () => ionRouter.push(`/suppliers/${id}/edit`),
                    },
                    {
                        text: 'Xóa nhà cung cấp',
                        icon: trashOutline,
                        role: 'destructive',
                        handler: handleDelete,
                    },
                    { text: 'Hủy', role: 'cancel' },
                ]}
            />

            <IonToast
                isOpen={toast !== null}
                message={toast ?? ''}
                duration={2000}
                onDidDismiss={() => setToast(null)}
            />
        </IonPage>
    );
};

export default SupplierDetailPage;
