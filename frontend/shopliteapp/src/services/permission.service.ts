import { authApis, endpoints, ApiError } from '../utils/Apis';
import type { Permission } from '../api/types';

export const permissionService = {
    async getPermissions(): Promise<Permission[]> {
        try {
            const res = await authApis().get<any>(`${endpoints.permissions}/all`);
            return res.data?.data || [];
        } catch (error: any) {
            console.error('Lỗi khi lấy danh sách phân quyền:', error);
            throw new ApiError(error.response?.data?.message || 'Không thể tải danh sách phân quyền', error.response || { status: 500, data: null, headers: new Headers() });
        }
    }
};
