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
import type { InventoryAdjustment } from '../api/types';

const InventoryAdjustments: React.FC = () => {
    const [items, setItems] = useState<InventoryAdjustment[]>([]);
    const [busy, setBusy] = useState(false);
    const [toast, setToast] = useState<string | null>(null);

    const load = async () => {
        setBusy(true);
        try {
            const res = await authApis().get<InventoryAdjustment[]>(endpoints['inventory-adjustments']);
            setItems(Array.isArray(res.data) ? res.data : []);
        } catch (err) {
            setItems([]);
            setToast(err instanceof ApiError ? err.message : 'Failed to load inventory adjustments');
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
                    <IonTitle>Inventory Adjustments</IonTitle>
                </IonToolbar>
            </IonHeader>
            <IonContent>
                <div className="ion-padding">
                    <IonButton routerLink="/inventory/adjustments/new" expand="block">
                        Create adjustment
                    </IonButton>
                    <IonButton fill="clear" onClick={() => void load()} disabled={busy}>
                        {busy ? 'Refreshing…' : 'Refresh'}
                    </IonButton>
                </div>

                <IonList inset>
                    {items.map((a) => (
                        <IonItem key={a.id}>
                            <IonLabel>
                                <h2>#{a.id} • {a.reason}</h2>
                                <p>By: {a.createdBy}</p>
                                {a.note && <p>{a.note}</p>}
                            </IonLabel>
                        </IonItem>
                    ))}

                    {items.length === 0 && !busy && (
                        <IonItem>
                            <IonLabel color="medium">No adjustments</IonLabel>
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

export default InventoryAdjustments;
