import { authApis, endpoints, ApiError, STORAGE_KEYS } from '../utils/Apis';
import type { AcceptInvitationResponse, StoreInvitation } from '../api/types';

const unwrap = <T>(payload: any): T => payload?.data ?? payload;

export const storeInvitationService = {
    async createInvitation(phone: string, roleId: number | string): Promise<StoreInvitation> {
        try {
            const res = await authApis().post<any>(endpoints['store-invitations'], { phone, roleId: Number(roleId) });
            return unwrap<StoreInvitation>(res.data);
        } catch (error: any) {
            throw new ApiError(error.response?.data?.message || 'Khong the gui loi moi', error.response);
        }
    },

    async accept(id: number | string): Promise<AcceptInvitationResponse> {
        try {
            const res = await authApis().post<any>(endpoints['store-invitation-accept'](id));
            const payload = unwrap<AcceptInvitationResponse>(res.data);
            if (payload?.currentStore) {
                localStorage.setItem(STORAGE_KEYS.currentStore, JSON.stringify(payload.currentStore));
            }
            return payload;
        } catch (error: any) {
            throw new ApiError(error.response?.data?.message || 'Khong the chap nhan loi moi', error.response);
        }
    },

    async decline(id: number | string): Promise<StoreInvitation> {
        try {
            const res = await authApis().post<any>(endpoints['store-invitation-decline'](id));
            return unwrap<StoreInvitation>(res.data);
        } catch (error: any) {
            throw new ApiError(error.response?.data?.message || 'Khong the tu choi loi moi', error.response);
        }
    }
};
