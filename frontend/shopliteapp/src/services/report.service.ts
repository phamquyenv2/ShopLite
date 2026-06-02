import { authApis, endpoints } from '../utils/Apis';

export interface TopProductDTO {
  name: string;
  qty: number;
  revenue: number;
}

export interface ResEndOfDayReportDTO {
  totalRevenue: number;
  totalOrders: number;
  totalProducts: number;
  totalDiscount: number;
  totalRefund: number;
  netRevenue: number;
  avgOrderValue: number;
  newCustomers: number;
  cashAmount: number;
  bankAmount: number;
  ewalletAmount: number;
  topProducts: TopProductDTO[];
}

export interface RevenuePointDTO {
  label: string;
  value: number;
}

export interface TopCategoryDTO {
  name: string;
  revenue: number;
  orders: number;
  pct: number;
}

export interface RecentOrderDTO {
  code: string;
  customer: string;
  amount: number;
  status: string;
  time: string;
}

export interface ResSalesReportDTO {
  totalRevenue: number;
  totalOrders: number;
  totalDiscount: number;
  netRevenue: number;
  avgOrderValue: number;
  returnAmount: number;
  growth: number;
  chartData: RevenuePointDTO[];
  topCategories: TopCategoryDTO[];
  recentOrders: RecentOrderDTO[];
}

export interface LowStockItemDTO {
  productId: number;
  name: string;
  sku: string;
  stock: number;
  minStock: number;
  importPrice?: number;
  imageUrl?: string;
}

export interface MovementItemDTO {
  name: string;
  sold: number;
  imported: number;
  adjusted: number;
  currentStock: number;
}

export interface ResInventoryReportDTO {
  totalSku: number;
  totalStock: number;
  totalValue: number;
  lowStockCount: number;
  outOfStockCount: number;
  newImportValue: number;
  soldUnits: number;
  lowStockItems: LowStockItemDTO[];
  movements: MovementItemDTO[];
}

export const reportService = {
  getEndOfDayReport: async (storeId: number, from: string, to: string): Promise<ResEndOfDayReportDTO | null> => {
    try {
      const response = await authApis().get<ResEndOfDayReportDTO>(
        `${endpoints['reports-end-of-day']}?storeId=${storeId}&from=${from}&to=${to}`
      );
      const payload: any = response.data;
      return payload?.data ?? payload;
    } catch (err) {
      console.error('[ReportService] getEndOfDayReport error:', err);
      return null;
    }
  },

  getSalesReport: async (storeId: number, period: string, from: string, to: string): Promise<ResSalesReportDTO | null> => {
    try {
      const response = await authApis().get<ResSalesReportDTO>(
        `${endpoints['reports-sales']}?storeId=${storeId}&period=${period}&from=${from}&to=${to}`
      );
      const payload: any = response.data;
      return payload?.data ?? payload;
    } catch (err) {
      console.error('[ReportService] getSalesReport error:', err);
      return null;
    }
  },

  getInventoryReport: async (storeId: number, from: string, to: string): Promise<ResInventoryReportDTO | null> => {
    try {
      const response = await authApis().get<ResInventoryReportDTO>(
        `${endpoints['reports-inventory']}?storeId=${storeId}&from=${from}&to=${to}`
      );
      const payload: any = response.data;
      return payload?.data ?? payload;
    } catch (err) {
      console.error('[ReportService] getInventoryReport error:', err);
      return null;
    }
  }
};
