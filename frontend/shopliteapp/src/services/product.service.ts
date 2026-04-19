import { Apis, endpoints } from '../utils/Apis';
import type { Category, Product, ProductPage } from '../api/types';

type GetProductsParams = {
    page?: number;
    size?: number;
    sortBy?: string;
    sortDir?: 'asc' | 'desc';
    keyword?: string;
    categoryId?: number;
};

const asArray = <T>(v: unknown): T[] => (Array.isArray(v) ? (v as T[]) : []);

const isRecord = (v: unknown): v is Record<string, unknown> => typeof v === 'object' && v !== null;

const pickArray = (payload: unknown): unknown[] => {
    if (Array.isArray(payload)) return payload;
    if (!isRecord(payload)) return [];

    const direct = payload.data;
    if (Array.isArray(direct)) return direct;

    const content = payload.content;
    if (Array.isArray(content)) return content;

    const items = payload.items;
    if (Array.isArray(items)) return items;

    const inner = payload.data;
    if (isRecord(inner) && Array.isArray(inner.data)) return inner.data as unknown[];

    return [];
};

const toNumber = (v: unknown): number => {
    const n = typeof v === 'number' ? v : Number(String(v ?? ''));
    return Number.isFinite(n) ? n : 0;
};

const normalizeCategory = (raw: unknown): Category | null => {
    if (!isRecord(raw)) return null;
    const id = toNumber(raw.id ?? raw.categoryId ?? raw.category_id);
    const name = String(raw.name ?? raw.categoryName ?? raw.category_name ?? '').trim();
    if (!id || !name) return null;
    return { id, name };
};

const normalizeProduct = (raw: unknown): Product | null => {
    if (!isRecord(raw)) return null;

    const id = toNumber(raw.id);
    const name = String(raw.name ?? '').trim();
    if (!id || !name) return null;

    const categoryId = toNumber(raw.categoryId ?? raw.category_id ?? 0);
    const categoryName = String(raw.categoryName ?? raw.category_name ?? '').trim() || null;

    const sku = (raw.sku ?? raw.SKU) as unknown;
    const barcode = raw.barcode as unknown;
    const image = raw.image as unknown;

    const stock = toNumber(raw.stock ?? raw.quantity ?? raw.qty ?? 0);
    const costPrice = toNumber(raw.costPrice ?? raw.cost_price ?? 0);
    const sellingPrice = toNumber(raw.sellingPrice ?? raw.selling_price ?? raw.price ?? 0);

    const status = (raw.status as Product['status']) ?? undefined;
    const deleted = Boolean(raw.deleted);

    return {
        id,
        categoryId,
        categoryName,
        unitId: toNumber(raw.unitId ?? raw.unit_id ?? 0),
        unitName: (raw.unitName ?? raw.unit_name) ? String(raw.unitName ?? raw.unit_name) : null,
        name,
        sku: typeof sku === 'string' && sku.trim() ? sku.trim() : null,
        barcode: typeof barcode === 'string' && barcode.trim() ? barcode.trim() : null,
        stock,
        costPrice,
        sellingPrice,
        image: typeof image === 'string' && image.trim() ? image.trim() : null,
        status,
        deleted,
        createdAt: typeof raw.createdAt === 'string' ? raw.createdAt : (typeof raw.created_at === 'string' ? String(raw.created_at) : undefined),
        updatedAt: typeof raw.updatedAt === 'string' ? raw.updatedAt : (typeof raw.updated_at === 'string' ? String(raw.updated_at) : undefined),
        version: raw.version === null ? null : (typeof raw.version === 'number' ? raw.version : undefined),
        minStock: raw.minStock === null ? null : (typeof raw.minStock === 'number' ? raw.minStock : undefined),
        maxStock: raw.maxStock === null ? null : (typeof raw.maxStock === 'number' ? raw.maxStock : undefined),
    };
};

const parseProducts = (payload: unknown): Product[] => {
    if (payload && typeof payload === 'object' && Array.isArray((payload as ProductPage).data)) {
        const arr = asArray<unknown>((payload as ProductPage).data);
        return arr.map(normalizeProduct).filter(Boolean) as Product[];
    }
    return pickArray(payload).map(normalizeProduct).filter(Boolean) as Product[];
};

const parseCategories = (payload: unknown): Category[] =>
    pickArray(payload).map(normalizeCategory).filter(Boolean) as Category[];

export const productService = {
    async getCategories(): Promise<Category[]> {
        const res = await Apis.get<unknown>(endpoints.categories);
        return parseCategories(res.data);
    },

    async getProducts(params: GetProductsParams = {}): Promise<Product[]> {
        const query = new URLSearchParams({
            page: String(params.page ?? 0),
            size: String(params.size ?? 200),
            sortBy: params.sortBy ?? 'createdAt',
            sortDir: params.sortDir ?? 'desc',
        });

        if (typeof params.keyword === 'string' && params.keyword.trim()) query.set('keyword', params.keyword.trim());
        if (typeof params.categoryId === 'number' && params.categoryId > 0) query.set('categoryId', String(params.categoryId));

        const wantsServerFilter =
            (typeof params.keyword === 'string' && params.keyword.trim()) ||
            (typeof params.categoryId === 'number' && params.categoryId > 0);

        try {
            const res = await Apis.get<unknown>(`${endpoints.products}?${query.toString()}`);
            return parseProducts(res.data);
        } catch (err) {
            if (!wantsServerFilter) throw err;
            // Backend might not support keyword/categoryId query; retry without them.
            const retryQuery = new URLSearchParams({
                page: String(params.page ?? 0),
                size: String(params.size ?? 200),
                sortBy: params.sortBy ?? 'createdAt',
                sortDir: params.sortDir ?? 'desc',
            });
            const res = await Apis.get<unknown>(`${endpoints.products}?${retryQuery.toString()}`);
            return parseProducts(res.data);
        }
    },

    async getProductDetail(id: number | string): Promise<Product | null> {
        try {
            const res = await Apis.get<unknown>(endpoints['product-detail'](id));
            if (!res.data) return null;
            return normalizeProduct(isRecord(res.data) && res.data.data ? res.data.data : res.data);
        } catch (err) {
            return null;
        }
    },

    async searchByBarcode(barcode: string): Promise<Product | null> {
        const code = String(barcode ?? '').trim();
        if (!code) return null;

        const list = await productService.getProducts({ keyword: code, size: 200 });
        const exact = list.find((p) => String(p.barcode ?? '').trim() === code);
        if (exact) return exact;

        // Fallback: fetch broader list and match.
        const all = await productService.getProducts({ size: 500 });
        return all.find((p) => String(p.barcode ?? '').trim() === code) ?? null;
    },
};

export type { GetProductsParams };
