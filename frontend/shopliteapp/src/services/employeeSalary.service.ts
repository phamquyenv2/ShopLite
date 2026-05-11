import type { EmployeeSalaryHistory, EmployeeSalaryHistoryPayload } from '../api/types';
import { ApiError, authApis, endpoints } from '../utils/Apis';

const unwrap = <T>(payload: any): T => payload?.data ?? payload;
const unwrapList = <T>(payload: any): T[] => payload?.data ?? payload ?? [];

export const employeeSalaryService = {
    async getHistory(employeeId: number): Promise<EmployeeSalaryHistory[]> {
        try {
            const res = await authApis().get<any>(endpoints['employee-salary-histories'](employeeId));
            return unwrapList<EmployeeSalaryHistory>(res.data);
        } catch (error: any) {
            throw new ApiError(
                error.response?.data?.message || error.message || 'Khong the tai lich su luong',
                error.response || { status: 500, data: null, headers: new Headers() }
            );
        }
    },

    async getCurrent(employeeId: number): Promise<EmployeeSalaryHistory | null> {
        try {
            const res = await authApis().get<any>(endpoints['employee-salary-current'](employeeId));
            return unwrap<EmployeeSalaryHistory>(res.data);
        } catch (error: any) {
            if (error.response?.status === 404) return null;
            throw new ApiError(
                error.response?.data?.message || error.message || 'Khong the tai muc luong hien tai',
                error.response || { status: 500, data: null, headers: new Headers() }
            );
        }
    },

    async create(employeeId: number, payload: EmployeeSalaryHistoryPayload): Promise<EmployeeSalaryHistory> {
        try {
            const res = await authApis().post<any>(endpoints['employee-salary-histories'](employeeId), payload);
            return unwrap<EmployeeSalaryHistory>(res.data);
        } catch (error: any) {
            throw new ApiError(
                error.response?.data?.message || error.message || 'Khong the luu cau hinh luong',
                error.response || { status: 500, data: null, headers: new Headers() }
            );
        }
    },

    async getMine(): Promise<EmployeeSalaryHistory | null> {
        try {
            const res = await authApis().get<any>(endpoints['employee-salary-me']);
            return unwrap<EmployeeSalaryHistory>(res.data);
        } catch (error: any) {
            if (error.response?.status === 404) return null;
            throw error;
        }
    },

    async getMyHistory(): Promise<EmployeeSalaryHistory[]> {
        const res = await authApis().get<any>(endpoints['employee-salary-me-history']);
        return unwrapList<EmployeeSalaryHistory>(res.data);
    },
};
