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
import { ApiError, authApis, endpoints } from '../utils/Apis';
import type { Order } from '../api/types';

const Orders: React.FC = () => {
    const [items, setItems] = useState<Order[]>([]);
    const [busy, setBusy] = useState(false);
    const [toast, setToast] = useState<string | null>(null);

    const load = async () => {
        setBusy(true);
        try {
            const res = await authApis().get<Order[]>(endpoints.orders);
            setItems(Array.isArray(res.data) ? res.data : []);
        } catch (err) {
            setItems([]);
            setToast(err instanceof ApiError ? err.message : 'Failed to load orders');
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
                    <IonTitle>Orders</IonTitle>
                </IonToolbar>
            </IonHeader>
            <IonContent>
                <div className="ion-padding">
                    <IonButton routerLink="/orders/new" expand="block">
                        Create order
                    </IonButton>
                    <IonButton fill="clear" onClick={() => void load()} disabled={busy}>
                        {busy ? 'Refreshing…' : 'Refresh'}
                    </IonButton>
                </div>

                <IonList inset>
                    {items.map((o) => (
                        <IonItem key={o.id} routerLink={`/orders/${o.id}`} button>
                            <IonLabel>
                                <h2>{o.code ?? `Order #${o.id}`}</h2>
                                <p>{o.customerName ?? `Customer #${o.customerId}`}</p>
                                <p>
                                    Total: {o.totalAmount ?? 0} • Status: {o.status ?? 'PENDING'}
                                </p>
                            </IonLabel>
                        </IonItem>
                    ))}

                    {items.length === 0 && !busy && (
                        <IonItem>
                            <IonLabel color="medium">No orders</IonLabel>
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

export default Orders;
