import { authApis, endpoints, ApiError } from '../utils/Apis';
import type { ImportOrder, ImportOrderUpsert, InspectImportOrderPayload } from '../api/types';

export const importOrderService = {
    async getAll(): Promise<ImportOrder[]> {
        try {
            const res = await authApis().get<any>(endpoints['import-orders']);
            return res.data?.data || [];
        } catch (error: any) {
            throw new ApiError(error.response?.data?.message || 'Không thể tải danh sách nhập hàng', error.response || { status: 500, data: null, headers: new Headers() });
        }
    },

    async getById(id: number | string): Promise<ImportOrder> {
        try {
            const res = await authApis().get<any>(endpoints['import-order-detail'](id));
            return res.data?.data;
        } catch (error: any) {
            throw new ApiError(error.response?.data?.message || 'Không thể tải phiếu nhập', error.response || { status: 500, data: null, headers: new Headers() });
        }
    },

    async create(data: ImportOrderUpsert): Promise<ImportOrder> {
        try {
            const res = await authApis().post<any>(endpoints['import-orders'], data);
            return res.data?.data;
        } catch (error: any) {
            throw new ApiError(error.response?.data?.message || 'Không thể tạo phiếu nhập', error.response || { status: 500, data: null, headers: new Headers() });
        }
    },

    async update(id: number | string, data: ImportOrderUpsert): Promise<ImportOrder> {
        try {
            const res = await authApis().put<any>(endpoints['import-order-detail'](id), data);
            return res.data?.data;
        } catch (error: any) {
            throw new ApiError(error.response?.data?.message || 'Không thể cập nhật phiếu nhập', error.response || { status: 500, data: null, headers: new Headers() });
        }
    },

    async updateStatus(id: number | string, status: string): Promise<ImportOrder> {
        try {
            const res = await authApis().put<any>(endpoints['import-order-status'](id), { status });
            return res.data?.data;
        } catch (error: any) {
            throw new ApiError(error.response?.data?.message || 'Không thể cập nhật trạng thái', error.response || { status: 500, data: null, headers: new Headers() });
        }
    },

    async sendToSupplier(id: number | string): Promise<ImportOrder> {
        return this.postAction(id, 'send');
    },

    async inspect(id: number | string, data: InspectImportOrderPayload): Promise<ImportOrder> {
        return this.postAction(id, 'inspect', data);
    },

    async approveDiscrepancy(id: number | string, note?: string): Promise<ImportOrder> {
        return this.postAction(id, 'approve-discrepancy', { note });
    },

    async rejectDiscrepancy(id: number | string, note?: string): Promise<ImportOrder> {
        return this.postAction(id, 'reject-discrepancy', { note });
    },

    async postAction(id: number | string, action: string, data?: unknown): Promise<ImportOrder> {
        try {
            const res = await authApis().post<any>(`${endpoints['import-orders']}/${id}/${action}`, data);
            return res.data?.data;
        } catch (error: any) {
            throw new ApiError(error.response?.data?.message || 'Không thể cập nhật phiếu nhập', error.response || { status: 500, data: null, headers: new Headers() });
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
            const res = await authApis().post<any>(`${endpoints['import-orders']}/${id}/pay`, data);
            return res.data?.data;
        } catch (error: any) {
            throw new ApiError(error.response?.data?.message || 'Không thể thanh toán phiếu nhập', error.response || { status: 500, data: null, headers: new Headers() });
        }
    },
};
