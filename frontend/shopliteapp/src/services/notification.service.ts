import { authApis, endpoints, ApiError } from '../utils/Apis';
import type { Notification } from '../api/types';

const unwrap = <T>(payload: any): T => payload?.data ?? payload;

export const notificationService = {
    async getNotifications(): Promise<Notification[]> {
        try {
            const res = await authApis().get<any>(endpoints.notifications);
            return unwrap<Notification[]>(res.data) || [];
        } catch (error: any) {
            throw new ApiError(error.response?.data?.message || 'Khong the tai thong bao', error.response);
        }
    },

    async markRead(id: number | string): Promise<Notification> {
        try {
            const res = await authApis().patch<any>(endpoints['notification-read'](id));
            return unwrap<Notification>(res.data);
        } catch (error: any) {
            throw new ApiError(error.response?.data?.message || 'Khong the danh dau thong bao', error.response);
        }
    }
};
