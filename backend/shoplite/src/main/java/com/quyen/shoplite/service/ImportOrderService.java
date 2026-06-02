package com.quyen.shoplite.service;

import com.quyen.shoplite.domain.*;
import com.quyen.shoplite.domain.request.*;
import com.quyen.shoplite.domain.response.ResImportOrderDTO;
import com.quyen.shoplite.repository.*;
import com.quyen.shoplite.util.DTOMapper;
import com.quyen.shoplite.util.SecurityUtil;
import com.quyen.shoplite.util.constant.ImportOrderStatusEnum;
import com.quyen.shoplite.util.constant.PaymentMethodEnum;
import com.quyen.shoplite.util.constant.RefTypeEnum;
import com.quyen.shoplite.util.constant.TypeInventoryEnum;
import com.quyen.shoplite.util.error.IdInvalidException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ImportOrderService {

    private final ImportOrderRepository importOrderRepository;
    private final ImportItemRepository importItemRepository;
    private final SupplierRepository supplierRepository;
    private final ProductRepository productRepository;
    private final InventoryLogsRepository inventoryLogsRepository;
    private final PaymentService paymentService;
    private final CurrentStoreService currentStoreService;
    private final ImportOrderNotificationService importOrderNotificationService;

    @Transactional
    public ResImportOrderDTO create(ReqImportOrderDTO req) {
        Store store = currentStoreService.getCurrentStore();
        validateProductsNotInUnfinishedOrders(req.getItems(), store.getId(), -1);
        Supplier supplier = findSupplier(req.getSupplierId(), store.getId());
        List<ImportItem> items = buildItems(req.getItems(), store.getId(), null);
        double totalAmount = calculateTotal(items, req.getTax(), req.getDiscount());

        ImportOrder order = ImportOrder.builder()
                .store(store)
                .supplier(supplier)
                .tax(orZero(req.getTax()))
                .discount(orZero(req.getDiscount()))
                .totalAmount(totalAmount)
                .amountPaid(orZero(req.getPaidAmount()))
                .status(ImportOrderStatusEnum.PENDING)
                .note(req.getNote())
                .createdAt(LocalDateTime.now())
                .build();
        ImportOrder saved = importOrderRepository.save(order);
        items.forEach(item -> item.setImportOrder(saved));
        return DTOMapper.toResImportOrderDTO(saved, importItemRepository.saveAll(items));
    }

    @Transactional
    public ResImportOrderDTO update(Integer id, ReqImportOrderDTO req) {
        Long storeId = currentStoreService.getCurrentStoreId();
        validateProductsNotInUnfinishedOrders(req.getItems(), storeId, id);
        ImportOrder order = importOrderRepository.findByIdAndStoreId(id, storeId)
                .orElseThrow(() -> new IdInvalidException("Khong tim thay ImportOrder id=" + id));
        requireStatus(order, ImportOrderStatusEnum.PENDING);

        List<ImportItem> oldItems = importItemRepository.findByImportOrder_Id(id);
        importItemRepository.deleteAll(oldItems);
        List<ImportItem> items = buildItems(req.getItems(), storeId, order);

        order.setSupplier(findSupplier(req.getSupplierId(), storeId));
        order.setTax(orZero(req.getTax()));
        order.setDiscount(orZero(req.getDiscount()));
        order.setTotalAmount(calculateTotal(items, req.getTax(), req.getDiscount()));
        order.setAmountPaid(orZero(req.getPaidAmount()));
        order.setNote(req.getNote());
        return DTOMapper.toResImportOrderDTO(importOrderRepository.save(order), importItemRepository.saveAll(items));
    }

    public List<ResImportOrderDTO> findAll() {
        Long storeId = currentStoreService.getCurrentStoreId();
        List<ImportOrder> orders = importOrderRepository.findAllByStoreIdOrderByCreatedAtDesc(storeId);
        Map<Integer, List<ImportItem>> itemsByOrderId = importItemRepository
                .findByImportOrder_IdIn(orders.stream().map(ImportOrder::getId).toList())
                .stream()
                .collect(Collectors.groupingBy(item -> item.getImportOrder().getId()));
        return orders.stream()
                .map(order -> DTOMapper.toResImportOrderDTO(order, itemsByOrderId.getOrDefault(order.getId(), List.of())))
                .toList();
    }

    public ResImportOrderDTO findById(Integer id) {
        Long storeId = currentStoreService.getCurrentStoreId();
        ImportOrder order = importOrderRepository.findByIdAndStoreId(id, storeId)
                .orElseThrow(() -> new IdInvalidException("Khong tim thay ImportOrder id=" + id));
        return toResponse(order);
    }

    /**
     * Kept for compatibility with old clients. Workflow transitions use explicit endpoints.
     */
    @Transactional
    public ResImportOrderDTO updateStatus(Integer id, ImportOrderStatusEnum newStatus) {
        ImportOrder order = findLockedOrder(id);
        if (order.getStatus() == ImportOrderStatusEnum.CANCELLED) {
            throw new IdInvalidException("Phieu nhap da bi huỷ, khong the thay doi trang thai");
        }
        if (order.getStatus() == ImportOrderStatusEnum.COMPLETED) {
            throw new IdInvalidException("Phieu nhap da ket thuc, khong the thay doi trang thai");
        }
        if (newStatus != ImportOrderStatusEnum.CANCELLED) {
            throw new IdInvalidException("Chi co the huy phieu qua endpoint status");
        }
        order.setStatus(ImportOrderStatusEnum.CANCELLED);
        return toResponse(importOrderRepository.save(order));
    }

    @Transactional
    public ResImportOrderDTO sendToSupplier(Integer id) {
        ImportOrder order = findLockedOrder(id);
        requireStatus(order, ImportOrderStatusEnum.PENDING);
        order.setStatus(ImportOrderStatusEnum.WAITING_FOR_INSPECTION);
        order.setSentAt(LocalDateTime.now());
        ImportOrder saved = importOrderRepository.save(order);
        importOrderNotificationService.notifyWarehouseInspectionRequired(saved, false);
        return toResponse(saved);
    }

    @Transactional
    public ResImportOrderDTO inspect(Integer id, ReqInspectImportOrderDTO req) {
        ImportOrder order = findLockedOrder(id);
        requireStatus(order, ImportOrderStatusEnum.WAITING_FOR_INSPECTION);
        List<ImportItem> items = importItemRepository.findByImportOrder_Id(id);
        Map<Integer, ImportItem> itemsById = items.stream()
                .collect(Collectors.toMap(ImportItem::getId, item -> item));

        if (req.getItems().size() != items.size()) {
            throw new IdInvalidException("Phai kiem nhan day du tat ca mat hang trong phieu");
        }

        boolean hasDiscrepancy = false;
        Set<Integer> inspectedIds = new HashSet<>();
        for (ReqInspectImportItemDTO inspected : req.getItems()) {
            ImportItem item = itemsById.get(inspected.getImportItemId());
            if (item == null || !inspectedIds.add(inspected.getImportItemId())) {
                throw new IdInvalidException("Danh sach mat hang kiem nhan khong hop le");
            }
            item.setReceivedQuantity(inspected.getReceivedQuantity());
            item.setInspectionNote(inspected.getNote());
            hasDiscrepancy |= !item.getQuantity().equals(inspected.getReceivedQuantity());
        }
        importItemRepository.saveAll(items);

        order.setInspectedAt(LocalDateTime.now());
        order.setInspectedBy(SecurityUtil.requireCurrentUserLogin());
        order.setDiscrepancyNote(req.getNote());
        if (hasDiscrepancy) {
            order.setStatus(ImportOrderStatusEnum.PENDING_DISCREPANCY_APPROVAL);
            importOrderNotificationService.notifyManagerDiscrepancyApprovalRequired(order);
        } else {
            applyReceivedStock(order, items);
            order.setStatus(ImportOrderStatusEnum.COMPLETED);
        }
        return toResponse(importOrderRepository.save(order));
    }

    @Transactional
    public ResImportOrderDTO approveDiscrepancy(Integer id, ReqImportOrderDecisionDTO req) {
        ImportOrder order = findLockedOrder(id);
        requireStatus(order, ImportOrderStatusEnum.PENDING_DISCREPANCY_APPROVAL);
        applyReceivedStock(order, importItemRepository.findByImportOrder_Id(id));
        order.setStatus(ImportOrderStatusEnum.COMPLETED);
        order.setApprovedAt(LocalDateTime.now());
        order.setApprovedBy(SecurityUtil.requireCurrentUserLogin());
        applyDecisionNote(order, req);
        return toResponse(importOrderRepository.save(order));
    }

    @Transactional
    public ResImportOrderDTO rejectDiscrepancy(Integer id, ReqImportOrderDecisionDTO req) {
        ImportOrder order = findLockedOrder(id);
        requireStatus(order, ImportOrderStatusEnum.PENDING_DISCREPANCY_APPROVAL);
        List<ImportItem> items = importItemRepository.findByImportOrder_Id(id);
        items.forEach(item -> {
            item.setReceivedQuantity(null);
            item.setInspectionNote(null);
        });
        importItemRepository.saveAll(items);
        order.setStatus(ImportOrderStatusEnum.WAITING_FOR_INSPECTION);
        order.setInspectedAt(null);
        order.setInspectedBy(null);
        applyDecisionNote(order, req);
        ImportOrder saved = importOrderRepository.save(order);
        importOrderNotificationService.notifyWarehouseInspectionRequired(saved, true);
        return toResponse(saved);
    }

    /**
     * Legacy retry endpoint for orders that already entered PENDING_PAYMENT before this workflow existed.
     */
    @Transactional
    public ResImportOrderDTO payOnly(Integer id, ReqImportOrderDTO req) {
        ImportOrder order = findLockedOrder(id);
        requireStatus(order, ImportOrderStatusEnum.PENDING_PAYMENT);
        double paidAmount = orZero(req.getPaidAmount());
        if (paidAmount <= 0 || req.getFundAccountId() == null) {
            throw new IdInvalidException("So tien va tai khoan thanh toan khong hop le");
        }
        createImportPayment(order, paidAmount, req.getPaymentMethod(), req.getFundAccountId());
        order.setAmountPaid(paidAmount);
        order.setStatus(ImportOrderStatusEnum.COMPLETED);
        return toResponse(importOrderRepository.save(order));
    }

    private void applyReceivedStock(ImportOrder order, List<ImportItem> items) {
        if (order.getStockAppliedAt() != null) {
            throw new IdInvalidException("Ton kho cua phieu nhap da duoc cap nhat");
        }
        for (ImportItem item : items) {
            if (item.getReceivedQuantity() == null) {
                throw new IdInvalidException("Phieu nhap chua duoc kiem nhan day du");
            }
            Product product = productRepository
                    .findByIdAndStoreIdWithLock(item.getProduct().getId(), order.getStore().getId())
                    .orElseThrow(() -> new IdInvalidException("Khong tim thay Product id=" + item.getProduct().getId()));
            int newStock = product.getStock() + item.getReceivedQuantity();
            product.setStock(newStock);
            productRepository.save(product);
            inventoryLogsRepository.save(InventoryLogs.builder()
                    .store(order.getStore())
                    .product(product)
                    .importItem(item)
                    .quantityIn(item.getReceivedQuantity())
                    .balanceAfter(newStock)
                    .currentStock(newStock)
                    .type(TypeInventoryEnum.IMPORT)
                    .createdAt(LocalDateTime.now())
                    .build());
        }
        order.setStockAppliedAt(LocalDateTime.now());
    }

    private List<ImportItem> buildItems(List<ReqImportItemDTO> requests, Long storeId, ImportOrder order) {
        List<ImportItem> items = new ArrayList<>();
        for (ReqImportItemDTO request : requests) {
            Product product = productRepository.findByIdAndStoreIdAndIsDeletedFalse(request.getProductId(), storeId)
                    .orElseThrow(() -> new IdInvalidException("Khong tim thay Product id=" + request.getProductId()));
            items.add(ImportItem.builder()
                    .importOrder(order)
                    .product(product)
                    .quantity(request.getQuantity())
                    .importPrice(request.getImportPrice())
                    .subTotal(request.getImportPrice() * request.getQuantity())
                    .build());
        }
        return items;
    }

    private void validateProductsNotInUnfinishedOrders(List<ReqImportItemDTO> items, Long storeId, Integer excludeOrderId) {
        if (items == null || items.isEmpty()) return;
        List<Integer> reqProductIds = items.stream()
                .map(ReqImportItemDTO::getProductId)
                .toList();

        List<ImportOrderStatusEnum> completedStatuses = List.of(
                ImportOrderStatusEnum.COMPLETED,
                ImportOrderStatusEnum.CANCELLED
        );

        List<Integer> conflictingIds = importItemRepository.findProductIdsInUnfinishedOrders(
                storeId, reqProductIds, completedStatuses, excludeOrderId
        );

        if (!conflictingIds.isEmpty()) {
            throw new IdInvalidException("Sản phẩm ID " + conflictingIds.get(0) + " đang tồn tại trong một phiếu nhập chưa hoàn thành (chờ kiểm hàng/duyệt chênh lệch). Vui lòng hoàn tất phiếu cũ trước.");
        }
    }

    private double calculateTotal(List<ImportItem> items, Double tax, Double discount) {
        double total = items.stream().mapToDouble(ImportItem::getSubTotal).sum() + orZero(tax) - orZero(discount);
        if (total < 0) {
            throw new IdInvalidException("Tong tien don nhap khong duoc am (âm)");
        }
        return total;
    }

    private Supplier findSupplier(Integer supplierId, Long storeId) {
        return supplierRepository.findByIdAndStoreId(supplierId, storeId)
                .orElseThrow(() -> new IdInvalidException("Khong tim thay Supplier id=" + supplierId));
    }

    private ImportOrder findLockedOrder(Integer id) {
        Long storeId = currentStoreService.getCurrentStoreId();
        return importOrderRepository.findByIdAndStoreIdWithLock(id, storeId)
                .orElseThrow(() -> new IdInvalidException("Khong tim thay ImportOrder id=" + id));
    }

    private void requireStatus(ImportOrder order, ImportOrderStatusEnum expected) {
        if (order.getStatus() != expected) {
            throw new IdInvalidException("Trang thai phieu nhap khong hop le. Can " + expected
                    + ", hien tai: " + order.getStatus());
        }
    }

    private ResImportOrderDTO toResponse(ImportOrder order) {
        return DTOMapper.toResImportOrderDTO(order, importItemRepository.findByImportOrder_Id(order.getId()));
    }

    private void applyDecisionNote(ImportOrder order, ReqImportOrderDecisionDTO req) {
        if (req != null && req.getNote() != null && !req.getNote().isBlank()) {
            order.setDiscrepancyNote(req.getNote());
        }
    }

    private double orZero(Double value) {
        return value != null ? value : 0.0;
    }

    private void createImportPayment(ImportOrder order, double paidAmount, String methodValue, Integer fundAccountId) {
        PaymentMethodEnum method = PaymentMethodEnum.CASH;
        if (methodValue != null) {
            try {
                method = PaymentMethodEnum.valueOf(methodValue);
            } catch (IllegalArgumentException ignored) {
                log.warn("Unknown import payment method {}, using CASH", methodValue);
            }
        }
        ReqPaymentDTO payment = new ReqPaymentDTO();
        payment.setReferenceType(RefTypeEnum.IMPORT_ORDER);
        payment.setReferenceId(order.getId());
        payment.setPaymentMethod(method);
        payment.setAmount(BigDecimal.valueOf(paidAmount));
        payment.setFundAccountId(fundAccountId);
        paymentService.createPaymentSession(payment);
    }
}
