

import { useEffect } from 'react';
import { Capacitor } from '@capacitor/core';
import { PushNotifications } from '@capacitor/push-notifications';
import { useIonRouter } from '@ionic/react';
import { authApis, endpoints } from './Apis';

const DEVICE_TYPE = Capacitor.getPlatform() === 'ios' ? 'IOS' : 'ANDROID';

export function usePushNotifications() {
    const ionRouter = useIonRouter();

    useEffect(() => {
        // Chỉ chạy trên native (Android/iOS)
        if (!Capacitor.isNativePlatform()) return;

        const setup = async () => {
            // 1. Xin quyền
            const permResult = await PushNotifications.requestPermissions();
            if (permResult.receive !== 'granted') {
                console.warn('[FCM] Permission not granted');
                return;
            }

            // 2. Đăng ký nhận push
            await PushNotifications.register();

            // 3. Nhận FCM token → gửi về backend
            PushNotifications.addListener('registration', async (token) => {
                console.log('[FCM] Token:', token.value);
                try {
                    await authApis().post(endpoints['device-tokens-register'], {
                        token: token.value,
                        deviceType: DEVICE_TYPE,
                    });
                    console.log('[FCM] Token registered to backend');
                } catch (e) {
                    console.error('[FCM] Failed to send token to backend:', e);
                }
            });

            // 4. Lỗi đăng ký
            PushNotifications.addListener('registrationError', (err) => {
                console.error('[FCM] Registration error:', err);
            });

            // 5. Foreground notification — hiện trong console (có thể thêm toast sau)
            PushNotifications.addListener('pushNotificationReceived', (notification) => {
                console.log('[FCM] Foreground notification:', notification);
                // TODO: hiện IonToast với nội dung từ notification.title + notification.body
            });

            // 6. Tap vào notification (background/killed) → navigate
            PushNotifications.addListener('pushNotificationActionPerformed', (action) => {
                const data = action.notification.data as Record<string, string>;
                console.log('[FCM] Notification tapped, data:', data);

                if (data?.type === 'PAYMENT_SUCCESS' && data?.order_id) {
                    ionRouter.push(`/orders/${data.order_id}`);
                }
            });
        };

        void setup();

        return () => {
            // Cleanup listeners khi component unmount
            void PushNotifications.removeAllListeners();
        };
    }, [ionRouter]);
}
