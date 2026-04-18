import React, { createContext, useCallback, useEffect, useMemo, useRef, useState } from 'react';
import {
    ApiError,
    AUTH_INVALID_EVENT,
    authApis,
    clearStoredAuth,
    createApiClient,
    endpoints,
    getStoredRefreshToken,
    getStoredUser,
    storeAuthFromPayload,
} from '../utils/Apis';
import type { AuthUser, LoginResponse } from './types';

export type AuthStatus = 'checking' | 'authenticated' | 'unauthenticated';

export type AuthContextValue = {
    status: AuthStatus;
    user: AuthUser | null;
    login: (phone: string, password: string) => Promise<void>;
    register: (username: string, phone: string, password: string) => Promise<void>;
    logout: () => Promise<void>;
    refreshSession: () => Promise<void>;
};

const AuthContext = createContext<AuthContextValue | null>(null);

export const AuthProvider: React.FC<React.PropsWithChildren> = ({ children }) => {
    const [status, setStatus] = useState<AuthStatus>('checking');
    const [user, setUser] = useState<AuthUser | null>(() => getStoredUser<AuthUser>());
    const refreshingRef = useRef(false);

    useEffect(() => {
        const handler = () => {
            setUser(null);
            setStatus('unauthenticated');
        };

        globalThis.addEventListener(AUTH_INVALID_EVENT, handler as EventListener);
        return () => {
            globalThis.removeEventListener(AUTH_INVALID_EVENT, handler as EventListener);
        };
    }, []);

    const refreshSession = useCallback(async () => {
        const refreshToken = getStoredRefreshToken();

        if (!refreshToken) {
            clearStoredAuth();
            setUser(null);
            setStatus('unauthenticated');
            return;
        }

        if (refreshingRef.current) return;
        refreshingRef.current = true;

        try {
            // Always validate session by refreshing with refresh token.
            // This ensures we also respect DB-level refresh token expiry.
            const refreshClient = createApiClient({ getToken: () => refreshToken });
            const res = await refreshClient.post<LoginResponse>(endpoints.refresh);
            const stored = storeAuthFromPayload(res.data);

            if (!stored.accessToken || !stored.refreshToken) {
                clearStoredAuth();
                setUser(null);
                setStatus('unauthenticated');
                return;
            }

            setUser((stored.user as AuthUser | null) ?? getStoredUser<AuthUser>());
            setStatus('authenticated');
        } catch {
            clearStoredAuth();
            setUser(null);
            setStatus('unauthenticated');
        } finally {
            refreshingRef.current = false;
        }
    }, []);

    useEffect(() => {
        void refreshSession();
    }, [refreshSession]);

    useEffect(() => {
        const onFocus = () => void refreshSession();
        const onVisibility = () => {
            if (document.visibilityState === 'visible') void refreshSession();
        };

        globalThis.addEventListener('focus', onFocus);
        document.addEventListener('visibilitychange', onVisibility);
        return () => {
            globalThis.removeEventListener('focus', onFocus);
            document.removeEventListener('visibilitychange', onVisibility);
        };
    }, [refreshSession]);

    const login = useCallback(async (phone: string, password: string) => {
        const client = createApiClient();
        const res = await client.post<LoginResponse>(endpoints.login, { phone, password });
        const stored = storeAuthFromPayload(res.data);

        if (!stored.accessToken || !stored.refreshToken) {
            clearStoredAuth();
            setUser(null);
            setStatus('unauthenticated');
            throw new ApiError('Login response missing tokens', {
                status: res.status,
                data: res.data,
                headers: res.headers,
            });
        }

        setUser((stored.user as AuthUser | null) ?? getStoredUser<AuthUser>());
        setStatus('authenticated');
    }, []);

    const register = useCallback(async (username: string, phone: string, password: string) => {
        const client = createApiClient();
        const res = await client.post<LoginResponse>(endpoints.register, { username, phone, password });
        const stored = storeAuthFromPayload(res.data);

        if (!stored.accessToken || !stored.refreshToken) {
            clearStoredAuth();
            setUser(null);
            setStatus('unauthenticated');
            throw new ApiError('Register response missing tokens', {
                status: res.status,
                data: res.data,
                headers: res.headers,
            });
        }

        setUser((stored.user as AuthUser | null) ?? getStoredUser<AuthUser>());
        setStatus('authenticated');
    }, []);

    const logout = useCallback(async () => {
        const refreshToken = getStoredRefreshToken();
        try {
            if (refreshToken) {
                const refreshClient = createApiClient({ getToken: () => refreshToken });
                await refreshClient.post(endpoints.logout);
            }
        } catch {
            // ignore logout errors
        } finally {
            clearStoredAuth();
            setUser(null);
            setStatus('unauthenticated');
        }
    }, []);

    const value = useMemo<AuthContextValue>(
        () => ({ status, user, login, register, logout, refreshSession }),
        [status, user, login, register, logout, refreshSession],
    );

    return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
};

export default AuthContext;
