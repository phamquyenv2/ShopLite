package com.quyen.shoplite.service;

import com.quyen.shoplite.domain.InventoryLogs;
import com.quyen.shoplite.domain.Order;
import com.quyen.shoplite.domain.OrderItems;
import com.quyen.shoplite.domain.Product;
import com.quyen.shoplite.event.OrderCancelledEvent;
import com.quyen.shoplite.event.OrderConfirmedEvent;
import com.quyen.shoplite.repository.InventoryLogsRepository;
import com.quyen.shoplite.repository.OrderItemsRepository;
import com.quyen.shoplite.repository.ProductRepository;
import com.quyen.shoplite.util.constant.TypeInventoryEnum;
import com.quyen.shoplite.util.error.IdInvalidException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderInventoryListener {

    private final ProductRepository productRepository;
    private final InventoryLogsRepository inventoryLogsRepository;
    private final OrderItemsRepository orderItemsRepository;

    @EventListener
    public void handleOrderConfirmed(OrderConfirmedEvent event) {
        Order order = event.getOrder();
        Long storeId = order.getStore().getId();
        List<OrderItems> items = orderItemsRepository.findAllByOrderId(order.getId());
        
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
        log.info("[OrderInventoryListener] Deducted stock for order id={}", order.getId());
    }

    @EventListener
    public void handleOrderCancelled(OrderCancelledEvent event) {
        Order order = event.getOrder();
        Long storeId = order.getStore().getId();
        boolean wasConfirmed = event.isWasConfirmed();

        if (wasConfirmed) {
            List<OrderItems> items = orderItemsRepository.findAllByOrderId(order.getId());
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
            log.info("[OrderInventoryListener] Restored stock for cancelled order id={}", order.getId());
        }
    }
}
