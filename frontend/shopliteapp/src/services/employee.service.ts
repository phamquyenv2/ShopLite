import { authApis, endpoints, ApiError } from '../utils/Apis';
import type { Employee } from '../api/types';

export type EmployeeUpdatePayload = {
    userId: number;
    officeId: number;
    salaryRate: number;
    qr?: string | null;
    note?: string | null;
};

export const employeeService = {
    async getEmployees(): Promise<Employee[]> {
        try {
            const res = await authApis().get<any>(endpoints.employees);
            return res.data?.data || [];
        } catch (error: any) {
            console.error('Lỗi khi lấy danh sách nhân viên:', error);
            throw new ApiError(
                error.response?.data?.message || 'Không thể tải danh sách nhân viên',
                error.response || { status: 500, data: null, headers: new Headers() }
            );
        }
    },

    async updateEmployee(id: number, payload: EmployeeUpdatePayload): Promise<Employee> {
        try {
            const res = await authApis().put<any>(endpoints['employee-detail'](id), payload);
            return res.data?.data || res.data;
        } catch (error: any) {
            throw new ApiError(
                error.response?.data?.message || 'Khong the cap nhat nhan vien',
                error.response || { status: 500, data: null, headers: new Headers() }
            );
        }
    }
};
