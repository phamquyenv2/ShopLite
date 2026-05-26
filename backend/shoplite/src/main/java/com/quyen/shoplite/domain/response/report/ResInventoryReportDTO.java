package com.quyen.shoplite.domain.response.report;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResInventoryReportDTO {
    private int totalSku;
    private int totalStock;
    private double totalValue;
    private int lowStockCount;
    private int outOfStockCount;
    private double newImportValue;
    private int soldUnits;

    private List<LowStockItemDTO> lowStockItems;
    private List<MovementItemDTO> movements;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class LowStockItemDTO {
        private String name;
        private String sku;
        private int stock;
        private int minStock;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class MovementItemDTO {
        private String name;
        private int sold;
        private int imported;
        private int adjusted;
        private int currentStock;
    }
}
