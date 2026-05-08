import { authApis, endpoints } from '../utils/Apis';
import type { Customer, CustomerUpsert, Order } from '../api/types';

const isRecord = (v: unknown): v is Record<string, unknown> =>
    typeof v === 'object' && v !== null;

const toNumber = (v: unknown): number => {
    const n = typeof v === 'number' ? v : Number(String(v ?? ''));
    return Number.isFinite(n) ? n : 0;
};

const normalizeCustomer = (raw: unknown): Customer | null => {
    if (!isRecord(raw)) return null;
    const id = toNumber(raw.id);
    const name = String(raw.name ?? '').trim();
    if (!id || !name) return null;
    return {
        id,
        name,
        phone: String(raw.phone ?? '').trim(),
        points: raw.points === null || raw.points === undefined ? null : toNumber(raw.points),
    };
};

const pickArray = (payload: unknown): unknown[] => {
    if (Array.isArray(payload)) return payload;
    if (!isRecord(payload)) return [];
    if (Array.isArray(payload.data)) return payload.data;
    if (Array.isArray(payload.content)) return payload.content;
    return [];
};

export const customerService = {
    async getCustomers(): Promise<Customer[]> {
        const res = await authApis().get<unknown>(endpoints.customers);
        return pickArray(res.data)
            .map(normalizeCustomer)
            .filter(Boolean) as Customer[];
    },

    async getCustomerById(id: number | string): Promise<Customer | null> {
        try {
            const res = await authApis().get<unknown>(endpoints['customer-detail'](id));
            if (!res.data) return null;
            const raw = isRecord(res.data) && res.data.data ? res.data.data : res.data;
            return normalizeCustomer(raw);
        } catch {
            return null;
        }
    },

    async createCustomer(data: CustomerUpsert): Promise<Customer> {
        const res = await authApis().post<unknown>(endpoints.customers, data);
        const raw = isRecord(res.data) && res.data.data ? res.data.data : res.data;
        const customer = normalizeCustomer(raw);
        if (!customer) throw new Error('Invalid response from server');
        return customer;
    },

    async updateCustomer(id: number | string, data: CustomerUpsert): Promise<Customer> {
        const res = await authApis().put<unknown>(endpoints['customer-detail'](id), data);
        const raw = isRecord(res.data) && res.data.data ? res.data.data : res.data;
        const customer = normalizeCustomer(raw);
        if (!customer) throw new Error('Invalid response from server');
        return customer;
    },

    async deleteCustomer(id: number | string): Promise<void> {
        await authApis().delete(endpoints['customer-detail'](id));
    },

    async searchCustomers(phone: string): Promise<Customer[]> {
        const res = await authApis().get<unknown>(
            `${endpoints.customers}/search?phone=${encodeURIComponent(phone)}`
        );
        return pickArray(res.data)
            .map(normalizeCustomer)
            .filter(Boolean) as Customer[];
    },

    /**
     * Lấy danh sách đơn hàng theo customerId (lọc client-side từ danh sách orders)
     */
    async getOrdersByCustomer(customerId: number): Promise<Order[]> {
        try {
            const res = await authApis().get<unknown>(
                `${endpoints.orders}?customerId=${customerId}&status=COMPLETED&status=PENDING_PAYMENT`
            );
            const payload = res.data;
            const list = Array.isArray((payload as any)?.data)
                ? (payload as any).data
                : Array.isArray(payload)
                ? payload
                : [];
            return list as Order[];
        } catch {
            return [];
        }
    },
};
