package com.quyen.shoplite.service;

import com.quyen.shoplite.repository.*;
import com.quyen.shoplite.util.DTOMapper;
import com.quyen.shoplite.util.constant.RefTypeEnum;
import com.quyen.shoplite.util.constant.StatusEnum;
import com.quyen.shoplite.util.constant.TypeInventoryEnum;
import com.quyen.shoplite.util.error.IdInvalidException;

import com.quyen.shoplite.domain.*;
import com.quyen.shoplite.domain.request.ReqOrderDTO;
import com.quyen.shoplite.domain.request.ReqOrderItemDTO;
import com.quyen.shoplite.domain.response.ResOrderDTO;
import com.quyen.shoplite.domain.response.ResOrderItemDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemsRepository orderItemsRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final InventoryLogsRepository inventoryLogsRepository;
    private final CustomerRepository customerRepository;
    private final PaymentRepository paymentRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final CurrentStoreService currentStoreService;

    public record CreateOrderResult(ResOrderDTO order, boolean created) {
    }

    private ResOrderDTO toOrderDTOWithPayment(Order order) {
        ResOrderDTO dto = DTOMapper.toResOrderDTO(order);
        paymentRepository.findFirstByStoreIdAndReferenceTypeAndReferenceIdOrderByIdDesc(order.getStore().getId(), RefTypeEnum.ORDER, order.getId())
                .map(Payment::getPaymentMethod)
                .ifPresent(dto::setPaymentMethod);
        return dto;
    }

    @Transactional
    public CreateOrderResult create(ReqOrderDTO req) {
        Store store = currentStoreService.getCurrentStore();
        Long storeId = store.getId();
        String requestId = req.getRequestId();
        if (requestId == null || requestId.isBlank()) {
            requestId = UUID.randomUUID().toString();
        } else {
            requestId = requestId.trim();
            Optional<Order> existingOrder = orderRepository.findByStoreIdAndRequestId(storeId, requestId);
            if (existingOrder.isPresent()) {
                Order order = existingOrder.get();
                ResOrderDTO dto = toOrderDTOWithPayment(order);
                dto.setItems(orderItemsRepository.findAllByOrderId(order.getId()).stream()
                        .map(DTOMapper::toResOrderItemDTO)
                        .toList());
                return new CreateOrderResult(dto, false);
            }
        }

        // 1. Validate user
        User user = userRepository.findById(req.getUserId())
                .orElseThrow(() -> new IdInvalidException("Không tìm thấy User id=" + req.getUserId()));


        Customer customer = null;
        if (req.getCustomerId() != null) {
            customer = customerRepository.findByIdAndStoreId(req.getCustomerId(), storeId)
                    .orElseThrow(() -> new IdInvalidException("Không tìm thấy Customer id=" + req.getCustomerId()));
        }

        // 3. Validate items + compute totalAmount
        double totalAmount = 0;
        List<OrderItems> itemsToSave = new ArrayList<>();

        for (ReqOrderItemDTO itemReq : req.getItems()) {
            Product product = productRepository.findByIdAndStoreIdAndIsDeletedFalse(itemReq.getProductId(), storeId)
                    .orElseThrow(() -> new IdInvalidException("Không tìm thấy Product id=" + itemReq.getProductId()));

            double itemTotal = itemReq.getPrice() * itemReq.getQuantity();
            totalAmount += itemTotal;

            itemsToSave.add(OrderItems.builder()
                    .product(product)
                    .productName(product.getName())
                    .quantity(itemReq.getQuantity())
                    .price(itemReq.getPrice())
                    .totalPrice(itemTotal)
                    .build());
        }

        double discount = req.getDiscount() != null ? req.getDiscount() : 0;
        double finalAmount = totalAmount - discount;
        if (finalAmount < 0) {
            throw new IdInvalidException("Tổng tiền đơn hàng sau khi giảm giá không được âm");
        }

        // 4. Save Order as DRAFT
        Order order = Order.builder()
                .store(store)
                .user(user)
                .customer(customer)
                .requestId(requestId)
                .code("ORD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .totalAmount(finalAmount)
                .discount(discount)
                .status(StatusEnum.DRAFT)
                .createdAt(LocalDateTime.now())
                .build();
        Order savedOrder = orderRepository.save(order);

        // 5. Save OrderItems (NO stock deduction)
        for (OrderItems item : itemsToSave) {
            item.setOrder(savedOrder);
        }
        orderItemsRepository.saveAll(itemsToSave);

        // 6. Build response
        ResOrderDTO dto = toOrderDTOWithPayment(savedOrder);
        dto.setItems(itemsToSave.stream().map(DTOMapper::toResOrderItemDTO).toList());
        return new CreateOrderResult(dto, true);
    }

    // ==================== UPDATE DRAFT ====================

    /**
     * Cập nhật đơn DRAFT — thay đổi items, discount, customer.
     * Chỉ cho phép khi status = DRAFT.
     */
    @Transactional
    public ResOrderDTO update(Integer id, ReqOrderDTO req) {
        Long storeId = currentStoreService.getCurrentStoreId();
        Order order = orderRepository.findByIdAndStoreIdWithLock(id, storeId)
                .orElseThrow(() -> new IdInvalidException("Không tìm thấy Order id=" + id));

        if (order.getStatus() != StatusEnum.DRAFT) {
            throw new IdInvalidException("Chỉ có thể cập nhật đơn hàng ở trạng thái DRAFT");
        }

        // Update customer
        if (req.getCustomerId() != null) {
            Customer customer = customerRepository.findByIdAndStoreId(req.getCustomerId(), storeId)
                    .orElseThrow(() -> new IdInvalidException("Không tìm thấy Customer id=" + req.getCustomerId()));
            order.setCustomer(customer);
        } else {
            order.setCustomer(null);
        }

        // Delete old items + recreate
        orderItemsRepository.deleteAllByOrder_Id(id);

        double totalAmount = 0;
        List<OrderItems> newItems = new ArrayList<>();

        for (ReqOrderItemDTO itemReq : req.getItems()) {
            Product product = productRepository.findByIdAndStoreIdAndIsDeletedFalse(itemReq.getProductId(), storeId)
                    .orElseThrow(() -> new IdInvalidException("Không tìm thấy Product id=" + itemReq.getProductId()));

            double itemTotal = itemReq.getPrice() * itemReq.getQuantity();
            totalAmount += itemTotal;

            newItems.add(OrderItems.builder()
                    .order(order)
                    .product(product)
                    .productName(product.getName())
                    .quantity(itemReq.getQuantity())
                    .price(itemReq.getPrice())
                    .totalPrice(itemTotal)
                    .build());
        }

        double discount = req.getDiscount() != null ? req.getDiscount() : 0;
        double finalAmount = totalAmount - discount;
        if (finalAmount < 0) {
            throw new IdInvalidException("Tổng tiền đơn hàng sau khi giảm giá không được âm");
        }

        order.setDiscount(discount);
        order.setTotalAmount(finalAmount);
        Order savedOrder = orderRepository.save(order);

        List<OrderItems> savedItems = orderItemsRepository.saveAll(newItems);

        ResOrderDTO dto = toOrderDTOWithPayment(savedOrder);
        dto.setItems(savedItems.stream().map(DTOMapper::toResOrderItemDTO).toList());
        return dto;
    }

    // ==================== CONFIRM (DRAFT → PENDING_PAYMENT) ====================

    /**
     * Chốt đơn — trừ kho + chuyển sang PENDING_PAYMENT.
     * Đây là thời điểm duy nhất stock bị trừ.
     */
    @Transactional
    public ResOrderDTO confirm(Integer id) {
        Long storeId = currentStoreService.getCurrentStoreId();
        Order order = orderRepository.findByIdAndStoreIdWithLock(id, storeId)
                .orElseThrow(() -> new IdInvalidException("Không tìm thấy Order id=" + id));

        if (order.getStatus() != StatusEnum.DRAFT) {
            throw new IdInvalidException("Chỉ có thể xác nhận đơn hàng ở trạng thái DRAFT");
        }

        List<OrderItems> items = orderItemsRepository.findAllByOrderId(id);
        if (items.isEmpty()) {
            throw new IdInvalidException("Đơn hàng không có sản phẩm");
        }

        // Deduct stock with pessimistic lock
        for (OrderItems item : items) {
            Product product = productRepository.findByIdAndStoreIdWithLock(item.getProduct().getId(), storeId)
                    .orElseThrow(() -> new IdInvalidException(
                            "Không tìm thấy Product id=" + item.getProduct().getId()));

            if (product.getStock() < item.getQuantity()) {
                throw new IdInvalidException(
                        "Sản phẩm '" + product.getName() + "' không đủ tồn kho (còn "
                        + product.getStock() + ", cần " + item.getQuantity() + ")");
            }

            int newStock = product.getStock() - item.getQuantity().intValue();
            product.setStock(newStock);
            productRepository.save(product);

            inventoryLogsRepository.save(InventoryLogs.builder()
                    .store(order.getStore())
                    .product(product)
                    .orderItem(item)
                    .quantityOut(item.getQuantity().intValue())
                    .balanceAfter(newStock)
                    .currentStock(newStock)
                    .type(TypeInventoryEnum.SALE)
                    .createdAt(LocalDateTime.now())
                    .build());
        }

        // Update status
        order.setStatus(StatusEnum.PENDING_PAYMENT);
        order.setConfirmedAt(LocalDateTime.now());
        Order savedOrder = orderRepository.save(order);

        log.info("[Order] Confirmed order id={}, code={}, items={}", id, order.getCode(), items.size());

        ResOrderDTO dto = toOrderDTOWithPayment(savedOrder);
        dto.setItems(items.stream().map(DTOMapper::toResOrderItemDTO).toList());
        return dto;
    }

    // ==================== CANCEL ====================

    /**
     * Huỷ đơn hàng:
     * - DRAFT: chỉ đổi status, không cần hoàn kho
     * - PENDING_PAYMENT / COMPLETED: hoàn kho
     * - Nếu đã thanh toán: việc hoàn tiền sẽ do frontend tạo Payment REFUND riêng
     */
    @Transactional
    public void cancel(Integer id) {
        Long storeId = currentStoreService.getCurrentStoreId();
        Order order = orderRepository.findByIdAndStoreIdWithLock(id, storeId)
                .orElseThrow(() -> new IdInvalidException("Không tìm thấy Order id=" + id));

        if (order.getStatus() == StatusEnum.CANCELLED) {
            throw new IdInvalidException("Đơn hàng này đã được huỷ, không thể huỷ lại");
        }

        boolean wasConfirmed = order.getConfirmedAt() != null;

        order.setStatus(StatusEnum.CANCELLED);
        orderRepository.save(order);

        // Only restore stock if order was confirmed (stock was deducted)
        if (wasConfirmed) {
            List<OrderItems> items = orderItemsRepository.findAllByOrderId(id);
            for (OrderItems item : items) {
                Product product = productRepository.findByIdAndStoreIdWithLock(item.getProduct().getId(), storeId)
                        .orElseThrow(() -> new IdInvalidException(
                                "Không tìm thấy Product id=" + item.getProduct().getId()));
                int restoreQuantity = item.getQuantity().intValue();
                int newStock = product.getStock() + restoreQuantity;
                product.setStock(newStock);
                productRepository.save(product);

                inventoryLogsRepository.save(InventoryLogs.builder()
                        .store(order.getStore())
                        .product(product)
                        .orderItem(item)
                        .quantityIn(restoreQuantity)
                        .balanceAfter(newStock)
                        .currentStock(newStock)
                        .type(TypeInventoryEnum.RETURN)
                        .createdAt(LocalDateTime.now())
                        .build());
            }
        }

        log.info("[Order] Cancelled order id={}, code={}", id, order.getCode());
    }

    // ==================== ADMIN STATUS UPDATE ====================

    @Transactional
    public ResOrderDTO updateStatus(Integer id, StatusEnum status) {
        Long storeId = currentStoreService.getCurrentStoreId();
        Order order = orderRepository.findByIdAndStoreIdWithLock(id, storeId)
                .orElseThrow(() -> new IdInvalidException("Không tìm thấy Order id=" + id));
        order.setStatus(status);
        if (status == StatusEnum.COMPLETED) {
            order.setPaidAt(LocalDateTime.now());
        }
        return toOrderDTOWithPayment(orderRepository.save(order));
    }

    // ==================== READ ====================

    public ResOrderDTO findById(Integer id) {
        Long storeId = currentStoreService.getCurrentStoreId();
        Order order = orderRepository.findByIdAndStoreId(id, storeId)
                .orElseThrow(() -> new IdInvalidException("Không tìm thấy Order id=" + id));
        ResOrderDTO dto = toOrderDTOWithPayment(order);
        List<ResOrderItemDTO> items = orderItemsRepository.findAllByOrderId(id).stream()
                .map(DTOMapper::toResOrderItemDTO)
                .toList();
        dto.setItems(items);
        return dto;
    }

    public List<ResOrderDTO> findAll(List<StatusEnum> statuses, String from, String to) {
        org.springframework.data.domain.Sort sort = org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "createdAt");
        List<Order> orders;
        Long storeId = currentStoreService.getCurrentStoreId();
        LocalDateTime fromDate = parseDateTime(from);
        LocalDateTime toDate = parseDateTime(to);
        boolean hasDateRange = fromDate != null && toDate != null;

        if (statuses == null || statuses.isEmpty()) {
            orders = hasDateRange
                    ? orderRepository.findAllByStoreIdAndCreatedAtBetween(storeId, fromDate, toDate, sort)
                    : orderRepository.findAllByStoreId(storeId, sort);
        } else {
            orders = hasDateRange
                    ? orderRepository.findByStoreIdAndStatusInAndCreatedAtBetween(storeId, statuses, fromDate, toDate, sort)
                    : orderRepository.findByStoreIdAndStatusIn(storeId, statuses, sort);
        }
        List<Integer> orderIds = orders.stream().map(Order::getId).toList();

        Map<Integer, Payment> paymentMap = paymentRepository
                .findByStoreIdAndReferenceTypeAndReferenceIdIn(storeId, RefTypeEnum.ORDER, orderIds)
                .stream()
                .collect(Collectors.toMap(Payment::getReferenceId, p -> p, (a, b) -> a));

        Map<Integer, List<OrderItems>> itemsMap = orderItemsRepository
                .findAllByOrderIdIn(orderIds)
                .stream()
                .collect(Collectors.groupingBy(item -> item.getOrder().getId()));

        return orders.stream().map(order -> {
            ResOrderDTO dto = DTOMapper.toResOrderDTO(order);
            Payment payment = paymentMap.get(order.getId());
            if (payment != null) dto.setPaymentMethod(payment.getPaymentMethod());
            dto.setItems(itemsMap.getOrDefault(order.getId(), List.of()).stream()
                    .map(DTOMapper::toResOrderItemDTO).toList());
            return dto;
        }).toList();
    }

    private LocalDateTime parseDateTime(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }

        String value = raw.trim();
        try {
            return OffsetDateTime.parse(value).toLocalDateTime();
        } catch (DateTimeParseException ignored) {
            try {
                return LocalDateTime.parse(value);
            } catch (DateTimeParseException ex) {
                throw new IdInvalidException("Ngay gio khong hop le: " + value);
            }
        }
    }
}
