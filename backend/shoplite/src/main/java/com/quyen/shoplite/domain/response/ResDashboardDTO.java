package com.quyen.shoplite.domain.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResDashboardDTO {
    private ResMeDTO.UserInfo user;
    private ResMeDTO.StoreInfo currentStore;
    private TodayStats todayStats;
    private List<ResNotificationDTO> notifications;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TodayStats {
        private int orderCount;
        private double revenue;
        private double profit;
        private List<ResOrderDTO> recentOrders;
    }
}
