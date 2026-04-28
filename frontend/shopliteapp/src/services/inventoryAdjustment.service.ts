import { authApis, ApiError, endpoints } from '../utils/Apis';
import type { InventoryAdjustment, InventoryAdjustmentUpsert, InventoryLog } from '../api/types';

const asArray = <T>(value: unknown): T[] => (Array.isArray(value) ? value as T[] : []);
const isRecord = (value: unknown): value is Record<string, unknown> => typeof value === 'object' && value !== null;
const toNumber = (value: unknown): number => {
  const parsed = typeof value === 'number' ? value : Number(String(value ?? ''));
  return Number.isFinite(parsed) ? parsed : 0;
};

const normalizeLog = (raw: unknown): InventoryLog | null => {
  if (!isRecord(raw)) return null;
  const id = toNumber(raw.id);
  const productId = toNumber(raw.productId);
  const type = String(raw.type ?? '').trim() as InventoryLog['type'];
  if (!id || !productId || !type) return null;
  return {
    id,
    productId,
    type,
    quantityIn: raw.quantityIn == null ? null : toNumber(raw.quantityIn),
    quantityOut: raw.quantityOut == null ? null : toNumber(raw.quantityOut),
    balanceAfter: raw.balanceAfter == null ? null : toNumber(raw.balanceAfter),
    currentStock: raw.currentStock == null ? null : toNumber(raw.currentStock),
    createdAt: typeof raw.createdAt === 'string' ? raw.createdAt : undefined,
    productName: typeof raw.productName === 'string' ? raw.productName : null,
    productSku: typeof raw.productSku === 'string' ? raw.productSku : null,
  };
};

const normalizeAdjustment = (raw: unknown): InventoryAdjustment | null => {
  if (!isRecord(raw)) return null;
  const id = toNumber(raw.id);
  const reason = String(raw.reason ?? '').trim();
  const createdBy = String(raw.createdBy ?? '').trim();
  if (!id || !reason || !createdBy) return null;
  const logs = asArray<unknown>(raw.logs).map(normalizeLog).filter(Boolean) as InventoryLog[];
  return {
    id,
    reason,
    createdBy,
    note: typeof raw.note === 'string' ? raw.note : null,
    createdAt: typeof raw.createdAt === 'string' ? raw.createdAt : undefined,
    logs,
  };
};

const extractList = (payload: unknown): InventoryAdjustment[] => {
  if (Array.isArray(payload)) return payload.map(normalizeAdjustment).filter(Boolean) as InventoryAdjustment[];
  if (isRecord(payload) && Array.isArray(payload.data)) {
    return payload.data.map(normalizeAdjustment).filter(Boolean) as InventoryAdjustment[];
  }
  return [];
};

const extractDetail = (payload: unknown): InventoryAdjustment | null => {
  if (isRecord(payload) && isRecord(payload.data)) return normalizeAdjustment(payload.data);
  return normalizeAdjustment(payload);
};

const fallbackResponse = { status: 500, data: null, headers: new Headers() };

export const inventoryAdjustmentService = {
  async getAll(): Promise<InventoryAdjustment[]> {
    try {
      const res = await authApis().get(endpoints['inventory-adjustments']);
      return extractList(res.data);
    } catch (error: any) {
      throw new ApiError(error.response?.data?.message || 'Khong the tai danh sach kiem kho', error.response ?? fallbackResponse);
    }
  },

  async getById(id: number | string): Promise<InventoryAdjustment> {
    try {
      const res = await authApis().get(endpoints['inventory-adjustment-detail'](id));
      const data = extractDetail(res.data);
      if (!data) throw new Error('Invalid payload');
      return data;
    } catch (error: any) {
      throw new ApiError(error.response?.data?.message || 'Khong the tai phieu kiem kho', error.response ?? fallbackResponse);
    }
  },

  async create(payload: InventoryAdjustmentUpsert): Promise<InventoryAdjustment> {
    try {
      const res = await authApis().post(endpoints['inventory-adjustments'], payload);
      const data = extractDetail(res.data);
      if (!data) throw new Error('Invalid payload');
      return data;
    } catch (error: any) {
      throw new ApiError(error.response?.data?.message || 'Khong the tao phieu kiem kho', error.response ?? fallbackResponse);
    }
  },
};
