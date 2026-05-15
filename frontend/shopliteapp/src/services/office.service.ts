import { authApis, endpoints, ApiError } from '../utils/Apis';
import type { Office } from '../api/types';

export type OfficeUpsert = {
    name: string;
    officeLat: number;
    officeLng: number;
    radius: number;
};

export const officeService = {
    async getOffices(): Promise<Office[]> {
        try {
            const res = await authApis().get<any>(endpoints.offices);
            return res.data?.data || res.data || [];
        } catch (error: any) {
            throw new ApiError(
                error.response?.data?.message || 'Không thể tải danh sách văn phòng',
                error.response || { status: 500, data: null, headers: new Headers() }
            );
        }
    },

    async createOffice(payload: OfficeUpsert): Promise<Office> {
        try {
            const res = await authApis().post<any>(endpoints.offices, payload);
            return res.data?.data || res.data;
        } catch (error: any) {
            throw new ApiError(
                error.response?.data?.message || 'Không thể tạo văn phòng',
                error.response || { status: 500, data: null, headers: new Headers() }
            );
        }
    },

    async updateOffice(id: number, payload: OfficeUpsert): Promise<Office> {
        try {
            const res = await authApis().put<any>(endpoints['office-detail'](id), payload);
            return res.data?.data || res.data;
        } catch (error: any) {
            throw new ApiError(
                error.response?.data?.message || 'Không thể cập nhật văn phòng',
                error.response || { status: 500, data: null, headers: new Headers() }
            );
        }
    },

    async deleteOffice(id: number): Promise<void> {
        try {
            await authApis().delete(endpoints['office-detail'](id));
        } catch (error: any) {
            throw new ApiError(
                error.response?.data?.message || 'Không thể xóa văn phòng',
                error.response || { status: 500, data: null, headers: new Headers() }
            );
        }
    }
};
