package com.quyen.shoplite.service;

import com.quyen.shoplite.repository.*;
import com.quyen.shoplite.util.constant.PaymentMethodEnum;
import com.quyen.shoplite.util.constant.RefTypeEnum;
import com.quyen.shoplite.util.constant.TypeInventoryEnum;
import com.quyen.shoplite.util.error.IdInvalidException;

import com.quyen.shoplite.domain.*;
import com.quyen.shoplite.domain.request.ReqImportReturnItemDTO;
import com.quyen.shoplite.domain.request.ReqImportReturnOrderDTO;
import com.quyen.shoplite.domain.request.ReqPaymentDTO;
import com.quyen.shoplite.domain.response.ResImportReturnItemDTO;
import com.quyen.shoplite.domain.response.ResImportReturnOrderDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ImportReturnOrderService {

    private final ImportReturnOrderRepository importReturnOrderRepository;
    private final ImportReturnItemRepository importReturnItemRepository;
    private final SupplierRepository supplierRepository;
    private final ProductRepository productRepository;
    private final ImportOrderRepository importOrderRepository;
    private final InventoryLogsRepository inventoryLogsRepository;
    private final PaymentService paymentService;
    private final ImportItemRepository importItemRepository;
    private final CurrentStoreService currentStoreService;

    // ==================== CREATE ====================

    @Transactional
    public ResImportReturnOrderDTO create(ReqImportReturnOrderDTO req) {
        Store store = currentStoreService.getCurrentStore();
        Long storeId = store.getId();
        // 1. Verify supplier
        Supplier supplier = supplierRepository.findByIdAndStoreId(req.getSupplierId(), storeId)
                .orElseThrow(() -> new IdInvalidException("Không tìm thấy Supplier id=" + req.getSupplierId()));

        // 2. Resolve optional import order
        ImportOrder importOrder = null;
        if (req.getImportOrderId() != null) {
            importOrder = importOrderRepository.findByIdAndStoreIdWithLock(req.getImportOrderId(), storeId)
                    .orElseThrow(() -> new IdInvalidException("Không tìm thấy ImportOrder id=" + req.getImportOrderId()));
            
            if (com.quyen.shoplite.util.constant.ImportReturnStatusEnum.FULL_RETURNED.equals(importOrder.getReturnStatus())) {
                throw new IdInvalidException("Phiếu nhập này đã được trả hàng toàn bộ!");
            }
        }

        // 3. Validate items & compute subtotals
        List<ImportReturnItem> itemsToSave = new ArrayList<>();
        double subtotalSum = 0.0;

        List<ImportItem> originalItems = null;
        if (importOrder != null) {
            originalItems = importItemRepository.findByImportOrder_Id(importOrder.getId());
        }

        for (ReqImportReturnItemDTO itemReq : req.getItems()) {
            Product product = productRepository.findByIdAndStoreIdWithLock(itemReq.getProductId(), storeId)
                    .orElseThrow(() -> new IdInvalidException("Không tìm thấy Product id=" + itemReq.getProductId()));

            if (importOrder != null && originalItems != null) {
                ImportItem originalItem = originalItems.stream()
                        .filter(i -> i.getProduct().getId().equals(itemReq.getProductId()))
                        .findFirst()
                        .orElseThrow(() -> new IdInvalidException("Sản phẩm " + product.getName() + " không có trong phiếu nhập này"));
                
                int currentReturned = originalItem.getReturnedQuantity() != null ? originalItem.getReturnedQuantity() : 0;
                if (currentReturned + itemReq.getQuantity() > originalItem.getQuantity()) {
                    throw new IdInvalidException("Số lượng trả vượt quá số lượng đã nhập cho sản phẩm: " + product.getName());
                }
                originalItem.setReturnedQuantity(currentReturned + itemReq.getQuantity());
            }

            double subTotal = itemReq.getReturnPrice() * itemReq.getQuantity();
            subtotalSum += subTotal;

            itemsToSave.add(ImportReturnItem.builder()
                    .product(product)
                    .quantity(itemReq.getQuantity())
                    .returnPrice(itemReq.getReturnPrice())
                    .subTotal(subTotal)
                    .build());
        }

        // 4. Compute total
        double discount = req.getDiscount() != null ? req.getDiscount() : 0.0;
        double totalAmount = subtotalSum - discount;
        if (totalAmount < 0) {
            throw new IdInvalidException("Tổng tiền trả hàng không được âm");
        }

        double amountPaid = req.getAmountPaid() != null ? req.getAmountPaid() : 0.0;

        // 5. Save ImportReturnOrder
        ImportReturnOrder returnOrder = ImportReturnOrder.builder()
                .store(store)
                .importOrder(importOrder)
                .supplier(supplier)
                .totalAmount(totalAmount)
                .discount(discount)
                .amountPaid(amountPaid)
                .note(req.getNote())
                .createdByUsername(req.getCreatedByUsername())
                .receivedByUsername(req.getReceivedByUsername())
                .createdAt(LocalDateTime.now())
                .build();
        ImportReturnOrder savedOrder = importReturnOrderRepository.save(returnOrder);

        if (importOrder != null && originalItems != null) {
            importItemRepository.saveAll(originalItems);

            boolean allFull = true;
            boolean anyPartial = false;
            for (ImportItem i : originalItems) {
                int returnedQty = i.getReturnedQuantity() != null ? i.getReturnedQuantity() : 0;
                if (returnedQty > 0) {
                    anyPartial = true;
                }
                if (returnedQty < i.getQuantity()) {
                    allFull = false;
                }
            }
            if (allFull) {
                importOrder.setReturnStatus(com.quyen.shoplite.util.constant.ImportReturnStatusEnum.FULL_RETURNED);
            } else if (anyPartial) {
                importOrder.setReturnStatus(com.quyen.shoplite.util.constant.ImportReturnStatusEnum.PARTIAL_RETURNED);
            } else {
                importOrder.setReturnStatus(com.quyen.shoplite.util.constant.ImportReturnStatusEnum.UNRETURNED);
            }
            importOrderRepository.save(importOrder);
        }

        // 6. Save items + reduce stock + log
        for (ImportReturnItem item : itemsToSave) {
            item.setImportReturnOrder(savedOrder);
        }
        List<ImportReturnItem> savedItems = importReturnItemRepository.saveAll(itemsToSave);

        for (ImportReturnItem item : savedItems) {
            Product product = productRepository.findByIdAndStoreIdWithLock(item.getProduct().getId(), storeId)
                    .orElseThrow(() -> new IdInvalidException("Không tìm thấy Product id=" + item.getProduct().getId()));

            int removedQty = item.getQuantity();
            int newStock = product.getStock() - removedQty;
            if (newStock < 0) {
                throw new IdInvalidException("Không đủ tồn kho để trả nhà cung cấp cho sản phẩm: " + product.getName() + ". Tồn hiện tại: " + product.getStock());
            }
            product.setStock(newStock);
            productRepository.save(product);

            inventoryLogsRepository.save(InventoryLogs.builder()
                    .store(store)
                    .product(product)
                    .quantityOut(removedQty)
                    .balanceAfter(newStock)
                    .currentStock(newStock)
                    .type(TypeInventoryEnum.RETURN)
                    .createdAt(LocalDateTime.now())
                    .build());
        }

        // 7. Tạo Payment SUPPLIER_RETURN thông qua PaymentService (nếu có fundAccountId)
        if (totalAmount > 0 && req.getFundAccountId() != null) {
            PaymentMethodEnum method = PaymentMethodEnum.CASH;
            if (req.getPaymentMethod() != null) {
                try {
                    method = PaymentMethodEnum.valueOf(req.getPaymentMethod());
                } catch (IllegalArgumentException ignored) {}
            }

            ReqPaymentDTO paymentReq = new ReqPaymentDTO();
            paymentReq.setReferenceType(RefTypeEnum.SUPPLIER_RETURN);
            paymentReq.setReferenceId(savedOrder.getId());
            paymentReq.setPaymentMethod(method);
            paymentReq.setAmount(BigDecimal.valueOf(totalAmount));
            paymentReq.setFundAccountId(req.getFundAccountId());
            paymentService.createPaymentSession(paymentReq);
        }

        log.info("[ImportReturnOrder] Created return order id={}, total={}, items={}",
                savedOrder.getId(), totalAmount, savedItems.size());

        return toDTO(savedOrder, savedItems);
    }

    // ==================== FIND ALL ====================

    public List<ResImportReturnOrderDTO> findAll() {
        Long storeId = currentStoreService.getCurrentStoreId();
        List<ImportReturnOrder> orders = importReturnOrderRepository.findAllByStoreIdOrderByCreatedAtDesc(storeId);
        List<Integer> orderIds = orders.stream().map(ImportReturnOrder::getId).toList();
        Map<Integer, List<ImportReturnItem>> itemsMap = importReturnItemRepository.findByImportReturnOrder_IdIn(orderIds)
                .stream()
                .collect(Collectors.groupingBy(item -> item.getImportReturnOrder().getId()));
        return orders.stream()
                .map(order -> toDTO(order, itemsMap.getOrDefault(order.getId(), List.of())))
                .collect(Collectors.toList());
    }

    // ==================== FIND BY ID ====================

    public ResImportReturnOrderDTO findById(Integer id) {
        Long storeId = currentStoreService.getCurrentStoreId();
        ImportReturnOrder order = importReturnOrderRepository.findByIdAndStoreId(id, storeId)
                .orElseThrow(() -> new IdInvalidException("Không tìm thấy ImportReturnOrder id=" + id));
        List<ImportReturnItem> items = importReturnItemRepository.findByImportReturnOrder_Id(id);
        return toDTO(order, items);
    }

    // ==================== DTO Mapping ====================

    private ResImportReturnOrderDTO toDTO(ImportReturnOrder order, List<ImportReturnItem> items) {
        ResImportReturnOrderDTO dto = new ResImportReturnOrderDTO();
        dto.setId(order.getId());
        dto.setTotalAmount(order.getTotalAmount());
        dto.setDiscount(order.getDiscount());
        dto.setAmountPaid(order.getAmountPaid());
        dto.setNote(order.getNote());
        dto.setCreatedByUsername(order.getCreatedByUsername());
        dto.setReceivedByUsername(order.getReceivedByUsername());
        dto.setCreatedAt(order.getCreatedAt());

        if (order.getSupplier() != null) {
            dto.setSupplierId(order.getSupplier().getId());
            dto.setSupplierName(order.getSupplier().getName());
            dto.setSupplierPhone(order.getSupplier().getPhone());
        }

        if (order.getImportOrder() != null) {
            dto.setImportOrderId(order.getImportOrder().getId());
        }

        if (items != null) {
            dto.setItems(items.stream().map(this::toItemDTO).collect(Collectors.toList()));
        } else {
            dto.setItems(Collections.emptyList());
        }

        return dto;
    }

    private ResImportReturnItemDTO toItemDTO(ImportReturnItem item) {
        ResImportReturnItemDTO dto = new ResImportReturnItemDTO();
        dto.setId(item.getId());
        dto.setQuantity(item.getQuantity());
        dto.setReturnPrice(item.getReturnPrice());
        dto.setSubTotal(item.getSubTotal());
        if (item.getProduct() != null) {
            dto.setProductId(item.getProduct().getId());
            dto.setProductName(item.getProduct().getName());
            dto.setProductSku(item.getProduct().getSku());
            dto.setProductImage(item.getProduct().getImage());
        }
        return dto;
    }
}
