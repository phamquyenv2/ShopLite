import { authApis, endpoints, ApiError } from '../utils/Apis';
import type { Supplier } from '../api/types';

export const supplierService = {
    async getAll(): Promise<Supplier[]> {
        try {
            const res = await authApis().get<any>(endpoints.suppliers);
            return res.data?.data || [];
        } catch (error: any) {
            throw new ApiError(error.response?.data?.message || 'Không thể tải nhà cung cấp', error.response || { status: 500, data: null, headers: new Headers() });
        }
    },

    async getById(id: number | string): Promise<Supplier> {
        try {
            const res = await authApis().get<any>(`${endpoints.suppliers}/${id}`);
            return res.data?.data as Supplier;
        } catch (error: any) {
            throw new ApiError(error.response?.data?.message || 'Không thể tải nhà cung cấp', error.response || { status: 500, data: null, headers: new Headers() });
        }
    },

    async create(data: { name: string; phone?: string; address?: string; email?: string }): Promise<Supplier> {
        try {
            const res = await authApis().post<any>(endpoints.suppliers, data);
            return res.data?.data as Supplier;
        } catch (error: any) {
            throw new ApiError(error.response?.data?.message || 'Không thể tạo nhà cung cấp', error.response || { status: 500, data: null, headers: new Headers() });
        }
    },

    async update(id: number | string, data: { name: string; phone?: string; address?: string; email?: string }): Promise<Supplier> {
        try {
            const res = await authApis().put<any>(`${endpoints.suppliers}/${id}`, data);
            return res.data?.data as Supplier;
        } catch (error: any) {
            throw new ApiError(error.response?.data?.message || 'Không thể cập nhật nhà cung cấp', error.response || { status: 500, data: null, headers: new Headers() });
        }
    },

    async delete(id: number | string): Promise<void> {
        try {
            await authApis().delete(`${endpoints.suppliers}/${id}`);
        } catch (error: any) {
            throw new ApiError(error.response?.data?.message || 'Không thể xóa nhà cung cấp', error.response || { status: 500, data: null, headers: new Headers() });
        }
    },
};
