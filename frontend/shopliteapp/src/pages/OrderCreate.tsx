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
import { useEffect, useMemo, useState } from 'react';
import { useHistory } from 'react-router-dom';
import { ApiError, Apis, authApis, endpoints } from '../utils/Apis';
import { useAuth } from '../auth/useAuth';
import type { Customer, OrderItemUpsert, OrderUpsert, Product, ProductPage } from '../api/types';

const toNumber = (v: unknown): number => {
    const n = typeof v === 'number' ? v : Number(String(v ?? ''));
    return Number.isFinite(n) ? n : 0;
};

const OrderCreatePage: React.FC = () => {
    const history = useHistory();
    const { user } = useAuth();

    const [busy, setBusy] = useState(false);
    const [toast, setToast] = useState<string | null>(null);

    const [customers, setCustomers] = useState<Customer[]>([]);
    const [products, setProducts] = useState<Product[]>([]);

    const [customerId, setCustomerId] = useState<number>(0);
    const [discount, setDiscount] = useState<number>(0);

    const [items, setItems] = useState<OrderItemUpsert[]>([]);

    const total = useMemo(() => {
        const sum = items.reduce((acc, it) => acc + (toNumber(it.quantity) * toNumber(it.price)), 0);
        return Math.max(0, sum - (toNumber(discount) || 0));
    }, [items, discount]);

    const loadLookups = async () => {
        try {
            const [custRes, prodRes] = await Promise.all([
                authApis().get<Customer[]>(endpoints.customers),
                Apis.get<ProductPage>(`${endpoints.products}?page=0&size=200&sortBy=createdAt&sortDir=desc`),
            ]);
            const custs = Array.isArray(custRes.data) ? custRes.data : [];
            const prods = Array.isArray(prodRes.data?.data) ? prodRes.data!.data : [];

            setCustomers(custs);
            setProducts(prods);
            setCustomerId(custs[0]?.id ?? 0);
        } catch (err) {
            setToast(err instanceof ApiError ? err.message : 'Failed to load lookups');
        }
    };

    useEffect(() => {
        void loadLookups();
    }, []);

    const addItem = () => {
        const first = products[0];
        if (!first) {
            setToast('No products available');
            return;
        }
        setItems((prev) => [
            ...prev,
            {
                productId: first.id,
                quantity: 1,
                price: first.sellingPrice ?? 0,
            },
        ]);
    };

    const updateItem = (idx: number, patch: Partial<OrderItemUpsert>) => {
        setItems((prev) => prev.map((it, i) => (i === idx ? { ...it, ...patch } : it)));
    };

    const removeItem = (idx: number) => {
        setItems((prev) => prev.filter((_, i) => i !== idx));
    };

    const onSubmit = async () => {
        if (!user?.id) {
            setToast('Missing userId');
            return;
        }
        if (!customerId) {
            setToast('Customer is required');
            return;
        }
        if (items.length === 0) {
            setToast('Add at least one item');
            return;
        }

        setBusy(true);
        try {
            const payload: OrderUpsert = {
                userId: user.id,
                customerId,
                discount: toNumber(discount) || 0,
                items: items.map((it) => ({
                    productId: toNumber(it.productId),
                    quantity: toNumber(it.quantity) || 1,
                    price: toNumber(it.price) || 0,
                })),
            };

            await authApis().post(endpoints.orders, payload);
            history.replace('/orders');
        } catch (err) {
            setToast(err instanceof ApiError ? err.message : 'Create order failed');
        } finally {
            setBusy(false);
        }
    };

    return (
        <IonPage>
            <IonHeader>
                <IonToolbar>
                    <IonButtons slot="start">
                        <IonBackButton defaultHref="/orders" />
                    </IonButtons>
                    <IonTitle>New Order</IonTitle>
                </IonToolbar>
            </IonHeader>
            <IonContent>
                <IonList inset>
                    <IonItem>
                        <IonLabel>Customer</IonLabel>
                        <IonSelect value={customerId || undefined} onIonChange={(e) => setCustomerId(Number(e.detail.value))}>
                            {customers.map((c) => (
                                <IonSelectOption key={c.id} value={c.id}>
                                    {c.name} • {c.phone}
                                </IonSelectOption>
                            ))}
                        </IonSelect>
                    </IonItem>

                    <IonItem>
                        <IonInput
                            label="Discount"
                            labelPlacement="stacked"
                            inputmode="decimal"
                            value={discount}
                            onIonInput={(e) => setDiscount(toNumber(e.detail.value))}
                        />
                    </IonItem>

                    <IonItem>
                        <IonLabel>
                            <strong>Total:</strong> {total}
                        </IonLabel>
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
                                        onIonChange={(e) => {
                                            const productId = Number(e.detail.value);
                                            const p = products.find((x) => x.id === productId);
                                            updateItem(idx, { productId, price: p?.sellingPrice ?? it.price });
                                        }}
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
                                        label="Quantity"
                                        labelPlacement="stacked"
                                        inputmode="numeric"
                                        value={it.quantity}
                                        onIonInput={(e) => updateItem(idx, { quantity: toNumber(e.detail.value) || 1 })}
                                    />
                                </IonItem>

                                <IonItem lines="none">
                                    <IonInput
                                        label="Price"
                                        labelPlacement="stacked"
                                        inputmode="decimal"
                                        value={it.price}
                                        onIonInput={(e) => updateItem(idx, { price: toNumber(e.detail.value) || 0 })}
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
                        {busy ? 'Creating…' : 'Create order'}
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

export default OrderCreatePage;
