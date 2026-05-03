export type AuthUser = {
    id: number;
    username: string;
    roleName: string;
};

export type MenuType = 'TAB' | 'SHORTCUT' | 'GROUP' | 'ITEM';

export type Menu = {
    id: number;
    code: string;
    title: string;
    route?: string | null;
    icon?: string | null;
    menuType: MenuType;
    parentId?: number | null;
    sortOrder?: number | null;
};

export type MeStore = {
    id: number;
    name: string;
    memberRole: string;
    membershipStatus: string;
    permissions?: {
        id: number;
        name: string;
        apiPath: string;
        method: string;
        module: string;
    }[];
    menus?: Menu[];
};

export type MeResponse = {
    user: {
        id: number;
        username: string;
        phone: string;
        globalRole: string | null;
    };
    currentStore: MeStore | null;
    stores: MeStore[];
};

export type LoginResponse = {
    accessToken?: string;
    refreshToken?: string;
    user?: AuthUser;
    currentStore?: MeStore | null;
    data?: LoginResponse;
};

export type OtpSendResponse = {
    message: string;
    phone: string;
    expiresIn: number;
    resendAfter: number;
};

export type OtpVerifyResponse = {
    registerSessionId: string;
    phone: string;
    expiresIn: number;
};
