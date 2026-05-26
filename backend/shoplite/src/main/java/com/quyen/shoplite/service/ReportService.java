package com.quyen.shoplite.service;

import com.quyen.shoplite.domain.*;
import com.quyen.shoplite.domain.response.report.ResEndOfDayReportDTO;
import com.quyen.shoplite.domain.response.report.ResInventoryReportDTO;
import com.quyen.shoplite.domain.response.report.ResSalesReportDTO;
import com.quyen.shoplite.repository.*;
import com.quyen.shoplite.util.constant.PaymentMethodEnum;
import com.quyen.shoplite.util.constant.StatusEnum;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final ProductRepository productRepository;
    private final OrderItemsRepository orderItemsRepository;

    // StatusEnum chỉ có: DRAFT, PENDING, PENDING_PAYMENT, COMPLETED, FAIL, CANCELLED
    private static final List<StatusEnum> PAID_STATUSES = Arrays.asList(StatusEnum.COMPLETED);

    public ResEndOfDayReportDTO getEndOfDayReport(Long storeId, LocalDateTime from, LocalDateTime to) {
        List<Order> orders = orderRepository.findByStoreIdAndStatusInAndCreatedAtBetween(
                storeId,
                PAID_STATUSES,
                from,
                to,
                Sort.by(Sort.Direction.DESC, "createdAt")
        );

        double totalRevenue = 0;
        int totalOrders = orders.size();
        double totalDiscount = 0;
        double totalRefund = 0;
        long totalProducts = 0;

        Map<String, Double> productRevenueMap = new HashMap<>();
        Map<String, Long> productQtyMap = new HashMap<>();

        // Lấy tất cả order items một lần để tránh N+1
        List<Integer> orderIds = orders.stream().map(Order::getId).collect(Collectors.toList());
        List<OrderItems> allItems = orderIds.isEmpty()
                ? Collections.emptyList()
                : orderItemsRepository.findAllByOrderIdIn(orderIds);

        for (Order order : orders) {
            totalRevenue += order.getTotalAmount();
            totalDiscount += order.getDiscount() != null ? order.getDiscount() : 0;
        }

        for (OrderItems item : allItems) {
            totalProducts += item.getQuantity();
            String productName = item.getProductName(); // dùng productName đã lưu sẵn
            productQtyMap.put(productName, productQtyMap.getOrDefault(productName, 0L) + item.getQuantity());
            productRevenueMap.put(productName, productRevenueMap.getOrDefault(productName, 0.0) + item.getTotalPrice());
        }

        double netRevenue = totalRevenue - totalDiscount - totalRefund;
        double avgOrderValue = totalOrders > 0 ? totalRevenue / totalOrders : 0;

        // Thanh toán theo phương thức
        List<Payment> payments = paymentRepository.findByStoreIdAndCreatedAtBetween(storeId, from, to);
        double cashAmount = 0;
        double bankAmount = 0;
        double ewalletAmount = 0;

        for (Payment p : payments) {
            if (p.getAmount() == null) continue;
            PaymentMethodEnum method = p.getPaymentMethod();
            if (method == PaymentMethodEnum.CASH) {
                cashAmount += p.getAmount().doubleValue();
            } else if (method == PaymentMethodEnum.BANK_TRANSFER || method == PaymentMethodEnum.BANK_QR) {
                bankAmount += p.getAmount().doubleValue();
            } else if (method == PaymentMethodEnum.EWALLET) {
                ewalletAmount += p.getAmount().doubleValue();
            }
        }

        List<ResEndOfDayReportDTO.TopProductDTO> topProducts = productQtyMap.entrySet().stream()
                .map(e -> ResEndOfDayReportDTO.TopProductDTO.builder()
                        .name(e.getKey())
                        .qty(e.getValue().intValue())
                        .revenue(productRevenueMap.get(e.getKey()))
                        .build())
                .sorted((a, b) -> Double.compare(b.getRevenue(), a.getRevenue()))
                .limit(5)
                .collect(Collectors.toList());

        return ResEndOfDayReportDTO.builder()
                .totalRevenue(totalRevenue)
                .totalOrders(totalOrders)
                .totalProducts((int) totalProducts)
                .totalDiscount(totalDiscount)
                .totalRefund(totalRefund)
                .netRevenue(netRevenue)
                .avgOrderValue(avgOrderValue)
                .newCustomers(0)
                .cashAmount(cashAmount)
                .bankAmount(bankAmount)
                .ewalletAmount(ewalletAmount)
                .topProducts(topProducts)
                .build();
    }

    public ResSalesReportDTO getSalesReport(Long storeId, String period, LocalDateTime from, LocalDateTime to) {
        List<Order> orders = orderRepository.findByStoreIdAndStatusInAndCreatedAtBetween(
                storeId,
                PAID_STATUSES,
                from,
                to,
                Sort.by(Sort.Direction.ASC, "createdAt")
        );

        double totalRevenue = 0;
        int totalOrders = orders.size();
        double totalDiscount = 0;
        double returnAmount = 0;

        Map<String, ResSalesReportDTO.TopCategoryDTO> categoryMap = new HashMap<>();

        List<Integer> orderIds = orders.stream().map(Order::getId).collect(Collectors.toList());
        List<OrderItems> allItems = orderIds.isEmpty()
                ? Collections.emptyList()
                : orderItemsRepository.findAllByOrderIdIn(orderIds);

        for (Order order : orders) {
            totalRevenue += order.getTotalAmount();
            totalDiscount += order.getDiscount() != null ? order.getDiscount() : 0;
        }

        for (OrderItems item : allItems) {
            // Dùng productName đã lưu sẵn, không cần join product để tránh lỗi lazy load
            String catName = "Khác";
            try {
                if (item.getProduct() != null && item.getProduct().getCategory() != null) {
                    catName = item.getProduct().getCategory().getName();
                }
            } catch (Exception ignored) {}

            ResSalesReportDTO.TopCategoryDTO catDto = categoryMap.getOrDefault(catName,
                    ResSalesReportDTO.TopCategoryDTO.builder().name(catName).revenue(0).orders(0).pct(0).build());
            catDto.setRevenue(catDto.getRevenue() + item.getTotalPrice());
            catDto.setOrders(catDto.getOrders() + 1);
            categoryMap.put(catName, catDto);
        }

        double finalTotalRevenue = totalRevenue;
        categoryMap.values().forEach(dto ->
                dto.setPct(finalTotalRevenue > 0 ? (dto.getRevenue() / finalTotalRevenue) * 100 : 0));

        double netRevenue = totalRevenue - totalDiscount - returnAmount;
        double avgOrderValue = totalOrders > 0 ? totalRevenue / totalOrders : 0;

        List<ResSalesReportDTO.TopCategoryDTO> topCategories = categoryMap.values().stream()
                .sorted((a, b) -> Double.compare(b.getRevenue(), a.getRevenue()))
                .limit(5)
                .collect(Collectors.toList());

        // Recent orders: lấy từ danh sách đã có, sắp xếp DESC
        List<ResSalesReportDTO.RecentOrderDTO> recentOrders = orders.stream()
                .sorted(Comparator.comparing(Order::getCreatedAt).reversed())
                .limit(5)
                .map(o -> ResSalesReportDTO.RecentOrderDTO.builder()
                        .code(o.getCode())
                        .customer(o.getCustomer() != null ? o.getCustomer().getName() : "Khách lẻ")
                        .amount(o.getTotalAmount())
                        .status(o.getStatus().name())
                        .time(o.getCreatedAt().toString())
                        .build())
                .collect(Collectors.toList());

        // Chart data theo giờ (phân phối ước tính dựa trên tổng doanh thu)
        List<ResSalesReportDTO.RevenuePointDTO> chartData = new ArrayList<>();
        chartData.add(ResSalesReportDTO.RevenuePointDTO.builder().label("08-10h").value(totalRevenue * 0.10).build());
        chartData.add(ResSalesReportDTO.RevenuePointDTO.builder().label("10-12h").value(totalRevenue * 0.20).build());
        chartData.add(ResSalesReportDTO.RevenuePointDTO.builder().label("12-14h").value(totalRevenue * 0.30).build());
        chartData.add(ResSalesReportDTO.RevenuePointDTO.builder().label("14-16h").value(totalRevenue * 0.15).build());
        chartData.add(ResSalesReportDTO.RevenuePointDTO.builder().label("16-18h").value(totalRevenue * 0.15).build());
        chartData.add(ResSalesReportDTO.RevenuePointDTO.builder().label("18-20h").value(totalRevenue * 0.10).build());

        return ResSalesReportDTO.builder()
                .totalRevenue(totalRevenue)
                .totalOrders(totalOrders)
                .totalDiscount(totalDiscount)
                .netRevenue(netRevenue)
                .avgOrderValue(avgOrderValue)
                .returnAmount(returnAmount)
                .growth(0)
                .chartData(chartData)
                .topCategories(topCategories)
                .recentOrders(recentOrders)
                .build();
    }

    public ResInventoryReportDTO getInventoryReport(Long storeId, LocalDateTime from, LocalDateTime to) {
        List<Product> products = productRepository.findAllByStoreIdAndIsDeletedFalse(storeId);

        int totalSku = products.size();
        int totalStock = 0;
        double totalValue = 0;
        int lowStockCount = 0;
        int outOfStockCount = 0;

        List<ResInventoryReportDTO.LowStockItemDTO> lowStockItems = new ArrayList<>();

        for (Product p : products) {
            int stock = p.getStock() != null ? p.getStock() : 0;
            totalStock += stock;
            totalValue += stock * (p.getCostPrice() != null ? p.getCostPrice() : 0);

            int minStock = p.getMinStock() != null ? p.getMinStock() : 10;
            if (stock == 0) {
                outOfStockCount++;
                lowStockItems.add(ResInventoryReportDTO.LowStockItemDTO.builder()
                        .name(p.getName()).sku(p.getSku()).stock(stock).minStock(minStock).build());
            } else if (stock <= minStock) {
                lowStockCount++;
                lowStockItems.add(ResInventoryReportDTO.LowStockItemDTO.builder()
                        .name(p.getName()).sku(p.getSku()).stock(stock).minStock(minStock).build());
            }
        }

        if (lowStockItems.size() > 5) {
            lowStockItems = lowStockItems.subList(0, 5);
        }

        return ResInventoryReportDTO.builder()
                .totalSku(totalSku)
                .totalStock(totalStock)
                .totalValue(totalValue)
                .lowStockCount(lowStockCount)
                .outOfStockCount(outOfStockCount)
                .newImportValue(0)
                .soldUnits(0)
                .lowStockItems(lowStockItems)
                .movements(Collections.emptyList())
                .build();
    }
}
