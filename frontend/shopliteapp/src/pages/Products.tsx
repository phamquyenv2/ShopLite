import {
    IonButton,
    IonContent,
    IonHeader,
    IonItem,
    IonLabel,
    IonList,
    IonPage,
    IonTitle,
    IonToast,
    IonToolbar,
} from '@ionic/react';
import { useEffect, useState } from 'react';
import { ApiError, Apis, endpoints } from '../utils/Apis';
import type { Product, ProductPage } from '../api/types';

const Products: React.FC = () => {
    const [items, setItems] = useState<Product[]>([]);
    const [busy, setBusy] = useState(false);
    const [toast, setToast] = useState<string | null>(null);

    const load = async () => {
        setBusy(true);
        try {
            const query = new URLSearchParams({
                page: '0',
                size: '50',
                sortBy: 'createdAt',
                sortDir: 'desc',
            });
            const res = await Apis.get<ProductPage>(`${endpoints.products}?${query.toString()}`);
            setItems(Array.isArray(res.data?.data) ? res.data!.data : []);
        } catch (err) {
            setItems([]);
            setToast(err instanceof ApiError ? err.message : 'Failed to load products');
        } finally {
            setBusy(false);
        }
    };

    useEffect(() => {
        void load();
    }, []);

    return (
        <IonPage>
            <IonHeader>
                <IonToolbar>
                    <IonTitle>Products</IonTitle>
                </IonToolbar>
            </IonHeader>
            <IonContent>
                <div className="ion-padding">
                    <IonButton routerLink="/products/new" expand="block">
                        Add product
                    </IonButton>
                    <IonButton fill="clear" onClick={() => void load()} disabled={busy}>
                        {busy ? 'Refreshing…' : 'Refresh'}
                    </IonButton>
                </div>

                <IonList inset>
                    {items.map((p) => (
                        <IonItem key={p.id} routerLink={`/products/${p.id}/edit`} button>
                            <IonLabel>
                                <h2>{p.name}</h2>
                                <p>
                                    {p.categoryName ?? `Category #${p.categoryId}`} • {p.unitName ?? `Unit #${p.unitId}`}
                                </p>
                                <p>
                                    Stock: {p.stock} • Selling: {p.sellingPrice}
                                </p>
                            </IonLabel>
                        </IonItem>
                    ))}

                    {items.length === 0 && !busy && (
                        <IonItem>
                            <IonLabel color="medium">No products</IonLabel>
                        </IonItem>
                    )}
                </IonList>

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

export default Products;
