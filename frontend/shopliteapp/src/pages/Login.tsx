import {
    IonButton,
    IonContent,
    IonHeader,
    IonInput,
    IonItem,
    IonList,
    IonPage,
    IonText,
    IonTitle,
    IonToast,
    IonToolbar,
} from '@ionic/react';
import { useMemo, useState } from 'react';
import { useHistory, useLocation } from 'react-router-dom';
import { ApiError } from '../utils/Apis';
import { useAuth } from '../auth/useAuth';

type LocationState = { from?: { pathname?: string } };

const Login: React.FC = () => {
    const { status, login } = useAuth();
    const history = useHistory();
    const location = useLocation<LocationState>();

    const [username, setUsername] = useState('');
    const [password, setPassword] = useState('');
    const [busy, setBusy] = useState(false);
    const [toast, setToast] = useState<string | null>(null);

    const redirectTo = useMemo(() => {
        const from = location.state?.from?.pathname;
        return typeof from === 'string' && from.startsWith('/') ? from : '/home';
    }, [location.state]);

    const onSubmit = async () => {
        if (!username.trim() || !password) {
            setToast('Please enter username and password');
            return;
        }
        setBusy(true);
        try {
            await login(username.trim(), password);
            history.replace(redirectTo);
        } catch (err) {
            if (err instanceof ApiError) {
                setToast(err.message);
            } else {
                setToast('Login failed');
            }
        } finally {
            setBusy(false);
        }
    };

    return (
        <IonPage>
            <IonHeader>
                <IonToolbar>
                    <IonTitle>ShopLite • Sign in</IonTitle>
                </IonToolbar>
            </IonHeader>
            <IonContent className="ion-padding">
                <IonText color="medium">
                    {status === 'checking'
                        ? 'Checking existing session...'
                        : 'Sign in to manage sales, customers, and inventory.'}
                </IonText>

                <IonList inset>
                    <IonItem>
                        <IonInput
                            label="Username"
                            labelPlacement="stacked"
                            value={username}
                            onIonInput={(e) => setUsername(String(e.detail.value ?? ''))}
                            autocomplete="username"
                        />
                    </IonItem>
                    <IonItem>
                        <IonInput
                            label="Password"
                            labelPlacement="stacked"
                            type="password"
                            value={password}
                            onIonInput={(e) => setPassword(String(e.detail.value ?? ''))}
                            autocomplete="current-password"
                        />
                    </IonItem>
                </IonList>

                <IonButton expand="block" onClick={onSubmit} disabled={busy}>
                    {busy ? 'Signing in…' : 'Sign in'}
                </IonButton>

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

export default Login;
