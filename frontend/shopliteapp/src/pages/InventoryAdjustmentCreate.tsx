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
import { useEffect, useState } from 'react';
import { useHistory } from 'react-router-dom';
import { ApiError, Apis, authApis, endpoints } from '../utils/Apis';
import type {
    AdjustmentItemUpsert,
    InventoryAdjustmentUpsert,
    Product,
    ProductPage,
} from '../api/types';

const toNumber = (v: unknown): number => {
    const n = typeof v === 'number' ? v : Number(String(v ?? ''));
    return Number.isFinite(n) ? n : 0;
};

const InventoryAdjustmentCreatePage: React.FC = () => {
    const history = useHistory();

    const [busy, setBusy] = useState(false);
    const [toast, setToast] = useState<string | null>(null);

    const [products, setProducts] = useState<Product[]>([]);

    const [reason, setReason] = useState('');
    const [note, setNote] = useState('');
    const [createdBy, setCreatedBy] = useState('');
    const [items, setItems] = useState<AdjustmentItemUpsert[]>([]);

    const loadProducts = async () => {
        try {
            const res = await Apis.get<ProductPage>(`${endpoints.products}?page=0&size=200&sortBy=createdAt&sortDir=desc`);
            setProducts(Array.isArray(res.data?.data) ? res.data!.data : []);
        } catch (err) {
            setToast(err instanceof ApiError ? err.message : 'Failed to load products');
        }
    };

    useEffect(() => {
        void loadProducts();
    }, []);

    const addItem = () => {
        const first = products[0];
        if (!first) {
            setToast('No products available');
            return;
        }
        setItems((prev) => [...prev, { productId: first.id, actualQuantity: 0 }]);
    };

    const updateItem = (idx: number, patch: Partial<AdjustmentItemUpsert>) => {
        setItems((prev) => prev.map((it, i) => (i === idx ? { ...it, ...patch } : it)));
    };

    const removeItem = (idx: number) => {
        setItems((prev) => prev.filter((_, i) => i !== idx));
    };

    const onSubmit = async () => {
        if (!reason.trim()) {
            setToast('Reason is required');
            return;
        }
        if (!createdBy.trim()) {
            setToast('Created by is required');
            return;
        }
        if (items.length === 0) {
            setToast('Add at least one item');
            return;
        }

        setBusy(true);
        try {
            const payload: InventoryAdjustmentUpsert = {
                reason: reason.trim(),
                createdBy: createdBy.trim(),
                note: note.trim() || null,
                items: items.map((it) => ({
                    productId: toNumber(it.productId),
                    actualQuantity: toNumber(it.actualQuantity),
                })),
            };

            await authApis().post(endpoints['inventory-adjustments'], payload);
            history.replace('/inventory/adjustments');
        } catch (err) {
            setToast(err instanceof ApiError ? err.message : 'Create adjustment failed');
        } finally {
            setBusy(false);
        }
    };

    return (
        <IonPage>
            <IonHeader>
                <IonToolbar>
                    <IonButtons slot="start">
                        <IonBackButton defaultHref="/inventory/adjustments" />
                    </IonButtons>
                    <IonTitle>New Inventory Adjustment</IonTitle>
                </IonToolbar>
            </IonHeader>
            <IonContent>
                <IonList inset>
                    <IonItem>
                        <IonInput
                            label="Reason"
                            labelPlacement="stacked"
                            value={reason}
                            onIonInput={(e) => setReason(String(e.detail.value ?? ''))}
                        />
                    </IonItem>
                    <IonItem>
                        <IonInput
                            label="Created by"
                            labelPlacement="stacked"
                            value={createdBy}
                            onIonInput={(e) => setCreatedBy(String(e.detail.value ?? ''))}
                        />
                    </IonItem>
                    <IonItem>
                        <IonInput
                            label="Note (optional)"
                            labelPlacement="stacked"
                            value={note}
                            onIonInput={(e) => setNote(String(e.detail.value ?? ''))}
                        />
                    </IonItem>
                </IonList>

                <div className="ion-padding">
                    <IonButton expand="block" onClick={addItem} disabled={products.length === 0}>
                        Add item
                    </IonButton>
                </div>

                <IonList inset>
                    {items.map((it, idx) => (
                        <IonItem key={idx}>
                            <div style={{ width: '100%' }}>
                                <IonItem lines="none">
                                    <IonLabel>Product</IonLabel>
                                    <IonSelect
                                        value={it.productId}
                                        onIonChange={(e) => updateItem(idx, { productId: Number(e.detail.value) })}
                                    >
                                        {products.map((p) => (
                                            <IonSelectOption key={p.id} value={p.id}>
                                                {p.name}
                                            </IonSelectOption>
                                        ))}
                                    </IonSelect>
                                </IonItem>

                                <IonItem lines="none">
                                    <IonInput
                                        label="Actual quantity"
                                        labelPlacement="stacked"
                                        inputmode="numeric"
                                        value={it.actualQuantity}
                                        onIonInput={(e) => updateItem(idx, { actualQuantity: toNumber(e.detail.value) })}
                                    />
                                </IonItem>

                                <IonButton fill="clear" color="danger" onClick={() => removeItem(idx)}>
                                    Remove
                                </IonButton>
                            </div>
                        </IonItem>
                    ))}

                    {items.length === 0 && (
                        <IonItem>
                            <IonLabel color="medium">No items</IonLabel>
                        </IonItem>
                    )}
                </IonList>

                <div className="ion-padding">
                    <IonButton expand="block" onClick={() => void onSubmit()} disabled={busy}>
                        {busy ? 'Creating…' : 'Create adjustment'}
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

export default InventoryAdjustmentCreatePage;
