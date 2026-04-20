import { authApis, endpoints, ApiError } from '../utils/Apis';
import type { Supplier } from '../api/types';

export const supplierService = {
    async getAll(): Promise<Supplier[]> {
        try {
            const res = await authApis().get(endpoints.suppliers);
            return res.data?.data || [];
        } catch (error: any) {
            throw new ApiError(error.response?.data?.message || 'Không thể tải nhà cung cấp');
        }
    }
};
