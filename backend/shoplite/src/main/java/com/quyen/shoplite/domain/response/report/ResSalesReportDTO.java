package com.quyen.shoplite.domain.response.report;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResSalesReportDTO {
    private double totalRevenue;
    private int totalOrders;
    private double totalDiscount;
    private double netRevenue;
    private double avgOrderValue;
    private double returnAmount;
    private double growth;

    private List<RevenuePointDTO> chartData;
    private List<TopCategoryDTO> topCategories;
    private List<RecentOrderDTO> recentOrders;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RevenuePointDTO {
        private String label;
        private double value;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TopCategoryDTO {
        private String name;
        private double revenue;
        private int orders;
        private double pct;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RecentOrderDTO {
        private String code;
        private String customer;
        private double amount;
        private String status;
        private String time;
    }
}
