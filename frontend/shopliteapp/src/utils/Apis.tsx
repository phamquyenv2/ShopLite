/* eslint-disable react-refresh/only-export-components */

export type HttpMethod = 'GET' | 'POST' | 'PUT' | 'PATCH' | 'DELETE';

export type ApiResult<T> = {
    ok: boolean;
    status: number;
    data: T | null;
    headers: Headers;
};

export class ApiError extends Error {
    response: { status: number; data: unknown; headers: Headers };

    constructor(message: string, response: { status: number; data: unknown; headers: Headers }) {
        super(message);
        this.name = 'ApiError';
        this.response = response;
    }
}

export const STORAGE_KEYS = {
    accessToken: 'token',
    refreshToken: 'refreshToken',
    user: 'userdata',
    currentStore: 'currentStore',
} as const;

export const AUTH_INVALID_EVENT = 'shoplite:auth-invalid';

export type AuthInvalidReason = 'missing_refresh_token' | 'refresh_failed' | 'unauthorized';

export const emitAuthInvalid = (reason: AuthInvalidReason): void => {
    try {
        globalThis.dispatchEvent(new CustomEvent(AUTH_INVALID_EVENT, { detail: { reason } }));
    } catch {
        // ignore
    }
};

export const endpoints = {
    // auth
    login: '/api/v1/auth/login',
    register: '/api/v1/auth/register',
    refresh: '/api/v1/auth/refresh',
    me: '/api/v1/auth/me',
    logout: '/api/v1/auth/logout',

    // OTP registration flow
    'register-otp-send':    '/api/v1/auth/register/otp/send',
    'register-otp-verify':  '/api/v1/auth/register/otp/verify',
    'register-store':       '/api/v1/auth/register/store',
    'register-complete':    '/api/v1/auth/register/complete',

    // categories
    categories: '/api/v1/categories',
    'category-detail': (id: number | string) => `/api/v1/categories/${id}`,

    // customers
    customers: '/api/v1/customers',
    'customer-detail': (id: number | string) => `/api/v1/customers/${id}`,

    // products
    products: '/api/v1/products',
    'product-detail': (id: number | string) => `/api/v1/products/${id}`,

    // suppliers
    suppliers: '/api/v1/suppliers',
    'supplier-detail': (id: number | string) => `/api/v1/suppliers/${id}`,

    // units
    units: '/api/v1/units',
    'unit-detail': (id: number | string) => `/api/v1/units/${id}`,

    // users
    users: '/api/v1/users',
    'user-detail': (id: number | string) => `/api/v1/users/${id}`,

    // employees
    employees: '/api/v1/employees',
    'employee-detail': (id: number | string) => `/api/v1/employees/${id}`,
    'employee-salary-histories': (employeeId: number | string) => `/api/v1/employees/${employeeId}/salary-histories`,
    'employee-salary-current': (employeeId: number | string) => `/api/v1/employees/${employeeId}/salary-histories/current`,
    'employee-salary-me': '/api/v1/employee-salaries/me',
    'employee-salary-me-history': '/api/v1/employee-salaries/me/history',

    // offices
    offices: '/api/v1/offices',
    'office-detail': (id: number | string) => `/api/v1/offices/${id}`,

    // roles (paged list)
    roles: '/api/v1/roles',
    'role-detail': (id: number | string) => `/api/v1/roles/${id}`,

    // permissions (paged list)
    permissions: '/api/v1/permissions',
    'permission-detail': (id: number | string) => `/api/v1/permissions/${id}`,

    // invitations + notifications
    'store-invitations': '/api/v1/store-invitations',
    'store-invitation-accept': (id: number | string) => `/api/v1/store-invitations/${id}/accept`,
    'store-invitation-decline': (id: number | string) => `/api/v1/store-invitations/${id}/decline`,
    notifications: '/api/v1/notifications',
    'notification-read': (id: number | string) => `/api/v1/notifications/${id}/read`,

    // orders
    orders: '/api/v1/orders',
    'order-detail': (id: number | string) => `/api/v1/orders/${id}`,
    'order-status': (id: number | string) => `/api/v1/orders/${id}/status`,
    'order-confirm': (id: number | string) => `/api/v1/orders/${id}/confirm`,
    'order-payments': (id: number | string) => `/api/v1/orders/${id}/payments`,

    // transactions
    transactions: '/api/v1/transactions',
    'transaction-detail': (id: number | string) => `/api/v1/transactions/${id}`,
    'transactions-by-order': (orderId: number | string) => `/api/v1/transactions/order/${orderId}`,
    'transactions-by-fund-account': (id: number | string) => `/api/v1/transactions/fund-account/${id}`,

    // fund accounts
    'fund-accounts': '/api/v1/fund-accounts',
    'fund-accounts-active': '/api/v1/fund-accounts/active',

    // attendance
    attendance: '/api/v1/attendance',
    'attendance-check-in': '/api/v1/attendance/check-in',
    'attendance-check-out': '/api/v1/attendance/check-out',
    'attendance-me-today': '/api/v1/attendance/me/today',
    'attendance-me-rosters-today': '/api/v1/attendance/me/rosters/today',
    'attendance-detail': (id: number | string) => `/api/v1/attendance/${id}`,

    // roster
    roster: '/api/v1/roster',
    'roster-detail': (id: number | string) => `/api/v1/roster/${id}`,
    'roster-by-employee': (employeeId: number | string) => `/api/v1/roster/employee/${employeeId}`,
    'roster-by-day': '/api/v1/roster/day',
    'roster-by-month': '/api/v1/roster/month',

    // payrolls
    payrolls: '/api/v1/payrolls',
    'payrolls-me': '/api/v1/payrolls/me',
    'payroll-detail': (id: number | string) => `/api/v1/payrolls/${id}`,
    'payroll-by-employee': (employeeId: number | string) => `/api/v1/payrolls/employee/${employeeId}`,
    'payroll-sync-monthly': '/api/v1/payrolls/sync-monthly',

    // import orders
    'import-orders': '/api/v1/import-orders',
    'import-order-detail': (id: number | string) => `/api/v1/import-orders/${id}`,
    'import-order-status': (id: number | string) => `/api/v1/import-orders/${id}/status`,

    // import return orders
    'import-return-orders': '/api/v1/import-return-orders',
    'import-return-order-detail': (id: number | string) => `/api/v1/import-return-orders/${id}`,

    // inventory logs
    'inventory-logs': '/api/v1/inventory-logs',
    'inventory-logs-by-product': (productId: number | string) => `/api/v1/inventory-logs/product/${productId}`,

    // inventory adjustments
    'inventory-adjustments': '/api/v1/inventory-adjustments',
    'inventory-adjustment-detail': (id: number | string) => `/api/v1/inventory-adjustments/${id}`,

    // device tokens
    'device-tokens': '/api/v1/device-tokens',
    'device-tokens-register': '/api/v1/device-tokens/register',
    'device-tokens-test-notification': '/api/v1/device-tokens/test-notification',

    // payment + webhook
    'payment-create': '/api/v1/payment/create',
    'webhook-sepay': '/api/v1/payment/webhook/sepay',
    'payment-status': (id: number | string) => `/api/v1/payment/orders/${id}/status`,

    // BFF aggregate endpoints
    'dashboard-today': '/api/v1/dashboard/today',
    'sales-init': '/api/v1/sales/init',
} as const;

const DEFAULT_BASE_URL = 'http://localhost:8080';

const getBaseUrl = (): string => {
    const envBase = import.meta.env.VITE_API_BASE_URL;
    return typeof envBase === 'string' && envBase.trim() ? envBase.trim() : DEFAULT_BASE_URL;
};

const safeJson = async (res: Response): Promise<unknown> => {
    const ct = res.headers.get('content-type') || '';
    if (ct.includes('application/json')) {
        try {
            return await res.json();
        } catch {
            return null;
        }
    }
    try {
        return await res.text();
    } catch {
        return null;
    }
};

const buildUrl = (baseUrl: string, path: string): string => {
    if (/^https?:\/\//i.test(path)) return path;
    const b = baseUrl.replace(/\/+$/, '');
    const p = path.startsWith('/') ? path : `/${path}`;
    return `${b}${p}`;
};

const isRecord = (v: unknown): v is Record<string, unknown> =>
    typeof v === 'object' && v !== null;

const getErrorMessage = (payload: unknown, status: number): string => {
    if (isRecord(payload)) {
        const message = payload.message;
        if (typeof message === 'string' && message) return message;
        const error = payload.error;
        if (typeof error === 'string' && error) return error;
        const data = payload.data;
        if (isRecord(data) && typeof data.message === 'string' && data.message) return data.message;
    }
    return `Request failed with status ${status}`;
};

export type ApiClient = {
    get<T = unknown>(path: string, init?: RequestInit): Promise<ApiResult<T>>;
    post<T = unknown>(path: string, body?: unknown, init?: RequestInit): Promise<ApiResult<T>>;
    put<T = unknown>(path: string, body?: unknown, init?: RequestInit): Promise<ApiResult<T>>;
    patch<T = unknown>(path: string, body?: unknown, init?: RequestInit): Promise<ApiResult<T>>;
    delete<T = unknown>(path: string, body?: unknown, init?: RequestInit): Promise<ApiResult<T>>;
};

type CreateApiClientOpts = {
    baseUrl?: string;
    getToken?: () => string | null | undefined;
    onUnauthorized?: (ctx: {
        retry: () => Promise<ApiResult<unknown>>;
        originalPath: string;
    }) => Promise<ApiResult<unknown>>;
};


async function request<T>(
    baseUrl: string,
    method: HttpMethod,
    path: string,
    body: unknown,
    init: RequestInit | undefined,
    token: string | null | undefined,
): Promise<ApiResult<T>> {
    const headers = new Headers(init?.headers || undefined);
    if (!headers.has('Accept')) headers.set('Accept', 'application/json');

    const isJsonBody =
        body !== undefined &&
        body !== null &&
        typeof body === 'object' &&
        !(body instanceof FormData) &&
        !(body instanceof Blob) &&
        !(body instanceof ArrayBuffer);

    if (isJsonBody && !headers.has('Content-Type')) {
        headers.set('Content-Type', 'application/json');
    }
    if (token && !headers.has('Authorization')) {
        headers.set('Authorization', `Bearer ${token}`);
    }
    const storeId = getStoredStoreId();
    if (storeId && !headers.has('X-Store-Id')) {
        headers.set('X-Store-Id', storeId);
    }

    const url = buildUrl(baseUrl, path);
    const res = await fetch(url, {
        ...init,
        method,
        headers,
        body:
            body === undefined || body === null
                ? undefined
                : isJsonBody
                    ? JSON.stringify(body)
                    : (body as BodyInit),
    });

    const payload = res.status === 204 ? null : await safeJson(res);
    const result: ApiResult<T> = {
        ok: res.ok,
        status: res.status,
        data: (payload as T) ?? null,
        headers: res.headers,
    };

    if (!res.ok) {
        throw new ApiError(getErrorMessage(payload, res.status), {
            status: res.status,
            data: payload,
            headers: res.headers,
        });
    }

    return result;
}

export const createApiClient = (opts: CreateApiClientOpts = {}): ApiClient => {
    const baseUrl = opts.baseUrl ?? getBaseUrl();
    const getToken = opts.getToken;

    async function doReq<T>(
        method: HttpMethod,
        path: string,
        body?: unknown,
        init?: RequestInit,
        retried = false,
    ): Promise<ApiResult<T>> {
        const token = getToken ? getToken() : null;
        try {
            return await request<T>(baseUrl, method, path, body, init, token);
        } catch (err) {
            if (!retried && err instanceof ApiError && err.response.status === 401 && opts.onUnauthorized) {
                const retry = () => doReq<unknown>(method, path, body, init, true);
                const res = await opts.onUnauthorized({ retry, originalPath: path });
                return res as ApiResult<T>;
            }
            throw err;
        }
    }

    return {
        get: (path, init) => doReq('GET', path, undefined, init),
        post: (path, body, init) => doReq('POST', path, body, init),
        put: (path, body, init) => doReq('PUT', path, body, init),
        patch: (path, body, init) => doReq('PATCH', path, body, init),
        delete: (path, body, init) => doReq('DELETE', path, body, init),
    };
};

export const getStoredAccessToken = (): string | null => {
    try {
        return localStorage.getItem(STORAGE_KEYS.accessToken);
    } catch {
        return null;
    }
};

export const getStoredRefreshToken = (): string | null => {
    try {
        return localStorage.getItem(STORAGE_KEYS.refreshToken);
    } catch {
        return null;
    }
};

export const getStoredUser = <T = unknown>(): T | null => {
    try {
        const raw = localStorage.getItem(STORAGE_KEYS.user);
        if (!raw) return null;
        return JSON.parse(raw) as T;
    } catch {
        return null;
    }
};

export const getStoredCurrentStore = <T = unknown>(): T | null => {
    try {
        const raw = localStorage.getItem(STORAGE_KEYS.currentStore);
        if (!raw) return null;
        return JSON.parse(raw) as T;
    } catch {
        return null;
    }
};

export const getStoredStoreId = (): string | null => {
    const store = getStoredCurrentStore<{ id?: number | string }>();
    if (store?.id === undefined || store.id === null) return null;
    return String(store.id);
};

export const clearStoredAuth = (): void => {
    try {
        localStorage.removeItem(STORAGE_KEYS.accessToken);
        localStorage.removeItem(STORAGE_KEYS.refreshToken);
        localStorage.removeItem(STORAGE_KEYS.user);
        localStorage.removeItem(STORAGE_KEYS.currentStore);
    } catch {
        // ignore
    }
};

export const storeAuthFromPayload = (payload: unknown): {
    accessToken: string | null;
    refreshToken: string | null;
    user: unknown | null;
    currentStore: unknown | null;
} => {
    const accessToken = extractToken(payload, 'accessToken');
    const refreshToken = extractToken(payload, 'refreshToken');
    const user = isRecord(payload)
        ? payload.user ?? (isRecord(payload.data) ? payload.data.user : null)
        : null;
    const currentStore = isRecord(payload)
        ? payload.currentStore ?? (isRecord(payload.data) ? payload.data.currentStore : null)
        : null;

    try {
        if (accessToken) localStorage.setItem(STORAGE_KEYS.accessToken, accessToken);
        if (refreshToken) localStorage.setItem(STORAGE_KEYS.refreshToken, refreshToken);
        if (user !== undefined) localStorage.setItem(STORAGE_KEYS.user, JSON.stringify(user ?? null));
        if (currentStore !== undefined) {
            localStorage.setItem(STORAGE_KEYS.currentStore, JSON.stringify(currentStore ?? null));
        }
    } catch {
        // ignore
    }

    return { accessToken, refreshToken, user: (user as unknown) ?? null, currentStore: (currentStore as unknown) ?? null };
};

const extractToken = (payload: unknown, key: 'accessToken' | 'refreshToken'): string | null => {
    if (!isRecord(payload)) return null;
    const direct = payload[key];
    if (typeof direct === 'string' && direct) return direct;
    const nested = payload.data;
    if (isRecord(nested)) {
        const value = nested[key];
        if (typeof value === 'string' && value) return value;
    }
    return null;
};

export const Apis = createApiClient();
export default Apis;

export const authApis = (token?: string | null): ApiClient => {
    return createApiClient({
        getToken: () => token ?? getStoredAccessToken(),
        onUnauthorized: async ({ retry, originalPath }) => {
            // Avoid refresh loops.
            if (originalPath === endpoints.refresh || originalPath === endpoints.logout || originalPath === endpoints.login) {
                emitAuthInvalid('unauthorized');
                throw new ApiError('Unauthorized', { status: 401, data: null, headers: new Headers() });
            }

            const refreshToken = getStoredRefreshToken();
            if (!refreshToken) {
                clearStoredAuth();
                emitAuthInvalid('missing_refresh_token');
                throw new ApiError('Missing refresh token', { status: 401, data: null, headers: new Headers() });
            }

            try {
                // Backend expects refresh token in Authorization header (Bearer refreshToken)
                const refreshClient = createApiClient({ getToken: () => refreshToken });
                const res = await refreshClient.post(endpoints.refresh);
                const newAccess = extractToken(res.data, 'accessToken');
                const newRefresh = extractToken(res.data, 'refreshToken');

                if (newAccess) {
                    try {
                        localStorage.setItem(STORAGE_KEYS.accessToken, newAccess);
                        if (newRefresh) localStorage.setItem(STORAGE_KEYS.refreshToken, newRefresh);
                    } catch {
                        // ignore
                    }
                    return await retry();
                }

                clearStoredAuth();
                emitAuthInvalid('refresh_failed');
                throw new ApiError('Refresh token failed', {
                    status: 401,
                    data: res.data,
                    headers: res.headers,
                });
            } catch (refreshErr) {
                clearStoredAuth();
                emitAuthInvalid('refresh_failed');
                throw refreshErr;
            }
        },
    });
};

