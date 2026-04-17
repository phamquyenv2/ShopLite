import React, { createContext, useCallback, useEffect, useMemo, useState } from 'react';
import {
    ApiError,
    authApis,
    clearStoredAuth,
    createApiClient,
    endpoints,
    getStoredAccessToken,
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

    const refreshSession = useCallback(async () => {
        const accessToken = getStoredAccessToken();
        const refreshToken = getStoredRefreshToken();

        if (!accessToken || !refreshToken) {
            clearStoredAuth();
            setUser(null);
            setStatus('unauthenticated');
            return;
        }

        try {
            // Uses access token and can auto-refresh via authApis()
            await authApis().get(endpoints.me);
            const stored = getStoredUser<AuthUser>();
            setUser(stored);
            setStatus('authenticated');
        } catch {
            clearStoredAuth();
            setUser(null);
            setStatus('unauthenticated');
        }
    }, []);

    useEffect(() => {
        void refreshSession();
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
