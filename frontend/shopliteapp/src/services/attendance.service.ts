import type { Attendance, AttendanceLocationPayload, Roster } from '../api/types';
import { ApiError, authApis, endpoints } from '../utils/Apis';

const unwrapList = <T>(payload: any): T[] => payload?.data ?? payload ?? [];
const unwrapOne = <T>(payload: any): T | null => payload?.data ?? payload ?? null;

export const attendanceService = {
    async getAll(): Promise<Attendance[]> {
        try {
            const res = await authApis().get<any>(endpoints.attendance);
            return unwrapList<Attendance>(res.data);
        } catch (error: any) {
            throw new ApiError(error.response?.data?.message || error.message || 'Không thể tải dữ liệu chấm công', error.response || { status: 500, data: null, headers: new Headers() });
        }
    },

    async getMeToday(): Promise<Attendance | null> {
        try {
            const res = await authApis().get<any>(endpoints['attendance-me-today']);
            return unwrapOne<Attendance>(res.data);
        } catch (error: any) {
            if (error.response?.status === 204) return null;
            throw new ApiError(error.response?.data?.message || error.message || 'Không thể tải ca hôm nay', error.response || { status: 500, data: null, headers: new Headers() });
        }
    },

    async getMyTodayRosters(): Promise<Roster[]> {
        try {
            const res = await authApis().get<any>(endpoints['attendance-me-rosters-today']);
            return unwrapList<Roster>(res.data);
        } catch (error: any) {
            throw new ApiError(error.response?.data?.message || error.message || 'Khong the tai ca lam hom nay', error.response || { status: 500, data: null, headers: new Headers() });
        }
    },

    async checkIn(payload: AttendanceLocationPayload): Promise<Attendance> {
        try {
            const res = await authApis().post<any>(endpoints['attendance-check-in'], payload);
            return unwrapOne<Attendance>(res.data) as Attendance;
        } catch (error: any) {
            throw new ApiError(error.response?.data?.message || error.message || 'Không thể check-in', error.response || { status: 500, data: null, headers: new Headers() });
        }
    },

    async checkOut(payload: AttendanceLocationPayload): Promise<Attendance> {
        try {
            const res = await authApis().post<any>(endpoints['attendance-check-out'], payload);
            return unwrapOne<Attendance>(res.data) as Attendance;
        } catch (error: any) {
            throw new ApiError(error.response?.data?.message || error.message || 'Không thể check-out', error.response || { status: 500, data: null, headers: new Headers() });
        }
    },
};
