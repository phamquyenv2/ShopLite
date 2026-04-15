import {
    IonBackButton,
    IonButton,
    IonButtons,
    IonContent,
    IonHeader,
    IonInput,
    IonItem,
    IonList,
    IonPage,
    IonTitle,
    IonToast,
    IonToolbar,
} from '@ionic/react';
import { useEffect, useMemo, useState } from 'react';
import { useHistory, useParams } from 'react-router-dom';
import { ApiError, authApis, endpoints } from '../utils/Apis';
import type { Customer, CustomerUpsert } from '../api/types';

const toNumber = (v: unknown): number | null => {
    const n = typeof v === 'number' ? v : Number(String(v ?? ''));
    return Number.isFinite(n) ? n : null;
};

const CustomerUpsertPage: React.FC = () => {
    const { id } = useParams<{ id?: string }>();
    const history = useHistory();

    const editingId = useMemo(() => {
        const n = toNumber(id);
        return n !== null ? n : null;
    }, [id]);

    const [busy, setBusy] = useState(false);
    const [toast, setToast] = useState<string | null>(null);

    const [form, setForm] = useState<CustomerUpsert>({
        name: '',
        phone: '',
    });

    const loadCustomer = async (customerId: number) => {
        setBusy(true);
        try {
            const res = await authApis().get<Customer>(endpoints['customer-detail'](customerId));
            const c = res.data;
            if (!c) throw new ApiError('Customer not found', { status: 404, data: null, headers: res.headers });
            setForm({ name: c.name ?? '', phone: c.phone ?? '' });
        } catch (err) {
            setToast(err instanceof ApiError ? err.message : 'Failed to load customer');
        } finally {
            setBusy(false);
        }
    };

    useEffect(() => {
        if (editingId) void loadCustomer(editingId);
    }, [editingId]);

    const onSave = async () => {
        if (!form.name.trim()) {
            setToast('Name is required');
            return;
        }
        if (!form.phone.trim()) {
            setToast('Phone is required');
            return;
        }

        setBusy(true);
        try {
            const payload: CustomerUpsert = {
                name: form.name.trim(),
                phone: form.phone.trim(),
            };

            if (editingId) {
                await authApis().put(endpoints['customer-detail'](editingId), payload);
            } else {
                await authApis().post(endpoints.customers, payload);
            }
            history.replace('/customers');
        } catch (err) {
            setToast(err instanceof ApiError ? err.message : 'Save failed');
        } finally {
            setBusy(false);
        }
    };

    const onDelete = async () => {
        if (!editingId) return;
        setBusy(true);
        try {
            await authApis().delete(endpoints['customer-detail'](editingId));
            history.replace('/customers');
        } catch (err) {
            setToast(err instanceof ApiError ? err.message : 'Delete failed');
        } finally {
            setBusy(false);
        }
    };

    return (
        <IonPage>
            <IonHeader>
                <IonToolbar>
                    <IonButtons slot="start">
                        <IonBackButton defaultHref="/customers" />
                    </IonButtons>
                    <IonTitle>{editingId ? `Edit Customer #${editingId}` : 'New Customer'}</IonTitle>
                </IonToolbar>
            </IonHeader>
            <IonContent>
                <IonList inset>
                    <IonItem>
                        <IonInput
                            label="Name"
                            labelPlacement="stacked"
                            value={form.name}
                            onIonInput={(e) => setForm((p) => ({ ...p, name: String(e.detail.value ?? '') }))}
                        />
                    </IonItem>
                    <IonItem>
                        <IonInput
                            label="Phone"
                            labelPlacement="stacked"
                            value={form.phone}
                            onIonInput={(e) => setForm((p) => ({ ...p, phone: String(e.detail.value ?? '') }))}
                            inputmode="tel"
                        />
                    </IonItem>
                </IonList>

                <div className="ion-padding">
                    <IonButton expand="block" onClick={() => void onSave()} disabled={busy}>
                        {busy ? 'Saving…' : 'Save'}
                    </IonButton>
                    {editingId && (
                        <IonButton expand="block" color="danger" fill="outline" onClick={() => void onDelete()} disabled={busy}>
                            Delete
                        </IonButton>
                    )}
                </div>

                <IonToast
                    isOpen={toast !== null}
                    message={toast ?? ''}
                    duration={2500}
                    onDidDismiss={() => setToast(null)}
                />
            </IonContent>
        </IonPage>
    );
};

export default CustomerUpsertPage;
