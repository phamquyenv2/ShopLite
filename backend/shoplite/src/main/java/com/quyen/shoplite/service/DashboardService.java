package com.quyen.shoplite.service;

import com.quyen.shoplite.domain.Order;
import com.quyen.shoplite.domain.Payment;
import com.quyen.shoplite.domain.response.ResDashboardDTO;
import com.quyen.shoplite.domain.response.ResMeDTO;
import com.quyen.shoplite.domain.response.ResNotificationDTO;
import com.quyen.shoplite.domain.response.ResOrderDTO;
import com.quyen.shoplite.domain.response.ResOrderItemDTO;
import com.quyen.shoplite.repository.NotificationRepository;
import com.quyen.shoplite.repository.OrderItemsRepository;
import com.quyen.shoplite.repository.OrderRepository;
import com.quyen.shoplite.repository.PaymentRepository;
import com.quyen.shoplite.util.DTOMapper;
import com.quyen.shoplite.util.constant.RefTypeEnum;
import com.quyen.shoplite.util.constant.StatusEnum;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final AuthService authService;
    private final NotificationService notificationService;
    private final OrderRepository orderRepository;
    private final OrderItemsRepository orderItemsRepository;
    private final PaymentRepository paymentRepository;

    @Transactional(readOnly = true)
    public ResDashboardDTO getDashboard(String jwtSubject) {
        // 1. User info + permissions + menus
        ResMeDTO me = authService.getCurrentUserProfile(jwtSubject);

        // 2. Today's orders
        Long storeId = me.getCurrentStore() != null ? me.getCurrentStore().getId() : null;
        ResDashboardDTO.TodayStats todayStats = buildTodayStats(storeId);

        // 3. Notifications
        List<ResNotificationDTO> notifications = notificationService.findMine();

        return ResDashboardDTO.builder()
                .user(me.getUser())
                .currentStore(me.getCurrentStore())
                .todayStats(todayStats)
                .notifications(notifications)
                .build();
    }

    private ResDashboardDTO.TodayStats buildTodayStats(Long storeId) {
        if (storeId == null) {
            return ResDashboardDTO.TodayStats.builder()
                    .orderCount(0).revenue(0).profit(0).recentOrders(List.of())
                    .build();
        }

        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime now = LocalDateTime.now();
        Sort sort = Sort.by(Sort.Direction.DESC, "createdAt");

        List<Order> orders = orderRepository.findAllByStoreIdAndCreatedAtBetween(storeId, startOfDay, now, sort);

        // Filter valid orders (COMPLETED or PENDING_PAYMENT)
        List<Order> validOrders = orders.stream()
                .filter(o -> o.getStatus() == StatusEnum.COMPLETED || o.getStatus() == StatusEnum.PENDING_PAYMENT)
                .toList();

        // Batch fetch payments and items
        List<Integer> orderIds = orders.stream().map(Order::getId).toList();
        Map<Integer, Payment> paymentMap = paymentRepository
                .findByStoreIdAndReferenceTypeAndReferenceIdIn(storeId, RefTypeEnum.ORDER, orderIds)
                .stream()
                .collect(Collectors.toMap(Payment::getReferenceId, p -> p, (a, b) -> a));
        Map<Integer, List<com.quyen.shoplite.domain.OrderItems>> itemsMap = orderItemsRepository
                .findAllByOrderIdIn(orderIds)
                .stream()
                .collect(Collectors.groupingBy(item -> item.getOrder().getId()));

        // Compute stats
        double revenue = validOrders.stream().mapToDouble(o -> o.getTotalAmount() != null ? o.getTotalAmount() : 0).sum();
        double profit = revenue * 0.3; // Estimated 30% margin

        // Recent 3 orders (all today orders, not just valid)
        List<ResOrderDTO> recentOrders = orders.stream()
                .sorted(Comparator.comparing(Order::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(3)
                .map(order -> {
                    ResOrderDTO dto = DTOMapper.toResOrderDTO(order);
                    Payment payment = paymentMap.get(order.getId());
                    if (payment != null) dto.setPaymentMethod(payment.getPaymentMethod());
                    dto.setItems(itemsMap.getOrDefault(order.getId(), List.of()).stream()
                            .map(DTOMapper::toResOrderItemDTO).toList());
                    return dto;
                })
                .toList();

        return ResDashboardDTO.TodayStats.builder()
                .orderCount(validOrders.size())
                .revenue(revenue)
                .profit(profit)
                .recentOrders(recentOrders)
                .build();
    }
}
