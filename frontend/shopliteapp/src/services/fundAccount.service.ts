import { authApis, endpoints } from '../utils/Apis';
import type { FundAccount } from '../api/types';

export const fundAccountService = {
    async getFundAccounts(): Promise<FundAccount[]> {
        const res = await authApis().get<any>(endpoints['fund-accounts']);
        const payload = res.data as any;
        return (payload?.data ?? payload) as FundAccount[];
    },

    async getActiveFundAccounts(): Promise<FundAccount[]> {
        const res = await authApis().get<any>(endpoints['fund-accounts-active']);
        const payload = res.data as any;
        return (payload?.data ?? payload) as FundAccount[];
    }
};
