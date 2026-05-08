import type { Payroll, PayrollSyncPayload } from '../api/types';
import { ApiError, authApis, endpoints } from '../utils/Apis';

const unwrapList = <T>(payload: any): T[] => payload?.data ?? payload ?? [];

export const payrollService = {
    async getAll(): Promise<Payroll[]> {
        try {
            const res = await authApis().get<any>(endpoints.payrolls);
            return unwrapList<Payroll>(res.data);
        } catch (error: any) {
            throw new ApiError(error.response?.data?.message || error.message || 'Không thể tải bảng lương', error.response || { status: 500, data: null, headers: new Headers() });
        }
    },

    async getMine(): Promise<Payroll[]> {
        try {
            const res = await authApis().get<any>(endpoints['payrolls-me']);
            return unwrapList<Payroll>(res.data);
        } catch (error: any) {
            throw new ApiError(error.response?.data?.message || error.message || 'Khong the tai bang luong cua ban', error.response || { status: 500, data: null, headers: new Headers() });
        }
    },

    async syncMonthly(payload: PayrollSyncPayload): Promise<Payroll[]> {
        try {
            const res = await authApis().post<any>(endpoints['payroll-sync-monthly'], payload);
            return unwrapList<Payroll>(res.data);
        } catch (error: any) {
            throw new ApiError(error.response?.data?.message || error.message || 'Không thể đồng bộ bảng lương', error.response || { status: 500, data: null, headers: new Headers() });
        }
    },
};
