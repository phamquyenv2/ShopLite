import type { MeResponse, MeStore } from '../auth/types';
import { authApis, endpoints, getStoredCurrentStore, STORAGE_KEYS } from './Apis';

const ME_CACHE_TTL_MS = 5 * 60 * 1000;

let cachedMe: MeResponse | null = null;
let cachedAt = 0;
let mePromise: Promise<MeResponse> | null = null;

export const ME_SESSION_UPDATED_EVENT = 'shoplite:me-session-updated';

const unwrapMePayload = (payload: unknown): MeResponse => {
    const wrapped = payload as { data?: MeResponse };
    return (wrapped?.data ?? payload) as MeResponse;
};

const emitMeSessionUpdated = (currentStore: MeStore | null | undefined) => {
    try {
        globalThis.dispatchEvent(new CustomEvent(ME_SESSION_UPDATED_EVENT, { detail: { currentStore: currentStore ?? null } }));
    } catch {
        // ignore event errors
    }
};

const storeCurrentStore = (currentStore: MeStore | null | undefined) => {
    try {
        localStorage.setItem(STORAGE_KEYS.currentStore, JSON.stringify(currentStore ?? null));
    } catch {
        // ignore storage errors
    }
    emitMeSessionUpdated(currentStore);
};

export const readStoredCurrentStore = (): MeStore | null =>
    getStoredCurrentStore<MeStore>();

export const getCurrentMe = async (opts: { force?: boolean; maxAgeMs?: number } = {}): Promise<MeResponse> => {
    const maxAgeMs = opts.maxAgeMs ?? ME_CACHE_TTL_MS;
    const freshMe = cachedMe && Date.now() - cachedAt < maxAgeMs ? cachedMe : null;

    if (!opts.force && freshMe) {
        return freshMe;
    }

    if (!mePromise) {
        mePromise = authApis()
            .get<unknown>(endpoints.me)
            .then(res => {
                const me = unwrapMePayload(res.data);
                cachedMe = me;
                cachedAt = Date.now();
                storeCurrentStore(me.currentStore ?? null);
                return me;
            })
            .finally(() => {
                mePromise = null;
            });
    }

    return mePromise;
};

export const clearMeCache = () => {
    cachedMe = null;
    cachedAt = 0;
    mePromise = null;
};
