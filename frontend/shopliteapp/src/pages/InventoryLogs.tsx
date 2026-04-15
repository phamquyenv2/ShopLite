import {
    IonButton,
    IonContent,
    IonHeader,
    IonInput,
    IonItem,
    IonLabel,
    IonList,
    IonPage,
    IonTitle,
    IonToast,
    IonToolbar,
} from '@ionic/react';
import { useEffect, useMemo, useState } from 'react';
import { ApiError, authApis, endpoints } from '../utils/Apis';
import type { InventoryLog } from '../api/types';

const toNumber = (v: unknown): number | null => {
    const n = typeof v === 'number' ? v : Number(String(v ?? ''));
    return Number.isFinite(n) ? n : null;
};

const InventoryLogs: React.FC = () => {
    const [items, setItems] = useState<InventoryLog[]>([]);
    const [busy, setBusy] = useState(false);
    const [toast, setToast] = useState<string | null>(null);

    const [productIdText, setProductIdText] = useState('');
    const productId = useMemo(() => toNumber(productIdText), [productIdText]);

    const load = async () => {
        setBusy(true);
        try {
            const path = productId ? endpoints['inventory-logs-by-product'](productId) : endpoints['inventory-logs'];
            const res = await authApis().get<InventoryLog[]>(path);
            setItems(Array.isArray(res.data) ? res.data : []);
        } catch (err) {
            setItems([]);
            setToast(err instanceof ApiError ? err.message : 'Failed to load inventory logs');
        } finally {
            setBusy(false);
        }
    };

    useEffect(() => {
        void load();
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, []);

    return (
        <IonPage>
            <IonHeader>
                <IonToolbar>
                    <IonTitle>Inventory Logs</IonTitle>
                </IonToolbar>
            </IonHeader>
            <IonContent>
                <IonList inset>
                    <IonItem>
                        <IonInput
                            label="Filter by Product ID (optional)"
                            labelPlacement="stacked"
                            inputmode="numeric"
                            value={productIdText}
                            onIonInput={(e) => setProductIdText(String(e.detail.value ?? ''))}
                        />
                    </IonItem>
                </IonList>

                <div className="ion-padding">
                    <IonButton expand="block" onClick={() => void load()} disabled={busy}>
                        {busy ? 'Loading…' : 'Load logs'}
                    </IonButton>
                </div>

                <IonList inset>
                    {items.map((l) => (
                        <IonItem key={l.id}>
                            <IonLabel>
                                <h2>
                                    {l.type} • Product: {l.productName ?? `#${l.productId}`}
                                </h2>
                                <p>
                                    In: {l.quantityIn ?? 0} • Out: {l.quantityOut ?? 0} • Stock: {l.currentStock ?? ''}
                                </p>
                                <p>Balance after: {l.balanceAfter ?? ''}</p>
                            </IonLabel>
                        </IonItem>
                    ))}

                    {items.length === 0 && !busy && (
                        <IonItem>
                            <IonLabel color="medium">No logs</IonLabel>
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

export default InventoryLogs;
