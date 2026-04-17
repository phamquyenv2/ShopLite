import {
    IonButton,
    IonContent,
    IonInput,
    IonIcon,
    IonItem,
    IonList,
    IonPage,
    IonText,
    IonToast,
} from '@ionic/react';
import { useMemo, useState } from 'react';
import { useHistory, useLocation } from 'react-router-dom';
import {
    arrowForwardOutline,
    callOutline,
    personOutline,
    eyeOffOutline,
    eyeOutline,
    lockClosedOutline,
    logoApple,
    logoGoogle,
    storefront,
} from 'ionicons/icons';
import { ApiError } from '../utils/Apis';
import { useAuth } from '../auth/useAuth';

import './Login.css';

type LocationState = { from?: { pathname?: string } };

const Login: React.FC = () => {
    const { status, login } = useAuth();
    const history = useHistory();
    const location = useLocation<LocationState>();

    const [phone, setPhone] = useState('');
    const [password, setPassword] = useState('');
    const [showPassword, setShowPassword] = useState(false);
    const [busy, setBusy] = useState(false);
    const [toast, setToast] = useState<string | null>(null);

    const redirectTo = useMemo(() => {
        const from = location.state?.from?.pathname;
        return typeof from === 'string' && from.startsWith('/') ? from : '/home';
    }, [location.state]);

    const onSubmit = async () => {
        if (!phone.trim() || !password) {
            setToast('Vui lòng nhập số điện thoại và mật khẩu');
            return;
        }
        setBusy(true);
        try {
            await login(phone.trim(), password);
            history.replace(redirectTo);
        } catch (err) {
            setToast(err instanceof ApiError ? err.message : 'Đăng nhập thất bại');
        } finally {
            setBusy(false);
        }
    };

    return (
        <IonPage>
            <IonContent className="login-content" fullscreen>
                <div className="login-shell">
                    <div className="login-hero">
                        <div className="login-appIcon">
                            <IonIcon icon={storefront} />
                        </div>
                        <h1 className="login-title">Minimart</h1>
                        <p className="login-subtitle">Quản lý kinh doanh tinh gọn</p>
                    </div>

                    <div className="login-form">
                        <div className="login-label">Số điện thoại</div>
                        <IonItem className="login-input-item" lines="none">
                            <IonIcon icon={callOutline} slot="start" className="login-icon-muted" />
                            <IonInput
                                value={phone}
                                onIonInput={(e) => setPhone(String(e.detail.value ?? ''))}
                                placeholder="Nhập số điện thoại"
                                autocomplete="tel"
                                inputmode="tel"
                                disabled={busy}
                            />
                        </IonItem>

                        <div className="login-row">
                            <div className="login-label">Mật khẩu</div>
                            <button
                                type="button"
                                className="login-forgot"
                                onClick={() => setToast('Tính năng đang phát triển')}
                            >
                                Quên mật khẩu?
                            </button>
                        </div>

                        <IonItem className="login-input-item" lines="none">
                            <IonIcon icon={lockClosedOutline} slot="start" className="login-icon-muted" />
                            <IonInput
                                value={password}
                                onIonInput={(e) => setPassword(String(e.detail.value ?? ''))}
                                placeholder="Nhập mật khẩu"
                                type={showPassword ? 'text' : 'password'}
                                onKeyDown={(e) => e.key === 'Enter' && void onSubmit()}
                                disabled={busy}
                            />
                            <button
                                type="button"
                                className="login-eye-btn"
                                onClick={() => setShowPassword((v) => !v)}
                            >
                                <IonIcon icon={showPassword ? eyeOffOutline : eyeOutline} />
                            </button>
                        </IonItem>

                        <IonButton
                            className="login-submit-btn"
                            expand="block"
                            onClick={onSubmit}
                            disabled={busy}
                        >
                            {busy ? 'Đang đăng nhập…' : 'Đăng nhập'}
                            <IonIcon icon={arrowForwardOutline} slot="end" />
                        </IonButton>

                        <div className="login-signup-footer">
                            Chưa có tài khoản?{' '}
                            <button
                                type="button"
                                className="signup-link"
                                onClick={() => history.push('/register')}
                                disabled={busy}
                            >
                                Đăng ký ngay
                            </button>
                        </div>
                    </div>
                </div>

                <IonToast
                    isOpen={toast !== null}
                    message={toast ?? ''}
                    duration={2000}
                    onDidDismiss={() => setToast(null)}
                />
            </IonContent>
        </IonPage>
    );
};

export default Login;