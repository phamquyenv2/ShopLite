import React, { useEffect, useMemo, useState } from 'react';
import {
    IonButton,
    IonButtons,
    IonContent,
    IonHeader,
    IonIcon,
    IonModal,
    IonPage,
    IonSpinner,
    IonToast,
    IonToolbar,
    useIonRouter,
    useIonViewWillEnter,
} from '@ionic/react';
import {
    addOutline,
    businessOutline,
    chevronBackOutline,
    closeOutline,
    createOutline,
    locationOutline,
    saveOutline,
    trashOutline,
} from 'ionicons/icons';
import type { Office } from '../api/types';
import { officeService, type OfficeUpsert } from '../services/office.service';
import { useStorePermissions } from '../utils/useStorePermissions';
import './OfficesPage.css';

type OfficeForm = {
    name: string;
    officeLat: string;
    officeLng: string;
    radius: string;
};

const emptyForm: OfficeForm = {
    name: '',
    officeLat: '',
    officeLng: '',
    radius: '200',
};

const toForm = (office?: Office | null): OfficeForm => office ? {
    name: office.name || '',
    officeLat: String(office.officeLat ?? ''),
    officeLng: String(office.officeLng ?? ''),
    radius: String(office.radius ?? 200),
} : emptyForm;

const OfficesPage: React.FC = () => {
    const router = useIonRouter();
    const { can } = useStorePermissions();
    const canCreate = can('/api/v1/offices', 'POST');
    const canUpdate = can('/api/v1/offices/{id}', 'PUT');
    const canDelete = can('/api/v1/offices/{id}', 'DELETE');

    const [offices, setOffices] = useState<Office[]>([]);
    const [loading, setLoading] = useState(false);
    const [saving, setSaving] = useState(false);
    const [toast, setToast] = useState<string | null>(null);
    const [editingOffice, setEditingOffice] = useState<Office | null>(null);
    const [modalOpen, setModalOpen] = useState(false);
    const [form, setForm] = useState<OfficeForm>(emptyForm);

    const isEdit = editingOffice !== null;
    const canSave = isEdit ? canUpdate : canCreate;

    const loadOffices = async () => {
        setLoading(true);
        try {
            setOffices(await officeService.getOffices());
        } catch (err: any) {
            setToast(err.message || 'Không thể tải danh sách văn phòng');
        } finally {
            setLoading(false);
        }
    };

    useIonViewWillEnter(() => {
        loadOffices();
    });

    const summary = useMemo(() => {
        if (offices.length === 0) return 'Chưa có văn phòng';
        return `${offices.length} văn phòng`;
    }, [offices.length]);

    const openCreate = () => {
        setEditingOffice(null);
        setForm(emptyForm);
        setModalOpen(true);
    };

    const openEdit = (office: Office) => {
        setEditingOffice(office);
        setForm(toForm(office));
        setModalOpen(true);
    };

    const setField = (key: keyof OfficeForm, value: string) => {
        setForm(prev => ({ ...prev, [key]: value }));
    };

    const buildPayload = (): OfficeUpsert | null => {
        const name = form.name.trim();
        const officeLat = Number(form.officeLat);
        const officeLng = Number(form.officeLng);
        const radius = Number(form.radius);

        if (!name) {
            setToast('Vui lòng nhập tên văn phòng');
            return null;
        }
        if (!Number.isFinite(officeLat) || !Number.isFinite(officeLng)) {
            setToast('Tọa độ không hợp lệ');
            return null;
        }
        if (!Number.isFinite(radius) || radius <= 0) {
            setToast('Bán kính phải lớn hơn 0');
            return null;
        }
        return {
            name,
            officeLat,
            officeLng,
            radius,
        };
    };

    const saveOffice = async () => {
        if (!canSave) return;
        const payload = buildPayload();
        if (!payload) return;

        setSaving(true);
        try {
            const saved = isEdit && editingOffice
                ? await officeService.updateOffice(editingOffice.id, payload)
                : await officeService.createOffice(payload);
            setOffices(prev => isEdit
                ? prev.map(item => item.id === saved.id ? saved : item)
                : [...prev, saved]);
            setToast(isEdit ? 'Đã cập nhật văn phòng' : 'Đã tạo văn phòng');
            setModalOpen(false);
        } catch (err: any) {
            setToast(err.message || 'Không thể lưu văn phòng');
        } finally {
            setSaving(false);
        }
    };

    const deleteOffice = async (office: Office) => {
        if (!canDelete) return;
        setSaving(true);
        try {
            await officeService.deleteOffice(office.id);
            setOffices(prev => prev.filter(item => item.id !== office.id));
            setToast('Đã xóa văn phòng');
        } catch (err: any) {
            setToast(err.message || 'Không thể xóa văn phòng');
        } finally {
            setSaving(false);
        }
    };

    useEffect(() => {
        if (!modalOpen) {
            setEditingOffice(null);
            setForm(emptyForm);
        }
    }, [modalOpen]);

    return (
        <IonPage className="offices-page">
            <IonHeader className="op-header ion-no-border">
                <IonToolbar className="op-toolbar">
                    <IonButtons slot="start">
                        <IonButton color="dark" onClick={() => router.goBack()}>
                            <IonIcon icon={chevronBackOutline} />
                        </IonButton>
                    </IonButtons>
                    <div className="op-title">Văn phòng</div>
                    {canCreate && (
                        <IonButtons slot="end">
                            <IonButton color="primary" onClick={openCreate}>
                                <IonIcon icon={addOutline} />
                            </IonButton>
                        </IonButtons>
                    )}
                </IonToolbar>
            </IonHeader>

            <IonContent className="op-content">
                <div className="op-hero">
                    <div>
                        <span className="op-kicker">Cài đặt chung</span>
                        <h1>Văn phòng</h1>
                        <p>{summary}</p>
                    </div>
                    <IonIcon icon={businessOutline} />
                </div>

                {loading ? (
                    <div className="op-loading"><IonSpinner name="crescent" /></div>
                ) : offices.length === 0 ? (
                    <div className="op-empty">Chưa có văn phòng nào</div>
                ) : (
                    <div className="op-list">
                        {offices.map(office => (
                            <div className="op-card" key={office.id}>
                                <div className="op-card-main">
                                    <div className="op-card-icon">
                                        <IonIcon icon={businessOutline} />
                                    </div>
                                    <div className="op-card-info">
                                        <h2>{office.name}</h2>
                                        <div className="op-meta">
                                            <span><IonIcon icon={locationOutline} /> {office.officeLat}, {office.officeLng}</span>
                                        </div>
                                    </div>
                                </div>
                                <div className="op-card-actions">
                                    <button onClick={() => openEdit(office)} disabled={!canUpdate}>
                                        <IonIcon icon={createOutline} />
                                    </button>
                                    <button onClick={() => deleteOffice(office)} disabled={!canDelete || saving}>
                                        <IonIcon icon={trashOutline} />
                                    </button>
                                </div>
                            </div>
                        ))}
                    </div>
                )}
            </IonContent>

            <IonModal isOpen={modalOpen} onDidDismiss={() => setModalOpen(false)} className="op-modal">
                <div className="op-modal-header">
                    <button onClick={() => setModalOpen(false)}>
                        <IonIcon icon={closeOutline} />
                    </button>
                    <span>{isEdit ? 'Sửa văn phòng' : 'Thêm văn phòng'}</span>
                </div>
                <IonContent className="op-modal-content">
                    <div className="op-form">
                        <label>
                            Tên văn phòng
                            <input value={form.name} onChange={e => setField('name', e.target.value)} placeholder="Chi nhánh trung tâm" />
                        </label>
                        <div className="op-form-grid">
                            <label>
                                Vĩ độ
                                <input type="number" inputMode="decimal" value={form.officeLat} onChange={e => setField('officeLat', e.target.value)} />
                            </label>
                            <label>
                                Kinh độ
                                <input type="number" inputMode="decimal" value={form.officeLng} onChange={e => setField('officeLng', e.target.value)} />
                            </label>
                        </div>
                        <label>
                            Bán kính chấm công (m)
                            <input type="number" min={1} inputMode="numeric" value={form.radius} onChange={e => setField('radius', e.target.value)} />
                        </label>
                    </div>
                </IonContent>
                <div className="op-footer">
                    <button disabled={!canSave || saving} onClick={saveOffice}>
                        <IonIcon icon={saveOutline} />
                        {saving ? 'Đang lưu...' : 'Lưu văn phòng'}
                    </button>
                </div>
            </IonModal>

            <IonToast isOpen={toast !== null} message={toast ?? ''} duration={2200} onDidDismiss={() => setToast(null)} />
        </IonPage>
    );
};

export default OfficesPage;
