import {
    IonButton,
    IonContent,
    IonHeader,
    IonIcon,
    IonInput,
    IonItem,
    IonBackButton,
    IonButtons,
    IonPage,
    IonText,
    IonTitle,
    IonToolbar,
    IonToast,
} from '@ionic/react';
import {
    arrowForwardOutline,
    callOutline,
    eyeOffOutline,
    eyeOutline,
    lockClosedOutline,
    personOutline,
    storefront,
} from 'ionicons/icons';
import { useMemo, useState } from 'react';
import { useHistory, useLocation } from 'react-router-dom';
import { ApiError } from '../utils/Apis';
import { useAuth } from '../auth/useAuth';

import './Register.css';

type LocationState = { from?: { pathname?: string } };

type TouchedState = {
    username: boolean;
    phone: boolean;
    password: boolean;
    confirmPassword: boolean;
};

const normalizePhone = (value: string): string => value.replace(/[\s\-_.()]+/g, '');

const validateLocal = (args: {
    username: string;
    phone: string;
    password: string;
    confirmPassword: string;
}) => {
    const username = args.username.trim();
    const phone = normalizePhone(args.phone.trim());
    const password = args.password;
    const confirmPassword = args.confirmPassword;

    const errors: Record<keyof TouchedState, string | null> = {
        username: null,
        phone: null,
        password: null,
        confirmPassword: null,
    };

    if (!username) errors.username = 'Vui lòng nhập tên đăng nhập';

    if (!phone) {
        errors.phone = 'Vui lòng nhập số điện thoại';
    } else if (!/^0\d{9}$/.test(phone)) {
        errors.phone = 'Số điện thoại phải gồm 10 chữ số và bắt đầu bằng 0';
    }

    if (!password) {
        errors.password = 'Vui lòng nhập mật khẩu';
    } else if (password.length < 6) {
        errors.password = 'Mật khẩu phải có ít nhất 6 ký tự';
    }

    if (!confirmPassword) errors.confirmPassword = 'Vui lòng nhập lại mật khẩu';
    if (password && confirmPassword && confirmPassword !== password) {
        errors.confirmPassword = 'Mật khẩu nhập lại không khớp';
    }

    const isValid = Object.values(errors).every((v) => v === null);
    return { errors, isValid };
};

const isRecord = (v: unknown): v is Record<string, unknown> => typeof v === 'object' && v !== null;

const parseApiFieldErrors = (err: ApiError): Partial<Record<keyof TouchedState, string>> => {
    const data = err.response?.data;
    const out: Partial<Record<keyof TouchedState, string>> = {};

    if (isRecord(data)) {
        const rawErrors = data.errors;
        if (Array.isArray(rawErrors)) {
            for (const item of rawErrors) {
                if (!isRecord(item)) continue;
                const field = item.field;
                const message = item.message;
                if (typeof message !== 'string' || !message) continue;
                if (field === 'username') out.username = message;
                if (field === 'phone') out.phone = message;
                if (field === 'password') out.password = message;
            }
            return out;
        }
    }

    const msg = String(err.message || '');
    if (/\busername\b/i.test(msg)) out.username = msg;
    else if (/\bphone\b/i.test(msg)) out.phone = msg;
    else if (/\bpassword\b/i.test(msg)) out.password = msg;
    return out;
};

const Register: React.FC = () => {
    const { register } = useAuth();
    const history = useHistory();
    const location = useLocation<LocationState>();
    const [username, setUsername] = useState('');
    const [phone, setPhone] = useState('');
    const [password, setPassword] = useState('');
    const [confirmPassword, setConfirmPassword] = useState('');
    const [showPassword, setShowPassword] = useState(false);
    const [showConfirmPassword, setShowConfirmPassword] = useState(false);
    const [busy, setBusy] = useState(false);
    const [submitted, setSubmitted] = useState(false);
    const [touched, setTouched] = useState<TouchedState>({
        username: false,
        phone: false,
        password: false,
        confirmPassword: false,
    });
    const [apiErrors, setApiErrors] = useState<Partial<Record<keyof TouchedState, string>>>({});
    const [toast, setToast] = useState<string | null>(null);

    const { errors: localErrors, isValid: isValidLocal } = useMemo(
        () =>
            validateLocal({
                username,
                phone,
                password,
                confirmPassword,
            }),
        [username, phone, password, confirmPassword]
    );

    const shouldShow = (field: keyof TouchedState) => submitted || touched[field];

    const getFieldError = (field: keyof TouchedState): string | null => {
        return localErrors[field] ?? apiErrors[field] ?? null;
    };

    const redirectTo = useMemo(() => {
        const from = location.state?.from?.pathname;
        return typeof from === 'string' && from.startsWith('/') ? from : '/home';
    }, [location.state]);

    const onSubmit = async () => {
        setSubmitted(true);
        setTouched({ username: true, phone: true, password: true, confirmPassword: true });
        if (!isValidLocal) {
            setToast('Vui lòng kiểm tra lại thông tin');
            return;
        }

        setBusy(true);
        setApiErrors({});
        try {
            await register(username.trim(), normalizePhone(phone.trim()), password);
            history.replace(redirectTo);
        } catch (err) {
            if (err instanceof ApiError) {
                const fieldErrors = parseApiFieldErrors(err);
                if (Object.keys(fieldErrors).length) {
                    setApiErrors(fieldErrors);
                }
                setToast(err.message);
            } else {
                setToast('Đăng ký thất bại');
            }
        } finally {
            setBusy(false);
        }
    };

    return (
        <IonPage>
            <IonHeader className="register-header">
                <IonToolbar className="register-toolbar">
                    <IonButtons slot="start">
                        <IonBackButton defaultHref="/login" text="" />
                    </IonButtons>
                    <IonTitle slot="start" className="register-toolbar-title">
                        Đăng ký tài khoản
                    </IonTitle>
                </IonToolbar>
            </IonHeader>

            <IonContent className="register-content" fullscreen>
                <div className="register-shell">
                    <div className="register-hero">
                        <div className="register-appIcon" aria-hidden="true">
                            <IonIcon icon={storefront} />
                        </div>

                        <h1 className="register-title">Đăng ký tài khoản</h1>
                        <p className="register-subtitle">Quản lý kinh doanh tinh gọn</p>
                    </div>

                    <div className="register-form" role="form" aria-label="Register">
                        <div className="register-field">
                            <IonItem
                                className={`register-input-item${shouldShow('username') && getFieldError('username') ? ' register-input-item--error' : ''}`}
                                lines="none"
                            >
                                <IonIcon icon={personOutline} slot="start" className="register-icon-muted" />
                                <IonInput
                                    value={username}
                                    onIonInput={(e) => {
                                        setUsername(String(e.detail.value ?? ''));
                                        setTouched((p) => ({ ...p, username: true }));
                                        setApiErrors((p) => ({ ...p, username: undefined }));
                                    }}
                                    onIonChange={(e) => {
                                        setUsername(String(e.detail.value ?? ''));
                                        setTouched((p) => ({ ...p, username: true }));
                                        setApiErrors((p) => ({ ...p, username: undefined }));
                                    }}
                                    onIonBlur={() => setTouched((p) => ({ ...p, username: true }))}
                                    placeholder="Tên đăng nhập"
                                    autocomplete="username"
                                    inputmode="text"
                                    disabled={busy}
                                    aria-invalid={shouldShow('username') && !!getFieldError('username')}
                                />
                            </IonItem>
                            {shouldShow('username') && getFieldError('username') && (
                                <IonText className="register-error" color="danger">
                                    {getFieldError('username')}
                                </IonText>
                            )}
                        </div>

                        <div className="register-field">
                            <IonItem
                                className={`register-input-item${shouldShow('phone') && getFieldError('phone') ? ' register-input-item--error' : ''}`}
                                lines="none"
                            >
                                <IonIcon icon={callOutline} slot="start" className="register-icon-muted" />
                                <IonInput
                                    value={phone}
                                    onIonInput={(e) => {
                                        setPhone(String(e.detail.value ?? ''));
                                        setTouched((p) => ({ ...p, phone: true }));
                                        setApiErrors((p) => ({ ...p, phone: undefined }));
                                    }}
                                    onIonChange={(e) => {
                                        setPhone(String(e.detail.value ?? ''));
                                        setTouched((p) => ({ ...p, phone: true }));
                                        setApiErrors((p) => ({ ...p, phone: undefined }));
                                    }}
                                    onIonBlur={() => setTouched((p) => ({ ...p, phone: true }))}
                                    placeholder="Số điện thoại"
                                    autocomplete="tel"
                                    inputmode="tel"
                                    disabled={busy}
                                    aria-invalid={shouldShow('phone') && !!getFieldError('phone')}
                                />
                            </IonItem>
                            {shouldShow('phone') && getFieldError('phone') && (
                                <IonText className="register-error" color="danger">
                                    {getFieldError('phone')}
                                </IonText>
                            )}
                        </div>

                        <div className="register-field">
                            <IonItem
                                className={`register-input-item${shouldShow('password') && getFieldError('password') ? ' register-input-item--error' : ''}`}
                                lines="none"
                            >
                                <IonIcon icon={lockClosedOutline} slot="start" className="register-icon-muted" />
                                <IonInput
                                    value={password}
                                    onIonInput={(e) => {
                                        setPassword(String(e.detail.value ?? ''));
                                        setTouched((p) => ({ ...p, password: true }));
                                        setApiErrors((p) => ({ ...p, password: undefined }));
                                    }}
                                    onIonChange={(e) => {
                                        setPassword(String(e.detail.value ?? ''));
                                        setTouched((p) => ({ ...p, password: true }));
                                        setApiErrors((p) => ({ ...p, password: undefined }));
                                    }}
                                    onIonBlur={() => setTouched((p) => ({ ...p, password: true }))}
                                    placeholder="Mật khẩu"
                                    type={showPassword ? 'text' : 'password'}
                                    disabled={busy}
                                    aria-invalid={shouldShow('password') && !!getFieldError('password')}
                                />
                                <button
                                    type="button"
                                    className="register-eye-btn"
                                    onClick={() => setShowPassword((v) => !v)}
                                    disabled={busy}
                                    aria-label={showPassword ? 'Ẩn mật khẩu' : 'Hiện mật khẩu'}
                                >
                                    <IonIcon icon={showPassword ? eyeOffOutline : eyeOutline} />
                                </button>
                            </IonItem>
                            {shouldShow('password') && getFieldError('password') && (
                                <IonText className="register-error" color="danger">
                                    {getFieldError('password')}
                                </IonText>
                            )}
                        </div>

                        <div className="register-field">
                            <IonItem
                                className={`register-input-item${shouldShow('confirmPassword') && getFieldError('confirmPassword') ? ' register-input-item--error' : ''}`}
                                lines="none"
                            >
                                <IonIcon icon={lockClosedOutline} slot="start" className="register-icon-muted" />
                                <IonInput
                                    value={confirmPassword}
                                    onIonInput={(e) => {
                                        setConfirmPassword(String(e.detail.value ?? ''));
                                        setTouched((p) => ({ ...p, confirmPassword: true }));
                                    }}
                                    onIonChange={(e) => {
                                        setConfirmPassword(String(e.detail.value ?? ''));
                                        setTouched((p) => ({ ...p, confirmPassword: true }));
                                    }}
                                    onIonBlur={() => setTouched((p) => ({ ...p, confirmPassword: true }))}
                                    placeholder="Nhập lại mật khẩu"
                                    type={showConfirmPassword ? 'text' : 'password'}
                                    onKeyDown={(e) => e.key === 'Enter' && void onSubmit()}
                                    disabled={busy}
                                    aria-invalid={shouldShow('confirmPassword') && !!getFieldError('confirmPassword')}
                                />
                                <button
                                    type="button"
                                    className="register-eye-btn"
                                    onClick={() => setShowConfirmPassword((v) => !v)}
                                    disabled={busy}
                                    aria-label={showConfirmPassword ? 'Ẩn mật khẩu' : 'Hiện mật khẩu'}
                                >
                                    <IonIcon icon={showConfirmPassword ? eyeOffOutline : eyeOutline} />
                                </button>
                            </IonItem>
                            {shouldShow('confirmPassword') && getFieldError('confirmPassword') && (
                                <IonText className="register-error" color="danger">
                                    {getFieldError('confirmPassword')}
                                </IonText>
                            )}
                        </div>

                        <IonButton
                            className="register-submit-btn"
                            expand="block"
                            onClick={onSubmit}
                        >
                            {busy ? 'Đang đăng ký…' : 'Đăng ký'}
                            <IonIcon icon={arrowForwardOutline} slot="end" />
                        </IonButton>


                        <div className="register-footer">
                            Đã có tài khoản?{' '}
                            <button
                                type="button"
                                className="register-footer-link"
                                onClick={() => history.replace('/login')}
                                disabled={busy}
                            >
                                Đăng nhập ngay
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

export default Register;
