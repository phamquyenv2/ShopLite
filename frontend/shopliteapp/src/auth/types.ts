export type AuthUser = {
    id: number;
    username: string;
    roleName: string;
};

export type MeStore = {
    id: number;
    name: string;
    memberRole: string;
    membershipStatus: string;
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
