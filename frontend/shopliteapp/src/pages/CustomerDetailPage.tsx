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
    IonModal,
    IonActionSheet,
    useIonRouter,
    useIonViewWillEnter,
} from '@ionic/react';
import { useParams } from 'react-router-dom';
import {
    chevronBackOutline,
    ellipsisHorizontalOutline,
    chevronForwardOutline,
    personOutline,
    locationOutline,
    addOutline,
    callOutline,
    peopleOutline,
    receiptOutline,
    trashOutline,
    createOutline,
} from 'ionicons/icons';
import { customerService } from '../services/customer.service';
import type { Customer, Order } from '../api/types';
import './CustomerDetailPage.css';

// ─── Helpers ──────────────────────────────────────────────────────────────────

const fmt = (n: number) => n.toLocaleString('vi-VN');

const fmtDate = (d: Date) =>
    `${String(d.getDate()).padStart(2, '0')}/${String(d.getMonth() + 1).padStart(2, '0')}/${d.getFullYear()}`;

// ─── Main Component ────────────────────────────────────────────────────────────

const CustomerDetailPage: React.FC = () => {
    const ionRouter = useIonRouter();
    const { id } = useParams<{ id: string }>();

    const [customer, setCustomer] = useState<Customer | null>(null);
    const [orders, setOrders] = useState<Order[]>([]);
    const [loading, setLoading] = useState(false);
    const [toast, setToast] = useState<string | null>(null);

    // Action sheet
    const [actionOpen, setActionOpen] = useState(false);

    // Invoice info edit
    const [invoiceEditOpen, setInvoiceEditOpen] = useState(false);

    const totalRevenue = orders.reduce((s, o) => s + (o.totalAmount ?? 0), 0);
    const totalDebt = 0; // placeholder: backend không có field này

    const loadData = async () => {
        setLoading(true);
        try {
            const [cust, ords] = await Promise.all([
                customerService.getCustomerById(id),
                customerService.getOrdersByCustomer(Number(id)),
            ]);
            if (cust) {
                setCustomer(cust);
            }
            setOrders(ords);
        } catch (err: any) {
            setToast(err.message || 'Không thể tải thông tin khách hàng');
        } finally {
            setLoading(false);
        }
    };

    useIonViewWillEnter(() => {
        void loadData();
    });

    const handleGoEdit = () => {
        ionRouter.push(`/customers/${id}/edit`);
    };

    const handleDelete = async () => {
        try {
            await customerService.deleteCustomer(id);
            setToast('Đã xóa khách hàng');
            setTimeout(() => ionRouter.goBack(), 1200);
        } catch (err: any) {
            setToast(err.message || 'Không thể xóa');
        }
    };

    // Mã khách hàng
    const customerCode = customer
        ? `KH${String(customer.id).padStart(6, '0')}`
        : '---';

    if (loading) {
        return (
            <IonPage className="cd-page">
                <IonHeader className="cd-header ion-no-border">
                    <IonToolbar className="cd-toolbar">
                        <IonButtons slot="start">
                            <IonButton color="dark" onClick={() => ionRouter.goBack()}>
                                <IonIcon icon={chevronBackOutline} style={{ fontSize: '26px' }} />
                            </IonButton>
                        </IonButtons>
                        <div className="cd-title">Chi tiết khách hàng</div>
                    </IonToolbar>
                </IonHeader>
                <IonContent className="cd-content">
                    <div className="cd-loading">
                        <IonSpinner name="crescent" color="primary" />
                    </div>
                </IonContent>
            </IonPage>
        );
    }

    return (
        <IonPage className="cd-page">
            <IonHeader className="cd-header ion-no-border">
                <IonToolbar className="cd-toolbar">
                    <IonButtons slot="start">
                        <IonButton color="dark" onClick={() => ionRouter.goBack()}>
                            <IonIcon icon={chevronBackOutline} style={{ fontSize: '26px' }} />
                        </IonButton>
                    </IonButtons>
                    <div className="cd-title">Chi tiết khách hàng</div>
                    <IonButtons slot="end">
                        <IonButton color="dark" onClick={() => setActionOpen(true)}>
                            <IonIcon icon={ellipsisHorizontalOutline} style={{ fontSize: '24px' }} />
                        </IonButton>
                    </IonButtons>
                </IonToolbar>
            </IonHeader>

            <IonContent className="cd-content">
                {/* ── THÔNG TIN CƠ BẢN ─────────────────────────────────── */}
                <div className="cd-section">
                    <div className="cd-section-header">
                        <span className="cd-section-label">THÔNG TIN CƠ BẢN</span>
                        <button className="cd-edit-btn" onClick={handleGoEdit}>
                            Sửa
                        </button>
                    </div>

                    {/* Avatar + Name */}
                    <div className="cd-identity">
                        <div className="cd-avatar">
                            <IonIcon icon={personOutline} />
                        </div>
                        <div className="cd-identity-name">
                            {customer?.name ?? '---'}
                        </div>
                    </div>

                    {/* Mã + Chi nhánh */}
                    <div className="cd-info-grid">
                        <div className="cd-info-cell">
                            <div className="cd-info-label">Mã khách hàng</div>
                            <div className="cd-info-value">{customerCode}</div>
                        </div>
                        <div className="cd-info-cell">
                            <div className="cd-info-label">Chi nhánh</div>
                            <div className="cd-info-value">Chi nhánh trung tâm</div>
                        </div>
                    </div>
                </div>

                {/* ── LỊCH SỬ GIAO DỊCH & CÔNG NỢ ────────────────────────── */}
                <div className="cd-section cd-section-rows">
                    <div
                        className="cd-row-item"
                        onClick={() => ionRouter.push(`/customers/${id}/orders`)}
                    >
                        <div className="cd-row-label">
                            <IonIcon icon={receiptOutline} className="cd-row-icon" />
                            Lịch sử giao dịch
                        </div>
                        <div className="cd-row-right">
                            <span className="cd-row-value">{fmt(totalRevenue)}</span>
                            <IonIcon icon={chevronForwardOutline} className="cd-row-arrow" />
                        </div>
                    </div>

                    <div className="cd-divider" />

                    <div 
                        className="cd-row-item"
                        onClick={() => ionRouter.push(`/customers/${id}/debt`)}
                    >
                        <div className="cd-row-label">
                            <IonIcon icon={receiptOutline} className="cd-row-icon" />
                            Công nợ
                        </div>
                        <div className="cd-row-right">
                            <span className="cd-row-value">{fmt(totalDebt)}</span>
                            <IonIcon icon={chevronForwardOutline} className="cd-row-arrow" />
                        </div>
                    </div>
                </div>

                {/* ── ĐỊA CHỈ ─────────────────────────────────────────── */}
                <div className="cd-section cd-section-collapse">
                    <div className="cd-collapse-header">
                        <IonIcon icon={locationOutline} className="cd-collapse-icon" />
                        <span className="cd-collapse-label">Địa chỉ</span>
                    </div>

                    <div className="cd-row-item cd-add-row" onClick={() => { }}>
                        <IonIcon icon={addOutline} className="cd-add-icon" />
                        <span className="cd-add-label">Thêm địa chỉ giao hàng</span>
                        <IonIcon icon={chevronForwardOutline} className="cd-row-arrow" />
                    </div>
                </div>

                {/* ── LIÊN HỆ ─────────────────────────────────────────── */}
                <div className="cd-section cd-section-collapse">
                    <div className="cd-collapse-header">
                        <IonIcon icon={callOutline} className="cd-collapse-icon" />
                        <span className="cd-collapse-label">Liên hệ</span>
                    </div>

                    {customer?.phone && (
                        <div className="cd-contact-item">
                            <IonIcon icon={callOutline} className="cd-contact-icon" />
                            <span>{customer.phone}</span>
                        </div>
                    )}
                </div>

                {/* ── NHÓM KHÁCH HÀNG ─────────────────────────────────── */}
                <div className="cd-section cd-section-collapse">
                    <div className="cd-collapse-header">
                        <IonIcon icon={peopleOutline} className="cd-collapse-icon" />
                        <span className="cd-collapse-label">Nhóm khách hàng</span>
                    </div>
                </div>

                {/* ── THÔNG TIN XUẤT HOÁ ĐƠN ──────────────────────────── */}
                <div className="cd-section">
                    <div className="cd-section-header">
                        <span className="cd-section-label">THÔNG TIN XUẤT HOÁ ĐƠN</span>
                        <button className="cd-edit-btn" onClick={() => setInvoiceEditOpen(true)}>
                            Sửa
                        </button>
                    </div>

                    <div className="cd-invoice-field">
                        <div className="cd-invoice-label">Loại khách hàng</div>
                        <div className="cd-invoice-value">Cá nhân</div>
                    </div>
                </div>

                {/* Bottom spacer */}
                <div style={{ height: '32px' }} />
            </IonContent>

            {/* ── ACTION SHEET ───────────────────────────────────────────────── */}
            <IonActionSheet
                isOpen={actionOpen}
                onDidDismiss={() => setActionOpen(false)}
                header="Tùy chọn"
                buttons={[
                    {
                        text: 'Chỉnh sửa',
                        icon: createOutline,
                        handler: handleGoEdit,
                    },
                    {
                        text: 'Xóa khách hàng',
                        icon: trashOutline,
                        role: 'destructive',
                        handler: handleDelete,
                    },
                    {
                        text: 'Hủy',
                        role: 'cancel',
                    },
                ]}
            />

            {/* ── INVOICE EDIT MODAL (placeholder) ─────────────────────────── */}
            <IonModal
                isOpen={invoiceEditOpen}
                onDidDismiss={() => setInvoiceEditOpen(false)}
                className="cd-edit-modal"
            >
                <div className="cd-modal-header">
                    <button className="cd-modal-back" onClick={() => setInvoiceEditOpen(false)}>
                        <IonIcon icon={chevronBackOutline} />
                    </button>
                    <span className="cd-modal-title">Thông tin xuất hoá đơn</span>
                </div>
                <IonContent style={{ '--background': '#f4f6f9' }}>
                    <div className="cd-modal-form">
                        <div className="cd-form-field">
                            <label>Loại khách hàng</label>
                            <input type="text" defaultValue="Cá nhân" readOnly />
                        </div>
                    </div>
                </IonContent>
                <div className="cd-modal-footer">
                    <button
                        className="cd-modal-submit"
                        onClick={() => setInvoiceEditOpen(false)}
                    >
                        Đóng
                    </button>
                </div>
            </IonModal>

            <IonToast
                isOpen={toast !== null}
                message={toast ?? ''}
                duration={2000}
                onDidDismiss={() => setToast(null)}
            />
        </IonPage>
    );
};

export default CustomerDetailPage;
