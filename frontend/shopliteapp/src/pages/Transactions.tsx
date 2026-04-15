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
import type { Transaction } from '../api/types';

const Transactions: React.FC = () => {
    const [items, setItems] = useState<Transaction[]>([]);
    const [busy, setBusy] = useState(false);
    const [toast, setToast] = useState<string | null>(null);

    const load = async () => {
        setBusy(true);
        try {
            const res = await authApis().get<Transaction[]>(endpoints.transactions);
            setItems(Array.isArray(res.data) ? res.data : []);
        } catch (err) {
            setItems([]);
            setToast(err instanceof ApiError ? err.message : 'Failed to load transactions');
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
                    <IonTitle>Transactions</IonTitle>
                </IonToolbar>
            </IonHeader>
            <IonContent>
                <div className="ion-padding">
                    <IonButton routerLink="/transactions/new" expand="block">
                        Add transaction
                    </IonButton>
                    <IonButton fill="clear" onClick={() => void load()} disabled={busy}>
                        {busy ? 'Refreshing…' : 'Refresh'}
                    </IonButton>
                </div>

                <IonList inset>
                    {items.map((t) => (
                        <IonItem key={t.id}>
                            <IonLabel>
                                <h2>
                                    {t.type} • {t.amount}
                                </h2>
                                <p>{t.content ?? ''}</p>
                                {t.orderCode && <p>Order: {t.orderCode}</p>}
                            </IonLabel>
                        </IonItem>
                    ))}

                    {items.length === 0 && !busy && (
                        <IonItem>
                            <IonLabel color="medium">No transactions</IonLabel>
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

export default Transactions;
