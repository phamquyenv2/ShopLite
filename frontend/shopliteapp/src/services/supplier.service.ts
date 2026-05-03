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
    async create(data: { name: string; phone?: string; address?: string; email?: string }): Promise<Supplier> {
        try {
            const res = await authApis().post<any>(endpoints.suppliers, data);
            return res.data?.data as Supplier;
        } catch (error: any) {
            throw new ApiError(error.response?.data?.message || 'Không thể tạo nhà cung cấp', error.response || { status: 500, data: null, headers: new Headers() });
        }
    }
};
