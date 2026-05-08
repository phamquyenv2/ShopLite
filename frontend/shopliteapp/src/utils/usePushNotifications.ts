import { useEffect } from 'react';
import { App } from '@capacitor/app';
import { Capacitor } from '@capacitor/core';
import { PushNotifications, type PushNotificationSchema } from '@capacitor/push-notifications';
import { useIonRouter } from '@ionic/react';
import { authApis, endpoints, getStoredUser } from './Apis';
import { emitFcmOtp, FCM_OTP_TOKEN_STORAGE_KEY } from './fcmOtp';
import { requestStorePermissionsRefresh } from './useStorePermissions';

const DEVICE_TYPE = Capacitor.getPlatform() === 'ios' ? 'IOS' : 'ANDROID';
const PUSH_NOTIFICATIONS_ENABLED = import.meta.env.VITE_PUSH_NOTIFICATIONS_ENABLED === 'true';

const getNotificationData = (notification: PushNotificationSchema): Record<string, string> =>
    (notification.data ?? {}) as Record<string, string>;

const handleRealtimeData = (data: Record<string, string> | undefined) => {
    if (data?.type === 'REGISTER_OTP_CODE' && data.otp) {
        emitFcmOtp({
            otp: data.otp,
            phone: data.phone,
            expiresIn: data.expires_in ? Number(data.expires_in) : undefined,
        });
        return;
    }

    if (data?.type === 'PERMISSIONS_CHANGED') {
        requestStorePermissionsRefresh();
    }
};

export function usePushNotifications() {
    const ionRouter = useIonRouter();

    useEffect(() => {
        let cancelled = false;
        const cleanupFns: Array<() => void> = [];
        const registerCleanup = (cleanup: () => void) => {
            if (cancelled) {
                cleanup();
            } else {
                cleanupFns.push(cleanup);
            }
        };
        const refreshIfActive = () => requestStorePermissionsRefresh();
        const refreshIfVisible = () => {
            if (document.visibilityState === 'visible') {
                requestStorePermissionsRefresh();
            }
        };

        window.addEventListener('focus', refreshIfActive);
        document.addEventListener('visibilitychange', refreshIfVisible);
        cleanupFns.push(() => window.removeEventListener('focus', refreshIfActive));
        cleanupFns.push(() => document.removeEventListener('visibilitychange', refreshIfVisible));

        void App.addListener('appStateChange', ({ isActive }) => {
            if (isActive) {
                requestStorePermissionsRefresh();
            }
        }).then(handle => {
            registerCleanup(() => void handle.remove());
        });

        // Push notification setup only runs on native Android/iOS builds when FCM is configured.
        if (!Capacitor.isNativePlatform() || !PUSH_NOTIFICATIONS_ENABLED) {
            return () => {
                cancelled = true;
                cleanupFns.forEach(cleanup => cleanup());
            };
        }

        const setup = async () => {
            try {
                const permResult = await PushNotifications.requestPermissions();
                if (permResult.receive !== 'granted') {
                    console.warn('[FCM] Permission not granted');
                    return;
                }

                await PushNotifications.register();

                const registrationListener = await PushNotifications.addListener('registration', async (token) => {
                    console.log('[FCM] Token:', token.value);
                    try {
                        localStorage.setItem(FCM_OTP_TOKEN_STORAGE_KEY, token.value);
                    } catch {
                        // ignore
                    }

                    const user = getStoredUser<{ id?: number }>();
                    if (!user?.id) return;

                    try {
                        await authApis().post<any>(endpoints['device-tokens-register'], {
                            userId: user.id,
                            token: token.value,
                            deviceType: DEVICE_TYPE,
                        });
                        console.log('[FCM] Token registered to backend');
                    } catch (e) {
                        console.error('[FCM] Failed to send token to backend:', e);
                    }
                });
                registerCleanup(() => void registrationListener.remove());

                const registrationErrorListener = await PushNotifications.addListener('registrationError', (err) => {
                    console.error('[FCM] Registration error:', err);
                });
                registerCleanup(() => void registrationErrorListener.remove());

                const receivedListener = await PushNotifications.addListener('pushNotificationReceived', (notification) => {
                    console.log('[FCM] Foreground notification:', notification);
                    handleRealtimeData(getNotificationData(notification));
                });
                registerCleanup(() => void receivedListener.remove());

                const actionListener = await PushNotifications.addListener('pushNotificationActionPerformed', (action) => {
                    const data = getNotificationData(action.notification);
                    console.log('[FCM] Notification tapped, data:', data);
                    handleRealtimeData(data);

                    if (data?.type === 'PAYMENT_SUCCESS' && data?.order_id) {
                        ionRouter.push(`/orders/${data.order_id}`);
                    }
                });
                registerCleanup(() => void actionListener.remove());
            } catch (err) {
                console.error('[FCM] Push notification setup failed:', err);
            }
        };

        void setup();

        return () => {
            cancelled = true;
            cleanupFns.forEach(cleanup => cleanup());
        };
    }, [ionRouter]);
}
