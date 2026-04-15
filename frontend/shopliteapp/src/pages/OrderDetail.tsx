import {
    IonBackButton,
    IonButtons,
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
import { useParams } from 'react-router-dom';
import { ApiError, authApis, endpoints } from '../utils/Apis';
import type { Order } from '../api/types';

const OrderDetailPage: React.FC = () => {
    const { id } = useParams<{ id: string }>();
    const orderId = Number(id);

    const [order, setOrder] = useState<Order | null>(null);
    const [toast, setToast] = useState<string | null>(null);

    useEffect(() => {
        const load = async () => {
            try {
                const res = await authApis().get<Order>(endpoints['order-detail'](orderId));
                setOrder(res.data ?? null);
            } catch (err) {
                setToast(err instanceof ApiError ? err.message : 'Failed to load order');
            }
        };
        if (Number.isFinite(orderId) && orderId > 0) void load();
    }, [orderId]);

    return (
        <IonPage>
            <IonHeader>
                <IonToolbar>
                    <IonButtons slot="start">
                        <IonBackButton defaultHref="/orders" />
                    </IonButtons>
                    <IonTitle>{order?.code ?? `Order #${orderId}`}</IonTitle>
                </IonToolbar>
            </IonHeader>
            <IonContent>
                <IonList inset>
                    <IonItem>
                        <IonLabel>
                            <h2>Customer</h2>
                            <p>{order?.customerName ?? `#${order?.customerId ?? ''}`}</p>
                        </IonLabel>
                    </IonItem>
                    <IonItem>
                        <IonLabel>
                            <h2>Status</h2>
                            <p>{order?.status ?? 'PENDING'}</p>
                        </IonLabel>
                    </IonItem>
                    <IonItem>
                        <IonLabel>
                            <h2>Total</h2>
                            <p>{order?.totalAmount ?? 0}</p>
                        </IonLabel>
                    </IonItem>
                    <IonItem>
                        <IonLabel>
                            <h2>Discount</h2>
                            <p>{order?.discount ?? 0}</p>
                        </IonLabel>
                    </IonItem>
                </IonList>

                <IonList inset>
                    {(order?.items ?? []).map((it) => (
                        <IonItem key={it.id}>
                            <IonLabel>
                                <h2>{it.productName ?? `Product #${it.productId}`}</h2>
                                <p>
                                    Qty: {it.quantity} • Price: {it.price} • Line: {it.totalPrice ?? (it.quantity * it.price)}
                                </p>
                            </IonLabel>
                        </IonItem>
                    ))}

                    {(order?.items?.length ?? 0) === 0 && (
                        <IonItem>
                            <IonLabel color="medium">No items</IonLabel>
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

export default OrderDetailPage;
