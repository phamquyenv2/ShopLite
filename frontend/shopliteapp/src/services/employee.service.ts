import { authApis, endpoints, ApiError } from '../utils/Apis';
import type { Employee } from '../api/types';

export const employeeService = {
    async getEmployees(): Promise<Employee[]> {
        try {
            const res = await authApis().get(endpoints.employees);
            return res.data?.data || [];
        } catch (error: any) {
            console.error('Lỗi khi lấy danh sách nhân viên:', error);
            throw new ApiError(error.response?.data?.message || 'Không thể tải danh sách nhân viên');
        }
    }
};
