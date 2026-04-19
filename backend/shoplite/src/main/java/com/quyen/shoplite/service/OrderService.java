package com.quyen.shoplite.service;

import com.quyen.shoplite.domain.*;
import com.quyen.shoplite.domain.request.ReqOrderDTO;
import com.quyen.shoplite.domain.request.ReqOrderItemDTO;
import com.quyen.shoplite.domain.response.ResOrderDTO;
import com.quyen.shoplite.domain.response.ResOrderItemDTO;
import com.quyen.shoplite.repository.*;
import com.quyen.shoplite.util.DTOMapper;
import com.quyen.shoplite.util.constant.StatusEnum;
import com.quyen.shoplite.util.constant.TypeInventoryEnum;
import com.quyen.shoplite.util.constant.TypeTransactionEnum;
import com.quyen.shoplite.util.error.IdInvalidException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

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
    private final TransactionRepository transactionRepository;
    private final ApplicationEventPublisher eventPublisher;

    // ==================== CREATE DRAFT ====================

    /**
     * Tạo đơn hàng DRAFT — chỉ lưu thông tin, KHÔNG trừ kho.
     * Stock chỉ bị trừ khi gọi confirm().
     */
    @Transactional
    public ResOrderDTO create(ReqOrderDTO req) {
        // 1. Validate user
        User user = userRepository.findById(req.getUserId())
                .orElseThrow(() -> new IdInvalidException("Không tìm thấy User id=" + req.getUserId()));


        Customer customer = null;
        if (req.getCustomerId() != null) {
            customer = customerRepository.findById(req.getCustomerId())
                    .orElseThrow(() -> new IdInvalidException("KhÃ´ng tÃ¬m tháº¥y Customer id=" + req.getCustomerId()));
        }

        // 3. Validate items + compute totalAmount
        double totalAmount = 0;
        List<OrderItems> itemsToSave = new ArrayList<>();

        for (ReqOrderItemDTO itemReq : req.getItems()) {
            Product product = productRepository.findById(itemReq.getProductId())
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
                .user(user)
                .customer(customer)
                .requestId(req.getRequestId())
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
        ResOrderDTO dto = DTOMapper.toResOrderDTO(savedOrder);
        dto.setItems(itemsToSave.stream().map(DTOMapper::toResOrderItemDTO).toList());
        return dto;
    }

    // ==================== UPDATE DRAFT ====================

    /**
     * Cập nhật đơn DRAFT — thay đổi items, discount, customer.
     * Chỉ cho phép khi status = DRAFT.
     */
    @Transactional
    public ResOrderDTO update(Integer id, ReqOrderDTO req) {
        Order order = orderRepository.findByIdWithLock(id)
                .orElseThrow(() -> new IdInvalidException("Không tìm thấy Order id=" + id));

        if (order.getStatus() != StatusEnum.DRAFT) {
            throw new IdInvalidException("Chỉ có thể cập nhật đơn hàng ở trạng thái DRAFT");
        }

        // Update customer
        if (req.getCustomerId() != null) {
            Customer customer = customerRepository.findById(req.getCustomerId())
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
            Product product = productRepository.findById(itemReq.getProductId())
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

        ResOrderDTO dto = DTOMapper.toResOrderDTO(savedOrder);
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
        Order order = orderRepository.findByIdWithLock(id)
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
            Product product = productRepository.findByIdWithLock(item.getProduct().getId())
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

        ResOrderDTO dto = DTOMapper.toResOrderDTO(savedOrder);
        dto.setItems(items.stream().map(DTOMapper::toResOrderItemDTO).toList());
        return dto;
    }

    // ==================== CANCEL ====================

    /**
     * Huỷ đơn hàng:
     * - DRAFT: chỉ đổi status, không cần hoàn kho
     * - PENDING_PAYMENT / COMPLETED: hoàn kho + tạo REFUND transaction nếu đã paid
     */
    @Transactional
    public void cancel(Integer id) {
        Order order = orderRepository.findByIdWithLock(id)
                .orElseThrow(() -> new IdInvalidException("Không tìm thấy Order id=" + id));

        if (order.getStatus() == StatusEnum.CANCELLED) {
            throw new IdInvalidException("Đơn hàng này đã được huỷ, không thể huỷ lại");
        }

        boolean wasConfirmed = order.getConfirmedAt() != null;
        boolean wasPaid = order.getPaidAt() != null || order.getStatus() == StatusEnum.COMPLETED;

        order.setStatus(StatusEnum.CANCELLED);
        orderRepository.save(order);

        // Only restore stock if order was confirmed (stock was deducted)
        if (wasConfirmed) {
            List<OrderItems> items = orderItemsRepository.findAllByOrderId(id);
            for (OrderItems item : items) {
                Product product = productRepository.findByIdWithLock(item.getProduct().getId())
                        .orElseThrow(() -> new IdInvalidException(
                                "Không tìm thấy Product id=" + item.getProduct().getId()));
                int restoreQuantity = item.getQuantity().intValue();
                int newStock = product.getStock() + restoreQuantity;
                product.setStock(newStock);
                productRepository.save(product);

                inventoryLogsRepository.save(InventoryLogs.builder()
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

        // If order was paid, create REFUND transaction in cashbook
        if (wasPaid) {
            Optional<Payment> payment = paymentRepository.findByOrder_Id(id);
            double refundAmount = payment.map(Payment::getAmount).orElse(order.getTotalAmount());

            transactionRepository.save(Transaction.builder()
                    .order(order)
                    .payment(payment.orElse(null))
                    .amount(refundAmount)
                    .type(TypeTransactionEnum.REFUND)
                    .content("Hoàn tiền huỷ đơn hàng " + order.getCode())
                    .transactionTime(LocalDateTime.now())
                    .createdAt(LocalDateTime.now())
                    .build());

            log.info("[Order] Cancelled paid order id={}, refund amount={}", id, refundAmount);
        }
    }

    // ==================== ADMIN STATUS UPDATE ====================

    @Transactional
    public ResOrderDTO updateStatus(Integer id, StatusEnum status) {
        Order order = orderRepository.findByIdWithLock(id)
                .orElseThrow(() -> new IdInvalidException("Không tìm thấy Order id=" + id));
        order.setStatus(status);
        if (status == StatusEnum.COMPLETED) {
            order.setPaidAt(LocalDateTime.now());
        }
        return DTOMapper.toResOrderDTO(orderRepository.save(order));
    }

    // ==================== READ ====================

    public ResOrderDTO findById(Integer id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new IdInvalidException("Không tìm thấy Order id=" + id));
        ResOrderDTO dto = DTOMapper.toResOrderDTO(order);
        List<ResOrderItemDTO> items = orderItemsRepository.findAllByOrderId(id).stream()
                .map(DTOMapper::toResOrderItemDTO)
                .toList();
        dto.setItems(items);
        return dto;
    }

    public List<ResOrderDTO> findAll(List<StatusEnum> statuses) {
        org.springframework.data.domain.Sort sort = org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "createdAt");
        List<Order> orders;
        if (statuses == null || statuses.isEmpty()) {
            orders = orderRepository.findAll(sort);
        } else {
            orders = orderRepository.findByStatusIn(statuses, sort);
        }
        return orders.stream()
                .map(order -> {
                    ResOrderDTO dto = DTOMapper.toResOrderDTO(order);
                    dto.setItems(orderItemsRepository.findAllByOrderId(order.getId()).stream()
                            .map(DTOMapper::toResOrderItemDTO).toList());
                    return dto;
                }).toList();
    }
}
