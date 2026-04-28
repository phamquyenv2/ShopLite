import { authApis, endpoints, ApiError } from '../utils/Apis';
import type { ImportOrder, ImportOrderUpsert } from '../api/types';

export const importOrderService = {
    async getAll(): Promise<ImportOrder[]> {
        try {
            const res = await authApis().get(endpoints['import-orders']);
            return res.data?.data || [];
        } catch (error: any) {
            throw new ApiError(error.response?.data?.message || 'Không thể tải danh sách nhập hàng');
        }
    },

    async getById(id: number | string): Promise<ImportOrder> {
        try {
            const res = await authApis().get(endpoints['import-order-detail'](id));
            return res.data?.data;
        } catch (error: any) {
            throw new ApiError(error.response?.data?.message || 'Không thể tải phiếu nhập');
        }
    },

    async create(data: ImportOrderUpsert): Promise<ImportOrder> {
        try {
            const res = await authApis().post(endpoints['import-orders'], data);
            return res.data?.data;
        } catch (error: any) {
            throw new ApiError(error.response?.data?.message || 'Không thể tạo phiếu nhập');
        }
    },

    async update(id: number | string, data: ImportOrderUpsert): Promise<ImportOrder> {
        try {
            const res = await authApis().put(endpoints['import-order-detail'](id), data);
            return res.data?.data;
        } catch (error: any) {
            throw new ApiError(error.response?.data?.message || 'Không thể cập nhật phiếu nhập');
        }
    },

    async updateStatus(id: number | string, status: string): Promise<ImportOrder> {
        try {
            const res = await authApis().put(endpoints['import-order-status'](id), { status });
            return res.data?.data;
        } catch (error: any) {
            throw new ApiError(error.response?.data?.message || 'Không thể cập nhật trạng thái');
        }
    },

    /**
     * Retry payment cho đơn đang ở PENDING_PAYMENT.
     * Chỉ gọi bước payment — KHÔNG update/confirm lại.
     * Backend endpoint: POST /api/v1/import-orders/{id}/pay
     */
    async payOnly(id: number | string, data: {
        paidAmount: number;
        paymentMethod: string;
        note?: string;
    }): Promise<ImportOrder> {
        try {
            const res = await authApis().post(`${endpoints['import-orders']}/${id}/pay`, data);
            return res.data?.data;
        } catch (error: any) {
            throw new ApiError(error.response?.data?.message || 'Không thể thanh toán phiếu nhập');
        }
    },
};
