import type { Permission } from '../api/types';

export const hasPermission = (
    permissions: Permission[] | undefined,
    apiPath: string,
    method = 'GET',
): boolean => {
    if (!permissions || permissions.length === 0) return false;
    return permissions.some(p => p.apiPath === apiPath && p.method.toUpperCase() === method.toUpperCase());
};

export const hasModule = (permissions: Permission[] | undefined, module: string): boolean => {
    if (!permissions || permissions.length === 0) return false;
    return permissions.some(p => p.module === module);
};
