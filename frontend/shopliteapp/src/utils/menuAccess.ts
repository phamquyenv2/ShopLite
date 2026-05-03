import {
    archiveOutline,
    arrowUndoOutline,
    bagHandleOutline,
    bagOutline,
    calendarOutline,
    cartOutline,
    clipboardOutline,
    cubeOutline,
    documentTextOutline,
    gridOutline,
    homeOutline,
    logInOutline,
    peopleOutline,
    personOutline,
    receiptOutline,
    reorderThreeOutline,
    settingsOutline,
    storefrontOutline,
    timeOutline,
    walletOutline,
} from 'ionicons/icons';
import type { Menu, MenuType, Permission } from '../api/types';
import { hasPermission } from './permissions';

const iconMap: Record<string, string> = {
    archiveOutline,
    arrowUndoOutline,
    bagHandleOutline,
    bagOutline,
    calendarOutline,
    cartOutline,
    clipboardOutline,
    cubeOutline,
    documentTextOutline,
    gridOutline,
    homeOutline,
    logInOutline,
    peopleOutline,
    personOutline,
    receiptOutline,
    reorderThreeOutline,
    settingsOutline,
    storefrontOutline,
    timeOutline,
    walletOutline,
};

const titleMap: Record<string, string> = {
    MENU_HOME: 'Tổng quan',
    MENU_PRODUCTS: 'Hàng hóa',
    MENU_SALES: 'Bán hàng',
    MENU_ORDERS: 'Hóa đơn',
    MENU_MORE: 'Khác',
    SHORTCUT_SALES: 'Tạo đơn',
    SHORTCUT_PRODUCTS: 'Sản phẩm',
    SHORTCUT_EMPLOYEES: 'Nhân viên',
    SHORTCUT_IMPORT_ORDERS: 'Nhập hàng',
    GROUP_TRANSACTION: 'Giao dịch',
    GROUP_PRODUCTS: 'Hàng hóa',
    GROUP_PARTNERS: 'Đối tác',
    GROUP_EMPLOYEES: 'Nhân viên',
    GROUP_SETTINGS: 'Cài đặt chung',
    ITEM_SALES: 'Bán hàng',
    ITEM_ORDERS: 'Hóa đơn',
    ITEM_ORDER_CREATE: 'Đặt hàng',
    ITEM_FUND_LEDGER: 'Sổ quỹ',
    ITEM_PRODUCTS: 'Hàng hóa',
    ITEM_INVENTORY_ADJUSTMENTS: 'Kiểm kho',
    ITEM_IMPORT_ORDERS: 'Nhập hàng',
    ITEM_IMPORT_RETURN_ORDERS: 'Trả hàng nhập',
    ITEM_CUSTOMERS: 'Khách hàng',
    ITEM_SUPPLIERS: 'Nhà cung cấp',
    ITEM_EMPLOYEES: 'Nhân viên',
    ITEM_ROSTER: 'Lịch làm việc',
    ITEM_ATTENDANCE: 'Chấm công',
    ITEM_PAYROLLS: 'Bảng lương',
    ITEM_ROLE_MANAGEMENT: 'Quản lý người dùng',
};

export const getMenuIcon = (icon?: string | null, fallback = gridOutline) =>
    icon ? iconMap[icon] ?? fallback : fallback;

export const getMenuTitle = (menu: Menu): string =>
    titleMap[menu.code] || menu.title;

export const hasMenuPayload = (menus?: Menu[]): menus is Menu[] =>
    Array.isArray(menus) && menus.length > 0;

export const getMenusByType = (menus: Menu[] | undefined, menuType: MenuType): Menu[] =>
    (menus || [])
        .filter(menu => menu.menuType === menuType)
        .sort((a, b) => (a.sortOrder ?? 0) - (b.sortOrder ?? 0));

export const hasMenuCode = (menus: Menu[] | undefined, code: string): boolean =>
    hasMenuPayload(menus) && menus.some(menu => menu.code === code);

export const canShowMenu = (
    menus: Menu[] | undefined,
    code: string,
    permissions: Permission[] | undefined,
    apiPath: string,
    method = 'GET',
): boolean => {
    if (hasMenuPayload(menus)) {
        return hasMenuCode(menus, code);
    }
    return hasPermission(permissions, apiPath, method);
};
