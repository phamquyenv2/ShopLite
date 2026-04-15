export type AuthUser = {
    id: number;
    username: string;
    roleName: string;
};

export type LoginResponse = {
    accessToken: string;
    refreshToken: string;
    user: AuthUser;
};
