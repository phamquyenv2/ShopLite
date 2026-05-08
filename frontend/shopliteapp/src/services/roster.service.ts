import type { Roster, RosterUpsert } from '../api/types';
import { ApiError, authApis, endpoints } from '../utils/Apis';

const unwrapList = <T>(payload: any): T[] => payload?.data ?? payload ?? [];
const unwrapOne = <T>(payload: any): T => payload?.data ?? payload;

export const rosterService = {
    async getByEmployee(employeeId: number, from: string, to: string): Promise<Roster[]> {
        try {
            const url = `${endpoints['roster-by-employee'](employeeId)}?from=${encodeURIComponent(from)}&to=${encodeURIComponent(to)}`;
            const res = await authApis().get<any>(url);
            return unwrapList<Roster>(res.data);
        } catch (error: any) {
            throw new ApiError(error.response?.data?.message || error.message || 'Không thể tải lịch theo nhân viên', error.response || { status: 500, data: null, headers: new Headers() });
        }
    },

    async getByDay(date: string): Promise<Roster[]> {
        try {
            const res = await authApis().get<any>(`${endpoints['roster-by-day']}?date=${encodeURIComponent(date)}`);
            return unwrapList<Roster>(res.data);
        } catch (error: any) {
            throw new ApiError(error.response?.data?.message || error.message || 'Không thể tải lịch làm việc', error.response || { status: 500, data: null, headers: new Headers() });
        }
    },

    async getByMonth(month: string): Promise<Roster[]> {
        try {
            const res = await authApis().get<any>(`${endpoints['roster-by-month']}?month=${encodeURIComponent(month)}`);
            return unwrapList<Roster>(res.data);
        } catch (error: any) {
            throw new ApiError(error.response?.data?.message || error.message || 'Không thể tải lịch làm việc theo tháng', error.response || { status: 500, data: null, headers: new Headers() });
        }
    },

    async create(payload: RosterUpsert): Promise<Roster> {
        try {
            const res = await authApis().post<any>(endpoints.roster, payload);
            return unwrapOne<Roster>(res.data);
        } catch (error: any) {
            throw new ApiError(error.response?.data?.message || error.message || 'Không thể tạo lịch làm việc', error.response || { status: 500, data: null, headers: new Headers() });
        }
    },

    async update(id: number, payload: RosterUpsert): Promise<Roster> {
        try {
            const res = await authApis().put<any>(endpoints['roster-detail'](id), payload);
            return unwrapOne<Roster>(res.data);
        } catch (error: any) {
            throw new ApiError(error.response?.data?.message || error.message || 'Không thể cập nhật lịch làm việc', error.response || { status: 500, data: null, headers: new Headers() });
        }
    },

    async remove(id: number): Promise<void> {
        try {
            await authApis().delete(endpoints['roster-detail'](id));
        } catch (error: any) {
            throw new ApiError(error.response?.data?.message || error.message || 'Không thể xóa lịch làm việc', error.response || { status: 500, data: null, headers: new Headers() });
        }
    },
};
