import { authApis, endpoints } from '../utils/Apis';
import type { Transaction } from '../api/types';

export const transactionService = {
    async getTransactions(): Promise<Transaction[]> {
        const res = await authApis().get<any>(endpoints.transactions);
        const payload = res.data as any;
        return (payload?.data ?? payload) as Transaction[];
    },

    async getTransactionsByFundAccount(fundAccountId: number): Promise<Transaction[]> {
        const res = await authApis().get<any>(endpoints['transactions-by-fund-account'](fundAccountId));
        const payload = res.data as any;
        return (payload?.data ?? payload) as Transaction[];
    }
};
