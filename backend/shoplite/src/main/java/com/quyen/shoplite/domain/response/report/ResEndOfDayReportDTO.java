package com.quyen.shoplite.domain.response.report;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResEndOfDayReportDTO {
    private double totalRevenue;
    private int totalOrders;
    private int totalProducts;
    private double totalDiscount;
    private double totalRefund;
    private double netRevenue;
    private double avgOrderValue;
    private int newCustomers;
    private double cashAmount;
    private double bankAmount;
    private double ewalletAmount;

    private List<TopProductDTO> topProducts;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TopProductDTO {
        private String name;
        private int qty;
        private double revenue;
    }
}
