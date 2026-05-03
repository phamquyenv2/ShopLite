import { authApis, endpoints, ApiError } from '../utils/Apis';
import type { Role } from '../api/types';

export const roleService = {
    async getRoles(): Promise<Role[]> {
        try {
            const res = await authApis().get<any>(endpoints.roles);
            // The API returns a paginated structure:
            // res.data = { statusCode: 200, data: { data: [...], totalElements: ... } }
            // So the array is at res.data?.data?.data
            return res.data?.data?.data || res.data?.data?.content || [];
        } catch (error: any) {
            console.error('Lỗi khi lấy danh sách vai trò:', error);
            throw new ApiError(error.response?.data?.message || 'Không thể tải danh sách vai trò', error.response || { status: 500, data: null, headers: new Headers() });
        }
    },

    async getRoleById(id: string | number): Promise<Role> {
        try {
            const res = await authApis().get<any>(endpoints['role-detail'](id));
            return res.data?.data;
        } catch (error: any) {
             console.error('Lỗi khi tải chi tiết vai trò:', error);
            throw new ApiError(error.response?.data?.message || 'Không thể tải chi tiết vai trò', error.response || { status: 500, data: null, headers: new Headers() });
        }
    },

    async updateRole(id: string | number, data: Partial<Role> & { permissionIds?: number[] }): Promise<Role> {
        try {
            const res = await authApis().put<any>(endpoints['role-detail'](id), data);
            return res.data?.data;
        } catch (error: any) {
            console.error('Lỗi khi cập nhật vai trò:', error);
            throw new ApiError(error.response?.data?.message || 'Lỗi khi cập nhật vai trò', error.response || { status: 500, data: null, headers: new Headers() });
        }
    }
};
