import { useCallback, useEffect, useMemo, useState } from 'react';
import type { Permission } from '../api/types';
import type { MeStore } from '../auth/types';
import { getCurrentMe, readStoredCurrentStore } from './meSession';
import { hasPermission } from './permissions';

let permissionsRefreshPromise: Promise<Permission[]> | null = null;
export const STORE_PERMISSIONS_REFRESH_EVENT = 'shoplite:store-permissions-refresh';

const readStoredPermissions = (): Permission[] =>
    (readStoredCurrentStore()?.permissions ?? []) as Permission[];

export const refreshStoredPermissions = async (): Promise<Permission[]> => {
    if (!permissionsRefreshPromise) {
        permissionsRefreshPromise = getCurrentMe({ force: true })
            .then(me => {
                const currentStore: MeStore | null = me.currentStore ?? null;
                return (currentStore?.permissions ?? []) as Permission[];
            })
            .finally(() => {
                permissionsRefreshPromise = null;
            });
    }

    return permissionsRefreshPromise;
};

export const requestStorePermissionsRefresh = () => {
    void refreshStoredPermissions().catch(() => {
        // ignore refresh errors; mounted hooks handle their own state fallback
    });

    try {
        globalThis.dispatchEvent(new CustomEvent(STORE_PERMISSIONS_REFRESH_EVENT));
    } catch {
        // ignore event dispatch errors
    }
};

export const useStorePermissions = () => {
    const [permissions, setPermissions] = useState<Permission[]>(() => readStoredPermissions());

    const refreshPermissions = useCallback(() => {
        refreshStoredPermissions()
            .then(nextPermissions => {
                setPermissions(nextPermissions);
            })
            .catch(() => {
                setPermissions([]);
            });
    }, []);

    useEffect(() => {
        if (permissions.length > 0) return;
        refreshPermissions();
    }, [permissions.length, refreshPermissions]);

    useEffect(() => {
        const handler = () => refreshPermissions();
        globalThis.addEventListener(STORE_PERMISSIONS_REFRESH_EVENT, handler);

        return () => {
            globalThis.removeEventListener(STORE_PERMISSIONS_REFRESH_EVENT, handler);
        };
    }, [refreshPermissions]);

    const can = useCallback(
        (apiPath: string, method = 'GET') =>
            hasPermission(permissions, apiPath, method),
        [permissions],
    );

    const canAny = useCallback(
        (...checks: Array<[apiPath: string, method?: string]>) =>
            checks.some(([apiPath, method = 'GET']) => can(apiPath, method)),
        [can],
    );

    return useMemo(() => ({ permissions, can, canAny }), [permissions, can, canAny]);
};
