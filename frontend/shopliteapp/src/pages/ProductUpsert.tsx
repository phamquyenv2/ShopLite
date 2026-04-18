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
import { useHistory, useParams } from 'react-router-dom';
import { ApiError, Apis, authApis, endpoints } from '../utils/Apis';
import type { Category, Product, ProductUpsert, Unit } from '../api/types';

const toNumber = (v: unknown): number | null => {
    const n = typeof v === 'number' ? v : Number(String(v ?? ''));
    return Number.isFinite(n) ? n : null;
};

const ProductUpsertPage: React.FC = () => {
    const { id } = useParams<{ id?: string }>();
    const history = useHistory();
    const editingId = useMemo(() => {
        const n = toNumber(id);
        return n !== null ? n : null;
    }, [id]);

    const [busy, setBusy] = useState(false);
    const [toast, setToast] = useState<string | null>(null);

    const [categories, setCategories] = useState<Category[]>([]);
    const [units, setUnits] = useState<Unit[]>([]);

    const [form, setForm] = useState<ProductUpsert>({
        name: '',
        sku: '',
        barcode: null,
        image: null,
        categoryId: 0,
        unitId: 0,
        stock: 0,
        sellingPrice: 0,
        costPrice: 0,
        minStock: null,
        maxStock: null,
        status: 'ACTIVE',
        version: null,
    });

    const loadLookups = async () => {
        try {
            const [cats, unitRes] = await Promise.all([
                Apis.get<Category[]>(endpoints.categories),
                authApis().get<Unit[]>(endpoints.units),
            ]);
            setCategories(Array.isArray(cats.data) ? cats.data : []);
            setUnits(Array.isArray(unitRes.data) ? unitRes.data : []);

            setForm((prev) => ({
                ...prev,
                categoryId: prev.categoryId || (Array.isArray(cats.data) && cats.data[0]?.id) || 0,
                unitId: prev.unitId || (Array.isArray(unitRes.data) && unitRes.data[0]?.id) || 0,
            }));
        } catch (err) {
            setToast(err instanceof ApiError ? err.message : 'Failed to load lookups');
        }
    };

    const loadProduct = async (productId: number) => {
        setBusy(true);
        try {
            const res = await Apis.get<Product>(endpoints['product-detail'](productId));
            const p = res.data;
            if (!p) throw new ApiError('Product not found', { status: 404, data: null, headers: res.headers });
            setForm({
                name: p.name ?? '',
                sku: p.sku ?? '',
                barcode: p.barcode ?? null,
                image: p.image ?? null,
                categoryId: p.categoryId ?? 0,
                unitId: p.unitId ?? 0,
                stock: p.stock ?? 0,
                sellingPrice: p.sellingPrice ?? 0,
                costPrice: p.costPrice ?? 0,
                minStock: p.minStock ?? null,
                maxStock: p.maxStock ?? null,
                status: p.status ?? 'ACTIVE',
                version: p.version ?? null,
            });
        } catch (err) {
            setToast(err instanceof ApiError ? err.message : 'Failed to load product');
        } finally {
            setBusy(false);
        }
    };

    useEffect(() => {
        void loadLookups();
    }, []);

    useEffect(() => {
        if (editingId) void loadProduct(editingId);
    }, [editingId]);

    const onSave = async () => {
        if (!form.name.trim()) {
            setToast('Name is required');
            return;
        }
        if (!form.categoryId || !form.unitId) {
            setToast('Category and Unit are required');
            return;
        }

        setBusy(true);
        try {
            const payload: ProductUpsert = {
                ...form,
                name: form.name.trim(),
                sku: form.sku?.trim() || null,
                barcode: (form.barcode ?? '').trim() ? String(form.barcode).trim() : null,
                image: (form.image ?? '').trim() ? String(form.image).trim() : null,
                stock: Number(form.stock) || 0,
                sellingPrice: Number(form.sellingPrice) || 0,
                costPrice: Number(form.costPrice) || 0,
                minStock: form.minStock === null ? null : Number(form.minStock) || 0,
                maxStock: form.maxStock === null ? null : Number(form.maxStock) || 0,
            };

            if (editingId) {
                await Apis.put(endpoints['product-detail'](editingId), payload);
            } else {
                await Apis.post(endpoints.products, payload);
            }
            history.replace('/products');
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
            await Apis.delete(endpoints['product-detail'](editingId));
            history.replace('/products');
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
                        <IonBackButton defaultHref="/products" />
                    </IonButtons>
                    <IonTitle>{editingId ? `Edit Product #${editingId}` : 'New Product'}</IonTitle>
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
                            label="SKU"
                            labelPlacement="stacked"
                            value={form.sku ?? ''}
                            onIonInput={(e) => setForm((p) => ({ ...p, sku: String(e.detail.value ?? '') }))}
                        />
                    </IonItem>
                    <IonItem>
                        <IonInput
                            label="Barcode"
                            labelPlacement="stacked"
                            inputmode="text"
                            value={form.barcode ?? ''}
                            onIonInput={(e) => setForm((p) => ({ ...p, barcode: String(e.detail.value ?? '') }))}
                        />
                    </IonItem>

                    <IonItem>
                        <IonInput
                            label="Image URL"
                            labelPlacement="stacked"
                            inputmode="url"
                            value={form.image ?? ''}
                            onIonInput={(e) => setForm((p) => ({ ...p, image: String(e.detail.value ?? '') }))}
                        />
                    </IonItem>

                    <IonItem>
                        <IonLabel>Category</IonLabel>
                        <IonSelect
                            value={form.categoryId || undefined}
                            onIonChange={(e) => setForm((p) => ({ ...p, categoryId: Number(e.detail.value) }))}
                        >
                            {categories.map((c) => (
                                <IonSelectOption key={c.id} value={c.id}>
                                    {c.name}
                                </IonSelectOption>
                            ))}
                        </IonSelect>
                    </IonItem>

                    <IonItem>
                        <IonLabel>Unit</IonLabel>
                        <IonSelect
                            value={form.unitId || undefined}
                            onIonChange={(e) => setForm((p) => ({ ...p, unitId: Number(e.detail.value) }))}
                        >
                            {units.map((u) => (
                                <IonSelectOption key={u.id} value={u.id}>
                                    {u.name}
                                </IonSelectOption>
                            ))}
                        </IonSelect>
                    </IonItem>

                    <IonItem>
                        <IonInput
                            label="Stock"
                            labelPlacement="stacked"
                            inputmode="numeric"
                            value={form.stock}
                            disabled={Boolean(editingId)}
                            onIonInput={(e) => {
                                const n = toNumber(e.detail.value);
                                setForm((p) => ({ ...p, stock: n === null ? 0 : n }));
                            }}
                        />
                    </IonItem>

                    <IonItem>
                        <IonInput
                            label="Selling price"
                            labelPlacement="stacked"
                            inputmode="decimal"
                            value={form.sellingPrice}
                            onIonInput={(e) => {
                                const n = toNumber(e.detail.value);
                                setForm((p) => ({ ...p, sellingPrice: n === null ? 0 : n }));
                            }}
                        />
                    </IonItem>

                    <IonItem>
                        <IonInput
                            label="Cost price"
                            labelPlacement="stacked"
                            inputmode="decimal"
                            value={form.costPrice}
                            onIonInput={(e) => {
                                const n = toNumber(e.detail.value);
                                setForm((p) => ({ ...p, costPrice: n === null ? 0 : n }));
                            }}
                        />
                    </IonItem>

                    <IonItem>
                        <IonInput
                            label="Min stock"
                            labelPlacement="stacked"
                            inputmode="numeric"
                            value={form.minStock ?? ''}
                            onIonInput={(e) => setForm((p) => ({ ...p, minStock: toNumber(e.detail.value) }))}
                        />
                    </IonItem>

                    <IonItem>
                        <IonInput
                            label="Max stock"
                            labelPlacement="stacked"
                            inputmode="numeric"
                            value={form.maxStock ?? ''}
                            onIonInput={(e) => setForm((p) => ({ ...p, maxStock: toNumber(e.detail.value) }))}
                        />
                    </IonItem>

                    <IonItem>
                        <IonLabel>Status</IonLabel>
                        <IonSelect value={form.status ?? 'ACTIVE'} onIonChange={(e) => setForm((p) => ({ ...p, status: e.detail.value }))}>
                            <IonSelectOption value="ACTIVE">ACTIVE</IonSelectOption>
                            <IonSelectOption value="INACTIVE">INACTIVE</IonSelectOption>
                            <IonSelectOption value="OUT_OF_STOCK">OUT_OF_STOCK</IonSelectOption>
                        </IonSelect>
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

export default ProductUpsertPage;
