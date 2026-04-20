package com.quyen.shoplite.service;

import com.quyen.shoplite.domain.*;
import com.quyen.shoplite.domain.request.ReqImportItemDTO;
import com.quyen.shoplite.domain.request.ReqImportOrderDTO;
import com.quyen.shoplite.domain.response.ResImportOrderDTO;
import com.quyen.shoplite.repository.*;
import com.quyen.shoplite.util.DTOMapper;
import com.quyen.shoplite.util.constant.ImportOrderStatusEnum;
import com.quyen.shoplite.util.constant.TypeInventoryEnum;
import com.quyen.shoplite.util.constant.TypeTransactionEnum;
import com.quyen.shoplite.util.error.IdInvalidException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    private final TransactionRepository transactionRepository;

    // ==================== CREATE ====================

    @Transactional
    public ResImportOrderDTO create(ReqImportOrderDTO req) {
        // 1. Verify supplier exists
        Supplier supplier = supplierRepository.findById(req.getSupplierId())
                .orElseThrow(() -> new IdInvalidException("Không tìm thấy Supplier id=" + req.getSupplierId()));

        // 2. Validate each item product & compute subtotals
        List<ImportItem> itemsToSave = new ArrayList<>();
        double subtotalSum = 0.0;

        for (ReqImportItemDTO itemReq : req.getItems()) {
            Product product = productRepository.findById(itemReq.getProductId())
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
            for (ImportItem item : savedItems) {
                Product product = productRepository.findByIdWithLock(item.getProduct().getId())
                        .orElseThrow(() -> new IdInvalidException(
                                "Không tìm thấy Product id=" + item.getProduct().getId()));
                int addedQty = item.getQuantity();
                int newStock = product.getStock() + addedQty;
                product.setStock(newStock);
                productRepository.save(product);

                inventoryLogsRepository.save(InventoryLogs.builder()
                        .product(product)
                        .importItem(item)
                        .quantityIn(addedQty)
                        .balanceAfter(newStock)
                        .currentStock(newStock)
                        .type(TypeInventoryEnum.IMPORT)
                        .createdAt(LocalDateTime.now())
                        .build());
            }

            // Create EXPENSE transaction with actual paidAmount if > 0
            if (paidAmount > 0) {
                transactionRepository.save(Transaction.builder()
                        .importOrder(savedOrder)
                        .amount(paidAmount) // Create expense transaction with what was actually paid
                        .type(TypeTransactionEnum.EXPENSE)
                        .content("Chi tiền nhập hàng - ImportOrder #" + savedOrder.getId() + (req.getPaymentMethod() != null ? " (" + req.getPaymentMethod() + ")" : ""))
                        .transactionTime(LocalDateTime.now())
                        .createdAt(LocalDateTime.now())
                        .build());
            }
            log.info("[ImportOrder] Completed import order upon creation id={}, total={}, paid={}",
                    savedOrder.getId(), savedOrder.getTotalAmount(), paidAmount);
        }

        return DTOMapper.toResImportOrderDTO(savedOrder, savedItems);
    }

    // ==================== UPDATE ====================

    @Transactional
    public ResImportOrderDTO update(Integer id, ReqImportOrderDTO req) {
        ImportOrder order = importOrderRepository.findById(id)
                .orElseThrow(() -> new IdInvalidException("Không tìm thấy ImportOrder id=" + id));

        if (order.getStatus() != ImportOrderStatusEnum.PENDING) {
            throw new IdInvalidException("Chỉ có thể sửa đơn nhập khi đang ở trạng thái phiếu tạm (PENDING)");
        }

        Supplier supplier = supplierRepository.findById(req.getSupplierId())
                .orElseThrow(() -> new IdInvalidException("Không tìm thấy Supplier id=" + req.getSupplierId()));

        // Delete old items
        List<ImportItem> oldItems = importItemRepository.findByImportOrder_Id(id);
        importItemRepository.deleteAll(oldItems);

        // Process new items
        List<ImportItem> itemsToSave = new ArrayList<>();
        double subtotalSum = 0.0;

        for (ReqImportItemDTO itemReq : req.getItems()) {
            Product product = productRepository.findById(itemReq.getProductId())
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
            for (ImportItem item : savedItems) {
                Product product = productRepository.findByIdWithLock(item.getProduct().getId())
                        .orElseThrow(() -> new IdInvalidException(
                                "Không tìm thấy Product id=" + item.getProduct().getId()));
                int addedQty = item.getQuantity();
                int newStock = product.getStock() + addedQty;
                product.setStock(newStock);
                productRepository.save(product);

                inventoryLogsRepository.save(InventoryLogs.builder()
                        .product(product)
                        .importItem(item)
                        .quantityIn(addedQty)
                        .balanceAfter(newStock)
                        .currentStock(newStock)
                        .type(TypeInventoryEnum.IMPORT)
                        .createdAt(LocalDateTime.now())
                        .build());
            }

            if (paidAmount > 0) {
                transactionRepository.save(Transaction.builder()
                        .importOrder(savedOrder)
                        .amount(paidAmount)
                        .type(TypeTransactionEnum.EXPENSE)
                        .content("Chi tiền nhập hàng - ImportOrder #" + savedOrder.getId() + (req.getPaymentMethod() != null ? " (" + req.getPaymentMethod() + ")" : ""))
                        .transactionTime(LocalDateTime.now())
                        .createdAt(LocalDateTime.now())
                        .build());
            }
            log.info("[ImportOrder] Completed import order upon update id={}, total={}, paid={}",
                    savedOrder.getId(), savedOrder.getTotalAmount(), paidAmount);
        }

        return DTOMapper.toResImportOrderDTO(savedOrder, savedItems);
    }

    // ==================== FIND ALL ====================

    public List<ResImportOrderDTO> findAll() {
        return importOrderRepository.findAll().stream()
                .map(order -> DTOMapper.toResImportOrderDTO(
                        order,
                        importItemRepository.findByImportOrder_Id(order.getId())))
                .toList();
    }

    // ==================== FIND BY ID ====================

    public ResImportOrderDTO findById(Integer id) {
        ImportOrder order = importOrderRepository.findById(id)
                .orElseThrow(() -> new IdInvalidException("Không tìm thấy ImportOrder id=" + id));
        List<ImportItem> items = importItemRepository.findByImportOrder_Id(id);
        return DTOMapper.toResImportOrderDTO(order, items);
    }

    // ==================== UPDATE STATUS ====================

    @Transactional
    public ResImportOrderDTO updateStatus(Integer id, ImportOrderStatusEnum newStatus) {
        // BUG-08: Pessimistic lock to prevent concurrent status transitions
        ImportOrder order = importOrderRepository.findByIdWithLock(id)
                .orElseThrow(() -> new IdInvalidException("Không tìm thấy ImportOrder id=" + id));

        ImportOrderStatusEnum currentStatus = order.getStatus();

        // Guard: already cancelled or completed cannot change
        if (currentStatus == ImportOrderStatusEnum.CANCELLED) {
            throw new IdInvalidException("Đơn nhập đã bị huỷ, không thể thay đổi trạng thái");
        }
        if (currentStatus == ImportOrderStatusEnum.COMPLETED) {
            throw new IdInvalidException("Đơn nhập đã hoàn tất, không thể thay đổi trạng thái");
        }

        order.setStatus(newStatus);
        ImportOrder savedOrder = importOrderRepository.save(order);

        if (newStatus == ImportOrderStatusEnum.COMPLETED) {
            // Guard against duplicate completion side effects (extra safety net)
            if (transactionRepository.existsByImportOrder_Id(id)) {
                throw new IdInvalidException("Đơn nhập id=" + id + " đã được xử lý hoàn tất trước đó");
            }

            // Load items
            List<ImportItem> items = importItemRepository.findByImportOrder_Id(id);

            // For each item: increase product stock + write IMPORT inventory log
            for (ImportItem item : items) {
                // BUG-08: Lock product row before modifying stock
                Product product = productRepository.findByIdWithLock(item.getProduct().getId())
                        .orElseThrow(() -> new IdInvalidException(
                                "Không tìm thấy Product id=" + item.getProduct().getId()));
                int addedQty = item.getQuantity();
                int newStock = product.getStock() + addedQty;
                product.setStock(newStock);
                productRepository.save(product);

                inventoryLogsRepository.save(InventoryLogs.builder()
                        .product(product)
                        .importItem(item)
                        .quantityIn(addedQty)
                        .balanceAfter(newStock)
                        .currentStock(newStock)
                        .type(TypeInventoryEnum.IMPORT)
                        .createdAt(LocalDateTime.now())
                        .build());
            }

            // Create EXPENSE transaction for the import order
            transactionRepository.save(Transaction.builder()
                    .importOrder(savedOrder)
                    .amount(savedOrder.getTotalAmount())
                    .type(TypeTransactionEnum.EXPENSE)
                    .content("Chi tiền nhập hàng - ImportOrder #" + savedOrder.getId())
                    .transactionTime(LocalDateTime.now())
                    .createdAt(LocalDateTime.now())
                    .build());

            log.info("[ImportOrder] Completed import order id={}, total={}, items={}",
                    id, savedOrder.getTotalAmount(), items.size());
        }

        return DTOMapper.toResImportOrderDTO(savedOrder,
                importItemRepository.findByImportOrder_Id(id));
    }
}
