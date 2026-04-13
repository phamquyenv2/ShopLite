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
import com.quyen.shoplite.util.error.IdInvalidException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
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
    private final FcmService fcmService;

    @Transactional
    public ResOrderDTO create(ReqOrderDTO req) {
        // 1. Tìm user
        User user = userRepository.findById(req.getUserId())
                .orElseThrow(() -> new IdInvalidException("Không tìm thấy User id=" + req.getUserId()));

        Customer customer = customerRepository.findById(req.getCustomerId())
                .orElseThrow(() -> new IdInvalidException("Không tìm thấy Customer id=" + req.getCustomerId()));

        // 2. Tính tổng tiền & tạo order items
        double totalAmount = 0;
        List<OrderItems> itemsToSave = new ArrayList<>();

        for (ReqOrderItemDTO itemReq : req.getItems()) {
            Product product = productRepository.findById(itemReq.getProductId())
                    .orElseThrow(() -> new IdInvalidException("Không tìm thấy Product id=" + itemReq.getProductId()));

            if (product.getStock() < itemReq.getQuantity()) {
                throw new IdInvalidException("Sản phẩm '" + product.getName() + "' không đủ tồn kho");
            }

            double itemTotal = itemReq.getPrice() * itemReq.getQuantity();
            totalAmount += itemTotal;

            itemsToSave.add(OrderItems.builder()
                    .product(product)
                    .productName(product.getName())   // snapshot
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

        // 3. Lưu Order
        Order order = Order.builder()
                .user(user)
                .customer(customer)
                .code("ORD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .totalAmount(finalAmount)
                .discount(discount)
                .status(StatusEnum.PENDING)
                .createdAt(LocalDateTime.now())
                .build();
        Order savedOrder = orderRepository.save(order);

        // 4. Lưu OrderItems và trừ stock + ghi InventoryLog
        for (int i = 0; i < itemsToSave.size(); i++) {
            OrderItems item = itemsToSave.get(i);
            item.setOrder(savedOrder);
            orderItemsRepository.save(item);

            // Trừ stock
            Product product = item.getProduct();
            int newStock = product.getStock() - item.getQuantity().intValue();
            product.setStock(newStock);
            productRepository.save(product);

            // Ghi inventory log
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

        // Gửi push notification sau khi đặt hàng thành công
        try {
            fcmService.sendPaymentSuccessNotification(savedOrder);
        } catch (Exception e) {
            log.error("[FCM] Lỗi khi gửi push notification cho đơn hàng {}: {}", savedOrder.getCode(), e.getMessage());
        }

        // 5. Build response
        ResOrderDTO dto = DTOMapper.toResOrderDTO(savedOrder);
        dto.setItems(itemsToSave.stream().map(DTOMapper::toResOrderItemDTO).toList());
        return dto;
    }

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

    public List<ResOrderDTO> findAll() {
        return orderRepository.findAll().stream()
                .map(order -> {
                    ResOrderDTO dto = DTOMapper.toResOrderDTO(order);
                    dto.setItems(orderItemsRepository.findAllByOrderId(order.getId()).stream()
                            .map(DTOMapper::toResOrderItemDTO).toList());
                    return dto;
                }).toList();
    }

    @Transactional
    public ResOrderDTO updateStatus(Integer id, StatusEnum status) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new IdInvalidException("Không tìm thấy Order id=" + id));
        order.setStatus(status);
        if (status == StatusEnum.COMPLETED) {
            order.setPaidAt(LocalDateTime.now());
        }
        return DTOMapper.toResOrderDTO(orderRepository.save(order));
    }

    @Transactional
    public void cancel(Integer id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new IdInvalidException("Không tìm thấy Order id=" + id));

        if (order.getStatus() == StatusEnum.CANCELLED) {
            throw new IdInvalidException("Đơn hàng này đã được huỷ, không thể huỷ lại");
        }

        order.setStatus(StatusEnum.CANCELLED);
        orderRepository.save(order);

        List<OrderItems> items = orderItemsRepository.findAllByOrderId(id);
        for (OrderItems item : items) {
            Product product = item.getProduct();
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
}
