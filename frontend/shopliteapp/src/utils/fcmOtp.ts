import { Capacitor } from '@capacitor/core';
import { PushNotifications, type Token } from '@capacitor/push-notifications';

export const FCM_OTP_EVENT = 'shoplite:fcm-otp';
export const FCM_OTP_TOKEN_STORAGE_KEY = 'shoplite:fcm-registration-token';

export type FcmOtpEventDetail = {
    otp: string;
    phone?: string;
    expiresIn?: number;
};

const PUSH_NOTIFICATIONS_ENABLED = import.meta.env.VITE_PUSH_NOTIFICATIONS_ENABLED === 'true';

export const emitFcmOtp = (detail: FcmOtpEventDetail): void => {
    try {
        sessionStorage.setItem('shoplite:last-fcm-otp', JSON.stringify(detail));
    } catch {
        // ignore
    }
    globalThis.dispatchEvent(new CustomEvent(FCM_OTP_EVENT, { detail }));
};

export const getCachedFcmToken = (): string | null => {
    try {
        return localStorage.getItem(FCM_OTP_TOKEN_STORAGE_KEY);
    } catch {
        return null;
    }
};

export const getFcmRegistrationToken = async (): Promise<string | null> => {
    if (!Capacitor.isNativePlatform() || !PUSH_NOTIFICATIONS_ENABLED) {
        return null;
    }

    const cached = getCachedFcmToken();
    if (cached) return cached;

    const permission = await PushNotifications.requestPermissions();
    if (permission.receive !== 'granted') {
        return null;
    }

    return new Promise<string | null>((resolve) => {
        let settled = false;
        let timeoutId: number | undefined;
        let registrationHandle: { remove: () => Promise<void> } | undefined;
        let errorHandle: { remove: () => Promise<void> } | undefined;

        const cleanup = () => {
            if (timeoutId !== undefined) window.clearTimeout(timeoutId);
            void registrationHandle?.remove();
            void errorHandle?.remove();
        };

        const finish = (token: string | null) => {
            if (settled) return;
            settled = true;
            cleanup();
            if (token) {
                try {
                    localStorage.setItem(FCM_OTP_TOKEN_STORAGE_KEY, token);
                } catch {
                    // ignore
                }
            }
            resolve(token);
        };

        timeoutId = window.setTimeout(() => finish(null), 10000);

        void PushNotifications.addListener('registration', (token: Token) => {
            finish(token.value);
        }).then((handle) => {
            registrationHandle = handle;
        });

        void PushNotifications.addListener('registrationError', (err) => {
            console.error('[FCM] Registration error:', err);
            finish(null);
        }).then((handle) => {
            errorHandle = handle;
        });

        void PushNotifications.register();
    });
};
