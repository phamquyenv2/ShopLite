import { authApis, endpoints, ApiError } from '../utils/Apis';
import type { ImportReturnOrder, ImportReturnOrderUpsert } from '../api/types';

export const importReturnOrderService = {
    async getAll(): Promise<ImportReturnOrder[]> {
        try {
            const res = await authApis().get<any>(endpoints['import-return-orders']);
            return (res.data as any)?.data || [];
        } catch (error: any) {
            throw new ApiError(
                error.response?.data?.message || 'Không thể tải danh sách trả hàng nhập',
                error.response || { status: 500, data: null, headers: new Headers() }
            );
        }
    },

    async getById(id: number | string): Promise<ImportReturnOrder> {
        try {
            const res = await authApis().get<any>(endpoints['import-return-order-detail'](id));
            return (res.data as any)?.data;
        } catch (error: any) {
            throw new ApiError(
                error.response?.data?.message || 'Không thể tải phiếu trả hàng',
                error.response || { status: 500, data: null, headers: new Headers() }
            );
        }
    },

    async create(data: ImportReturnOrderUpsert): Promise<ImportReturnOrder> {
        try {
            const res = await authApis().post<any>(endpoints['import-return-orders'], data);
            return (res.data as any)?.data;
        } catch (error: any) {
            throw new ApiError(
                error.response?.data?.message || 'Không thể tạo phiếu trả hàng',
                error.response || { status: 500, data: null, headers: new Headers() }
            );
        }
    },
};
