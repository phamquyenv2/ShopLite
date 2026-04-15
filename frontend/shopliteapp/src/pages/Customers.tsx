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
import type { Customer } from '../api/types';

const Customers: React.FC = () => {
    const [items, setItems] = useState<Customer[]>([]);
    const [busy, setBusy] = useState(false);
    const [toast, setToast] = useState<string | null>(null);

    const load = async () => {
        setBusy(true);
        try {
            const res = await authApis().get<Customer[]>(endpoints.customers);
            setItems(Array.isArray(res.data) ? res.data : []);
        } catch (err) {
            setItems([]);
            setToast(err instanceof ApiError ? err.message : 'Failed to load customers');
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
                    <IonTitle>Customers</IonTitle>
                </IonToolbar>
            </IonHeader>
            <IonContent>
                <div className="ion-padding">
                    <IonButton routerLink="/customers/new" expand="block">
                        Add customer
                    </IonButton>
                    <IonButton fill="clear" onClick={() => void load()} disabled={busy}>
                        {busy ? 'Refreshing…' : 'Refresh'}
                    </IonButton>
                </div>

                <IonList inset>
                    {items.map((c) => (
                        <IonItem key={c.id} routerLink={`/customers/${c.id}/edit`} button>
                            <IonLabel>
                                <h2>{c.name}</h2>
                                <p>{c.phone}</p>
                                {typeof c.points === 'number' && <p>Points: {c.points}</p>}
                            </IonLabel>
                        </IonItem>
                    ))}

                    {items.length === 0 && !busy && (
                        <IonItem>
                            <IonLabel color="medium">No customers</IonLabel>
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

export default Customers;
