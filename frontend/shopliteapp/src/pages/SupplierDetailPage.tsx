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
    callOutline,
    mailOutline,
    locationOutline,
    createOutline,
    trashOutline,
    chevronForwardOutline,
} from 'ionicons/icons';
import { supplierService } from '../services/supplier.service';
import type { Supplier } from '../api/types';
import './SupplierDetailPage.css';

const SupplierDetailPage: React.FC = () => {
    const ionRouter = useIonRouter();
    const { id } = useParams<{ id: string }>();

    const [supplier, setSupplier] = useState<Supplier | null>(null);
    const [loading, setLoading] = useState(false);
    const [toast, setToast] = useState<string | null>(null);
    const [actionOpen, setActionOpen] = useState(false);

    const loadData = async () => {
        setLoading(true);
        try {
            const s = await supplierService.getById(id);
            setSupplier(s);
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
                        <div className="sd-title">Chi tiết nhà cung cấp</div>
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
                    <div className="sd-title">Chi tiết nhà cung cấp</div>
                    <IonButtons slot="end">
                        <IonButton color="dark" onClick={() => setActionOpen(true)}>
                            <IonIcon icon={ellipsisHorizontalOutline} style={{ fontSize: '24px' }} />
                        </IonButton>
                    </IonButtons>
                </IonToolbar>
            </IonHeader>

            <IonContent className="sd-content">
                {/* THÔNG TIN CƠ BẢN */}
                <div className="sd-section">
                    <div className="sd-section-header">
                        <span className="sd-section-label">THÔNG TIN CƠ BẢN</span>
                        <button className="sd-edit-btn" onClick={() => ionRouter.push(`/suppliers/${id}/edit`)}>Sửa</button>
                    </div>

                    <div className="sd-name-row">
                        <div className="sd-avatar">
                            <IonIcon icon={chevronBackOutline} style={{ opacity: 0 }} />
                            <span>{supplier?.name?.charAt(0)?.toUpperCase() ?? 'N'}</span>
                        </div>
                        <div className="sd-name">{supplier?.name ?? '---'}</div>
                    </div>
                </div>

                {/* LIÊN HỆ */}
                {(supplier?.phone || supplier?.email || supplier?.address) && (
                    <div className="sd-section">
                        <div className="sd-section-header">
                            <span className="sd-section-label">LIÊN HỆ</span>
                        </div>

                        {supplier?.phone && (
                            <div className="sd-contact-item">
                                <IonIcon icon={callOutline} className="sd-contact-icon" />
                                <span>{supplier.phone}</span>
                            </div>
                        )}
                        {supplier?.email && (
                            <div className="sd-contact-item">
                                <IonIcon icon={mailOutline} className="sd-contact-icon" />
                                <span>{supplier.email}</span>
                            </div>
                        )}
                        {supplier?.address && (
                            <div className="sd-contact-item">
                                <IonIcon icon={locationOutline} className="sd-contact-icon" />
                                <span>{supplier.address}</span>
                            </div>
                        )}
                    </div>
                )}

                {/* LỊCH SỬ NHẬP HÀNG */}
                <div className="sd-section sd-section-rows">
                    <div
                        className="sd-row-item"
                        onClick={() => ionRouter.push(`/import-orders?supplierId=${id}`)}
                    >
                        <div className="sd-row-label">Lịch sử nhập hàng</div>
                        <IonIcon icon={chevronForwardOutline} className="sd-row-arrow" />
                    </div>
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
