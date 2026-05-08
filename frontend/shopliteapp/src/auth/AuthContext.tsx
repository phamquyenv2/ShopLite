import React, { createContext, useCallback, useEffect, useMemo, useRef, useState } from 'react';
import {
    ApiError,
    AUTH_INVALID_EVENT,
    authApis,
    clearStoredAuth,
    createApiClient,
    endpoints,
    getStoredAccessToken,
    getStoredRefreshToken,
    getStoredUser,
    storeAuthFromPayload,
} from '../utils/Apis';
import type { AuthUser, LoginResponse, OtpSendResponse, OtpVerifyResponse } from './types';
import { getFcmRegistrationToken } from '../utils/fcmOtp';
import { clearMeCache, getCurrentMe } from '../utils/meSession';

export type AuthStatus = 'checking' | 'authenticated' | 'unauthenticated';

export type AuthContextValue = {
    status: AuthStatus;
    user: AuthUser | null;
    login: (phone: string, password: string) => Promise<void>;
    register: (username: string, phone: string, password: string) => Promise<void>;
    logout: () => Promise<void>;
    refreshSession: () => Promise<void>;
    // OTP Registration Flow
    sendOtp: (phone: string) => Promise<OtpSendResponse>;
    verifyOtp: (phone: string, otp: string) => Promise<OtpVerifyResponse>;
    setStoreName: (sessionId: string, storeName: string) => Promise<void>;
    completeRegister: (sessionId: string, password: string) => Promise<void>;
};

const AuthContext = createContext<AuthContextValue | null>(null);

// ─── Helpers ──────────────────────────────────────────────────────────────────

/**
 * Decode JWT payload (base64url) mà không cần verify signature.
 * Trả về null nếu token không hợp lệ.
 */
const decodeJwtPayload = (token: string): Record<string, unknown> | null => {
    try {
        const parts = token.split('.');
        if (parts.length !== 3) return null;
        const base64 = parts[1].replace(/-/g, '+').replace(/_/g, '/');
        const json = atob(base64);
        return JSON.parse(json) as Record<string, unknown>;
    } catch {
        return null;
    }
};

/**
 * Trả về true nếu access token trong localStorage còn hợp lệ
 * (exp > now + bufferSeconds). Buffer mặc định 60s để proactively
 * refresh trước khi thật sự hết hạn.
 */
const isAccessTokenValid = (bufferSeconds = 60): boolean => {
    const token = getStoredAccessToken();
    if (!token) return false;
    const payload = decodeJwtPayload(token);
    if (!payload || typeof payload.exp !== 'number') return false;
    const expiresAtMs = payload.exp * 1000;
    return Date.now() < expiresAtMs - bufferSeconds * 1000;
};

// ──────────────────────────────────────────────────────────────────────────────

export const AuthProvider: React.FC<React.PropsWithChildren> = ({ children }) => {
    const [status, setStatus] = useState<AuthStatus>('checking');
    const [user, setUser] = useState<AuthUser | null>(() => getStoredUser<AuthUser>());
    const refreshingRef = useRef(false);

    const warmMeCache = useCallback((force = false) => {
        void getCurrentMe({ force }).catch(() => {
            // Protected pages still handle auth/API errors through their own requests.
        });
    }, []);

    useEffect(() => {
        const handler = () => {
            clearMeCache();
            setUser(null);
            setStatus('unauthenticated');
        };

        globalThis.addEventListener(AUTH_INVALID_EVENT, handler as EventListener);
        return () => {
            globalThis.removeEventListener(AUTH_INVALID_EVENT, handler as EventListener);
        };
    }, []);

    const refreshSession = useCallback(async () => {
        // ── Bước 1: Decode access token trong localStorage ────────────────────────
        // Nếu còn hợp lệ → restore session từ storage mà KHÔNG gọi API.
        // Đây là trường hợp phổ biến nhất khi focus/visibilitychange xảy ra
        // ngay sau khi vừa login, tránh rotate refresh token không cần thiết.
        if (isAccessTokenValid()) {
            const storedUser = getStoredUser<AuthUser>();
            if (storedUser) {
                setUser(storedUser);
                setStatus('authenticated');
                warmMeCache();
                return;
            }
        }

        // ── Bước 2: Access token hết hạn / không có → cần gọi POST /refresh ──────
        const refreshToken = getStoredRefreshToken();
        if (!refreshToken) {
            clearStoredAuth();
            clearMeCache();
            setUser(null);
            setStatus('unauthenticated');
            return;
        }

        // Guard chống concurrent calls (e.g. focus + visibilitychange cùng lúc)
        if (refreshingRef.current) return;
        refreshingRef.current = true;

        try {
            const refreshClient = createApiClient({ getToken: () => refreshToken });
            const res = await refreshClient.post<LoginResponse>(endpoints.refresh);
            const stored = storeAuthFromPayload(res.data);

            if (!stored.accessToken || !stored.refreshToken) {
                clearStoredAuth();
                clearMeCache();
                setUser(null);
                setStatus('unauthenticated');
                return;
            }

            setUser((stored.user as AuthUser | null) ?? getStoredUser<AuthUser>());
            setStatus('authenticated');
            warmMeCache(true);
        } catch {
            clearStoredAuth();
            clearMeCache();
            setUser(null);
            setStatus('unauthenticated');
        } finally {
            refreshingRef.current = false;
        }
    }, [warmMeCache]);

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
            clearMeCache();
            setUser(null);
            setStatus('unauthenticated');
            throw new ApiError('Login response missing tokens', {
                status: res.status,
                data: res.data,
                headers: res.headers,
            });
        }

        clearMeCache();
        setUser((stored.user as AuthUser | null) ?? getStoredUser<AuthUser>());
        setStatus('authenticated');
        warmMeCache(true);
    }, [warmMeCache]);

    const register = useCallback(async (username: string, phone: string, password: string) => {
        const client = createApiClient();
        const res = await client.post<LoginResponse>(endpoints.register, { username, phone, password });
        const stored = storeAuthFromPayload(res.data);

        if (!stored.accessToken || !stored.refreshToken) {
            clearStoredAuth();
            clearMeCache();
            setUser(null);
            setStatus('unauthenticated');
            throw new ApiError('Register response missing tokens', {
                status: res.status,
                data: res.data,
                headers: res.headers,
            });
        }

        clearMeCache();
        setUser((stored.user as AuthUser | null) ?? getStoredUser<AuthUser>());
        setStatus('authenticated');
        warmMeCache(true);
    }, [warmMeCache]);

    // ─── OTP Registration Flow ────────────────────────────────────────────────

    const sendOtp = useCallback(async (phone: string): Promise<OtpSendResponse> => {
        const client = createApiClient();
        const fcmToken = await getFcmRegistrationToken();
        const res = await client.post<{ data: OtpSendResponse }>(endpoints['register-otp-send'], {
            phone,
            ...(fcmToken ? { fcmToken } : {}),
        });
        const payload = res.data as unknown as { data?: OtpSendResponse } & OtpSendResponse;
        return payload?.data ?? payload;
    }, []);

    const verifyOtp = useCallback(async (phone: string, otp: string): Promise<OtpVerifyResponse> => {
        const client = createApiClient();
        const res = await client.post<{ data: OtpVerifyResponse }>(endpoints['register-otp-verify'], { phone, otp });
        const payload = res.data as unknown as { data?: OtpVerifyResponse } & OtpVerifyResponse;
        return payload?.data ?? payload;
    }, []);

    const setStoreName = useCallback(async (sessionId: string, storeName: string): Promise<void> => {
        const client = createApiClient();
        await client.post(endpoints['register-store'], {
            registerSessionId: sessionId,
            storeName,
        });
    }, []);

    const completeRegister = useCallback(async (sessionId: string, password: string): Promise<void> => {
        const client = createApiClient();
        const res = await client.post<LoginResponse>(endpoints['register-complete'], {
            registerSessionId: sessionId,
            password,
        });
        const stored = storeAuthFromPayload(res.data);

        if (!stored.accessToken || !stored.refreshToken) {
            clearStoredAuth();
            clearMeCache();
            setUser(null);
            setStatus('unauthenticated');
            throw new ApiError('Register complete response missing tokens', {
                status: res.status,
                data: res.data,
                headers: res.headers,
            });
        }

        clearMeCache();
        setUser((stored.user as AuthUser | null) ?? getStoredUser<AuthUser>());
        setStatus('authenticated');
        warmMeCache(true);
    }, [warmMeCache]);

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
            clearMeCache();
            setUser(null);
            setStatus('unauthenticated');
        }
    }, []);

    const value = useMemo<AuthContextValue>(
        () => ({ status, user, login, register, logout, refreshSession,
                 sendOtp, verifyOtp, setStoreName, completeRegister }),
        [status, user, login, register, logout, refreshSession,
         sendOtp, verifyOtp, setStoreName, completeRegister],
    );

    return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
};

export default AuthContext;
