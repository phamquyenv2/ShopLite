import {
    IonBackButton,
    IonButton,
    IonButtons,
    IonContent,
    IonHeader,
    IonInput,
    IonItem,
    IonLabel,
    IonList,
    IonPage,
    IonSelect,
    IonSelectOption,
    IonTitle,
    IonToast,
    IonToolbar,
} from '@ionic/react';
import { useMemo, useState } from 'react';
import { useHistory } from 'react-router-dom';
import { ApiError, authApis, endpoints } from '../utils/Apis';
import type { TransactionType, TransactionUpsert } from '../api/types';

const toNumber = (v: unknown): number => {
    const n = typeof v === 'number' ? v : Number(String(v ?? ''));
    return Number.isFinite(n) ? n : 0;
};

const TYPES: TransactionType[] = ['REVENUE', 'EXPENSE', 'REFUND', 'SALARY'];

const TransactionCreatePage: React.FC = () => {
    const history = useHistory();

    const [busy, setBusy] = useState(false);
    const [toast, setToast] = useState<string | null>(null);

    const [form, setForm] = useState<TransactionUpsert>({
        amount: 0,
        type: 'REVENUE',
        content: '',
        orderId: null,
        bankCode: '',
        externalId: '',
        transactionTime: '',
    });

    const canSubmit = useMemo(() => form.amount > 0 && !!form.type, [form.amount, form.type]);

    const onSubmit = async () => {
        if (!canSubmit) {
            setToast('Amount and type are required');
            return;
        }

        setBusy(true);
        try {
            const payload: TransactionUpsert = {
                ...form,
                amount: toNumber(form.amount),
                content: form.content?.trim() || null,
                bankCode: form.bankCode?.trim() || null,
                externalId: form.externalId?.trim() || null,
                orderId: form.orderId ? toNumber(form.orderId) : null,
                transactionTime: form.transactionTime?.trim() || null,
            };

            await authApis().post(endpoints.transactions, payload);
            history.replace('/transactions');
        } catch (err) {
            setToast(err instanceof ApiError ? err.message : 'Create transaction failed');
        } finally {
            setBusy(false);
        }
    };

    return (
        <IonPage>
            <IonHeader>
                <IonToolbar>
                    <IonButtons slot="start">
                        <IonBackButton defaultHref="/transactions" />
                    </IonButtons>
                    <IonTitle>New Transaction</IonTitle>
                </IonToolbar>
            </IonHeader>
            <IonContent>
                <IonList inset>
                    <IonItem>
                        <IonInput
                            label="Amount"
                            labelPlacement="stacked"
                            inputmode="decimal"
                            value={form.amount}
                            onIonInput={(e) => setForm((p) => ({ ...p, amount: toNumber(e.detail.value) }))}
                        />
                    </IonItem>

                    <IonItem>
                        <IonLabel>Type</IonLabel>
                        <IonSelect
                            value={form.type}
                            onIonChange={(e) => setForm((p) => ({ ...p, type: String(e.detail.value) as TransactionType }))}
                        >
                            {TYPES.map((t) => (
                                <IonSelectOption key={t} value={t}>
                                    {t}
                                </IonSelectOption>
                            ))}
                        </IonSelect>
                    </IonItem>

                    <IonItem>
                        <IonInput
                            label="Content"
                            labelPlacement="stacked"
                            value={form.content ?? ''}
                            onIonInput={(e) => setForm((p) => ({ ...p, content: String(e.detail.value ?? '') }))}
                        />
                    </IonItem>

                    <IonItem>
                        <IonInput
                            label="Order ID (optional)"
                            labelPlacement="stacked"
                            inputmode="numeric"
                            value={form.orderId ?? ''}
                            onIonInput={(e) => setForm((p) => ({ ...p, orderId: toNumber(e.detail.value) || null }))}
                        />
                    </IonItem>

                    <IonItem>
                        <IonInput
                            label="Bank code (optional)"
                            labelPlacement="stacked"
                            value={form.bankCode ?? ''}
                            onIonInput={(e) => setForm((p) => ({ ...p, bankCode: String(e.detail.value ?? '') }))}
                        />
                    </IonItem>

                    <IonItem>
                        <IonInput
                            label="External ID (optional)"
                            labelPlacement="stacked"
                            value={form.externalId ?? ''}
                            onIonInput={(e) => setForm((p) => ({ ...p, externalId: String(e.detail.value ?? '') }))}
                        />
                    </IonItem>

                    <IonItem>
                        <IonInput
                            label="Transaction time (ISO, optional)"
                            labelPlacement="stacked"
                            placeholder="2026-04-15T10:30:00"
                            value={form.transactionTime ?? ''}
                            onIonInput={(e) => setForm((p) => ({ ...p, transactionTime: String(e.detail.value ?? '') }))}
                        />
                    </IonItem>
                </IonList>

                <div className="ion-padding">
                    <IonButton expand="block" onClick={() => void onSubmit()} disabled={busy}>
                        {busy ? 'Saving…' : 'Save'}
                    </IonButton>
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

export default TransactionCreatePage;
