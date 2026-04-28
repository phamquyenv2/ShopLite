package com.quyen.shoplite.service;

import com.quyen.shoplite.domain.*;
import com.quyen.shoplite.domain.request.ReqImportItemDTO;
import com.quyen.shoplite.domain.request.ReqImportOrderDTO;
import com.quyen.shoplite.domain.request.ReqPaymentDTO;
import com.quyen.shoplite.domain.response.ResImportOrderDTO;
import com.quyen.shoplite.repository.*;
import com.quyen.shoplite.util.DTOMapper;
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
import java.util.ArrayList;
import java.util.List;

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

    // ==================== CREATE ====================

    @Transactional
    public ResImportOrderDTO create(ReqImportOrderDTO req) {
        Store store = currentStoreService.getCurrentStore();
        Long storeId = store.getId();
        // 1. Verify supplier exists
        Supplier supplier = supplierRepository.findByIdAndStoreId(req.getSupplierId(), storeId)
                .orElseThrow(() -> new IdInvalidException("Không tìm thấy Supplier id=" + req.getSupplierId()));

        // 2. Validate each item product & compute subtotals
        List<ImportItem> itemsToSave = new ArrayList<>();
        double subtotalSum = 0.0;

        for (ReqImportItemDTO itemReq : req.getItems()) {
            Product product = productRepository.findByIdAndStoreIdAndIsDeletedFalse(itemReq.getProductId(), storeId)
                    .orElseThrow(() -> new IdInvalidException(
                            "Không tìm thấy Product id=" + itemReq.getProductId()));

            double subTotal = itemReq.getImportPrice() * itemReq.getQuantity();
            subtotalSum += subTotal;

            itemsToSave.add(ImportItem.builder()
                    .product(product)
                    .quantity(itemReq.getQuantity())
                    .importPrice(itemReq.getImportPrice())
                    .subTotal(subTotal)
                    .build());
        }

        // 3. Compute total = sum(subtotals) + tax - discount
        double tax = req.getTax() != null ? req.getTax() : 0.0;
        double discount = req.getDiscount() != null ? req.getDiscount() : 0.0;
        double totalAmount = subtotalSum + tax - discount;

        if (totalAmount < 0) {
            throw new IdInvalidException("Tổng tiền đơn nhập không được âm");
        }

        Double paidAmount = req.getPaidAmount() != null ? req.getPaidAmount() : 0.0;
        ImportOrderStatusEnum status = req.getStatus() != null ? req.getStatus() : ImportOrderStatusEnum.PENDING;

        // 4. Save ImportOrder
        ImportOrder importOrder = ImportOrder.builder()
                .store(store)
                .supplier(supplier)
                .tax(tax)
                .discount(discount)
                .totalAmount(totalAmount)
                .amountPaid(paidAmount)
                .status(status)
                .note(req.getNote())
                .createdAt(LocalDateTime.now())
                .build();
        ImportOrder savedOrder = importOrderRepository.save(importOrder);

        // 5. Save ImportItems (link to saved order)
        for (ImportItem item : itemsToSave) {
            item.setImportOrder(savedOrder);
        }
        List<ImportItem> savedItems = importItemRepository.saveAll(itemsToSave);

        // 6. Handle COMPLETED status on creation
        if (status == ImportOrderStatusEnum.COMPLETED) {
            // Bước 1: Cập nhật stock và chuyển sang PENDING_PAYMENT
            // → Nếu payment bị lỗi sau đây, order vẫn ở PENDING_PAYMENT (không rollback stock)
            processCompletedImportOrder(savedOrder, savedItems);
            savedOrder.setStatus(ImportOrderStatusEnum.PENDING_PAYMENT);
            savedOrder = importOrderRepository.save(savedOrder);

            // Bước 2: Tạo Payment → nếu được thì chuyển sang COMPLETED
            if (paidAmount > 0 && req.getFundAccountId() != null) {
                createImportPayment(savedOrder, paidAmount, req.getPaymentMethod(), req.getFundAccountId());
            }
            savedOrder.setStatus(ImportOrderStatusEnum.COMPLETED);
            savedOrder = importOrderRepository.save(savedOrder);

            log.info("[ImportOrder] Completed import order upon creation id={}, total={}, paid={}",
                    savedOrder.getId(), savedOrder.getTotalAmount(), paidAmount);
        }

        return DTOMapper.toResImportOrderDTO(savedOrder, savedItems);
    }

    // ==================== UPDATE ====================

    @Transactional
    public ResImportOrderDTO update(Integer id, ReqImportOrderDTO req) {
        Long storeId = currentStoreService.getCurrentStoreId();
        ImportOrder order = importOrderRepository.findByIdAndStoreId(id, storeId)
                .orElseThrow(() -> new IdInvalidException("Không tìm thấy ImportOrder id=" + id));

        if (order.getStatus() != ImportOrderStatusEnum.PENDING) {
            throw new IdInvalidException("Chỉ có thể sửa đơn nhập khi đang ở trạng thái phiếu tạm (PENDING)");
        }

        Supplier supplier = supplierRepository.findByIdAndStoreId(req.getSupplierId(), storeId)
                .orElseThrow(() -> new IdInvalidException("Không tìm thấy Supplier id=" + req.getSupplierId()));

        // Delete old items
        List<ImportItem> oldItems = importItemRepository.findByImportOrder_Id(id);
        importItemRepository.deleteAll(oldItems);

        // Process new items
        List<ImportItem> itemsToSave = new ArrayList<>();
        double subtotalSum = 0.0;

        for (ReqImportItemDTO itemReq : req.getItems()) {
            Product product = productRepository.findByIdAndStoreIdAndIsDeletedFalse(itemReq.getProductId(), storeId)
                    .orElseThrow(() -> new IdInvalidException(
                            "Không tìm thấy Product id=" + itemReq.getProductId()));

            double subTotal = itemReq.getImportPrice() * itemReq.getQuantity();
            subtotalSum += subTotal;

            itemsToSave.add(ImportItem.builder()
                    .product(product)
                    .quantity(itemReq.getQuantity())
                    .importPrice(itemReq.getImportPrice())
                    .subTotal(subTotal)
                    .importOrder(order)
                    .build());
        }

        double tax = req.getTax() != null ? req.getTax() : 0.0;
        double discount = req.getDiscount() != null ? req.getDiscount() : 0.0;
        double totalAmount = subtotalSum + tax - discount;

        if (totalAmount < 0) {
            throw new IdInvalidException("Tổng tiền đơn nhập không được âm");
        }

        Double paidAmount = req.getPaidAmount() != null ? req.getPaidAmount() : 0.0;
        ImportOrderStatusEnum status = req.getStatus() != null ? req.getStatus() : ImportOrderStatusEnum.PENDING;

        order.setSupplier(supplier);
        order.setTax(tax);
        order.setDiscount(discount);
        order.setTotalAmount(totalAmount);
        order.setAmountPaid(paidAmount);
        order.setStatus(status);
        order.setNote(req.getNote());
        
        ImportOrder savedOrder = importOrderRepository.save(order);
        List<ImportItem> savedItems = importItemRepository.saveAll(itemsToSave);

        // Handle COMPLETED status on update
        if (status == ImportOrderStatusEnum.COMPLETED) {
            // Bước 1: Cập nhật stock + chuyển PENDING_PAYMENT
            processCompletedImportOrder(savedOrder, savedItems);
            savedOrder.setStatus(ImportOrderStatusEnum.PENDING_PAYMENT);
            savedOrder = importOrderRepository.save(savedOrder);

            // Bước 2: Payment → COMPLETED
            if (paidAmount > 0 && req.getFundAccountId() != null) {
                createImportPayment(savedOrder, paidAmount, req.getPaymentMethod(), req.getFundAccountId());
            }
            savedOrder.setStatus(ImportOrderStatusEnum.COMPLETED);
            savedOrder = importOrderRepository.save(savedOrder);

            log.info("[ImportOrder] Completed import order upon update id={}, total={}, paid={}",
                    savedOrder.getId(), savedOrder.getTotalAmount(), paidAmount);
        }

        return DTOMapper.toResImportOrderDTO(savedOrder, savedItems);
    }

    // ==================== FIND ALL ====================

    public List<ResImportOrderDTO> findAll() {
        Long storeId = currentStoreService.getCurrentStoreId();
        return importOrderRepository.findAllByStoreIdOrderByCreatedAtDesc(storeId).stream()
                .map(order -> DTOMapper.toResImportOrderDTO(
                        order,
                        importItemRepository.findByImportOrder_Id(order.getId())))
                .toList();
    }

    // ==================== FIND BY ID ====================

    public ResImportOrderDTO findById(Integer id) {
        Long storeId = currentStoreService.getCurrentStoreId();
        ImportOrder order = importOrderRepository.findByIdAndStoreId(id, storeId)
                .orElseThrow(() -> new IdInvalidException("Không tìm thấy ImportOrder id=" + id));
        List<ImportItem> items = importItemRepository.findByImportOrder_Id(id);
        return DTOMapper.toResImportOrderDTO(order, items);
    }

    // ==================== UPDATE STATUS ====================

    @Transactional
    public ResImportOrderDTO updateStatus(Integer id, ImportOrderStatusEnum newStatus) {
        Long storeId = currentStoreService.getCurrentStoreId();
        ImportOrder order = importOrderRepository.findByIdAndStoreIdWithLock(id, storeId)
                .orElseThrow(() -> new IdInvalidException("Không tìm thấy ImportOrder id=" + id));

        ImportOrderStatusEnum currentStatus = order.getStatus();

        if (currentStatus == ImportOrderStatusEnum.CANCELLED) {
            throw new IdInvalidException("Đơn nhập đã bị huỷ, không thể thay đổi trạng thái");
        }
        if (currentStatus == ImportOrderStatusEnum.COMPLETED) {
            throw new IdInvalidException("Đơn nhập đã hoàn tất, không thể thay đổi trạng thái");
        }

        order.setStatus(newStatus);
        ImportOrder savedOrder = importOrderRepository.save(order);

        if (newStatus == ImportOrderStatusEnum.COMPLETED) {
            List<ImportItem> items = importItemRepository.findByImportOrder_Id(id);
            processCompletedImportOrder(savedOrder, items);

            // Tạo Payment cho toàn bộ totalAmount khi hoàn tất qua updateStatus
            // NOTE: fundAccountId cần được truyền từ frontend — tạm không tạo Payment ở đây
            // Việc tạo Payment sẽ do frontend gọi POST /api/v1/payment riêng

            log.info("[ImportOrder] Completed import order id={}, total={}, items={}",
                    id, savedOrder.getTotalAmount(), items.size());
        }

        return DTOMapper.toResImportOrderDTO(savedOrder,
                importItemRepository.findByImportOrder_Id(id));
    }

    // ==================== PAY ONLY (Retry) ====================

    /**
     * Thực hiện payment cho đơn đang ở PENDING_PAYMENT.
     * Không update stock (stock đã được cập nhật ở bước confirm trước đó).
     * Idempotent: gọi lại nhiều lần cũng chỉ tạo 1 payment.
     */
    @Transactional
    public ResImportOrderDTO payOnly(Integer id, ReqImportOrderDTO req) {
        Long storeId = currentStoreService.getCurrentStoreId();
        ImportOrder order = importOrderRepository.findByIdAndStoreIdWithLock(id, storeId)
                .orElseThrow(() -> new IdInvalidException("Không tìm thấy ImportOrder id=" + id));

        if (order.getStatus() != ImportOrderStatusEnum.PENDING_PAYMENT) {
            throw new IdInvalidException(
                "Chỉ có thể thanh toán lại cho đơn đang ở trạng thái PENDING_PAYMENT, " +
                "hiện tại: " + order.getStatus());
        }

        double paidAmount = req.getPaidAmount() != null ? req.getPaidAmount() : 0.0;
        if (paidAmount <= 0) {
            throw new IdInvalidException("Số tiền thanh toán phải lớn hơn 0");
        }
        if (req.getFundAccountId() == null) {
            throw new IdInvalidException("Vui lòng chọn tài khoản thanh toán");
        }

        createImportPayment(order, paidAmount, req.getPaymentMethod(), req.getFundAccountId());

        order.setAmountPaid(paidAmount);
        order.setStatus(ImportOrderStatusEnum.COMPLETED);
        ImportOrder saved = importOrderRepository.save(order);

        log.info("[ImportOrder] payOnly completed for id={}, paid={}", id, paidAmount);
        return DTOMapper.toResImportOrderDTO(saved, importItemRepository.findByImportOrder_Id(id));
    }

    // ==================== PRIVATE HELPERS ====================

    private void processCompletedImportOrder(ImportOrder savedOrder, List<ImportItem> items) {
        for (ImportItem item : items) {
            Product product = productRepository.findByIdAndStoreIdWithLock(item.getProduct().getId(), savedOrder.getStore().getId())
                    .orElseThrow(() -> new IdInvalidException(
                            "Không tìm thấy Product id=" + item.getProduct().getId()));
            int addedQty = item.getQuantity();
            int newStock = product.getStock() + addedQty;
            product.setStock(newStock);
            productRepository.save(product);

            inventoryLogsRepository.save(InventoryLogs.builder()
                    .store(savedOrder.getStore())
                    .product(product)
                    .importItem(item)
                    .quantityIn(addedQty)
                    .balanceAfter(newStock)
                    .currentStock(newStock)
                    .type(TypeInventoryEnum.IMPORT)
                    .createdAt(LocalDateTime.now())
                    .build());
        }
    }

    /**
     * Tạo Payment cho ImportOrder thông qua PaymentService (qua Payment → Transaction → FundAccount).
     */
    private void createImportPayment(ImportOrder importOrder, double paidAmount,
                                     String paymentMethodStr, Integer fundAccountId) {
        PaymentMethodEnum method = PaymentMethodEnum.CASH;
        if (paymentMethodStr != null) {
            try {
                method = PaymentMethodEnum.valueOf(paymentMethodStr);
            } catch (IllegalArgumentException ignored) {}
        }

        ReqPaymentDTO paymentReq = new ReqPaymentDTO();
        paymentReq.setReferenceType(RefTypeEnum.IMPORT_ORDER);
        paymentReq.setReferenceId(importOrder.getId());
        paymentReq.setPaymentMethod(method);
        paymentReq.setAmount(BigDecimal.valueOf(paidAmount));
        paymentReq.setFundAccountId(fundAccountId);
        paymentService.createPaymentSession(paymentReq);
    }
}
