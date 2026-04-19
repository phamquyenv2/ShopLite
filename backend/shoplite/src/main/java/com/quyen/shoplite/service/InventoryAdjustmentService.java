package com.quyen.shoplite.service;

import com.quyen.shoplite.domain.*;
import com.quyen.shoplite.domain.request.ReqAdjustmentItemDTO;
import com.quyen.shoplite.domain.request.ReqInventoryAdjustmentDTO;
import com.quyen.shoplite.domain.response.ResInventoryAdjustmentDTO;
import com.quyen.shoplite.repository.InventoryAdjustmentRepository;
import com.quyen.shoplite.repository.InventoryLogsRepository;
import com.quyen.shoplite.repository.ProductRepository;
import com.quyen.shoplite.util.DTOMapper;
import com.quyen.shoplite.util.constant.TypeInventoryEnum;
import com.quyen.shoplite.util.error.BadRequestException;
import com.quyen.shoplite.util.error.ResourceNotFoundException;
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
public class InventoryAdjustmentService {

    private final InventoryAdjustmentRepository adjustmentRepository;
    private final InventoryLogsRepository       inventoryLogsRepository;
    private final ProductRepository             productRepository;

    // ==================== CREATE ====================

    /**
     * Creates an inventory adjustment session.
     * For each item: delta = actualQuantity - currentStock.
     * delta == 0  → BadRequestException (no change needed)
     * delta  < 0  and currentStock + delta < 0 → BadRequestException (insufficient stock)
     * All stock updates and logs are written atomically within the transaction.
     */
    @Transactional
    public ResInventoryAdjustmentDTO create(ReqInventoryAdjustmentDTO req) {

        // ── 1. Persist the adjustment header ──────────────────────────────
        InventoryAdjustment adjustment = adjustmentRepository.save(
                InventoryAdjustment.builder()
                        .reason(req.getReason())
                        .note(req.getNote())
                        .createdBy(req.getCreatedBy())
                        .createdAt(LocalDateTime.now())
                        .build());

        // ── 2. Validate all items upfront (fail-fast before touching stock) ─
        record ItemWork(Product product, int delta) {}
        List<ItemWork> workItems = new ArrayList<>();

        for (ReqAdjustmentItemDTO itemReq : req.getItems()) {
            // 2a. Product must exist — lock row to prevent concurrent stock corruption (BUG-08)
            Product product = productRepository.findByIdWithLock(itemReq.getProductId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Không tìm thấy Product id=" + itemReq.getProductId()));

            int delta = itemReq.getActualQuantity() - product.getStock();

            // 2b. Zero delta → nothing to adjust
            if (delta == 0) {
                throw new BadRequestException(
                        "Product id=" + itemReq.getProductId()
                        + ": Số lượng thực tế bằng số lượng tồn kho");
            }

            // 2c. Decrease cannot make stock negative
            int resultingStock = product.getStock() + delta;
            if (resultingStock < 0) {
                throw new BadRequestException(
                        "Product id=" + itemReq.getProductId()
                        + ": số lượng điều chỉnh sẽ làm tồn kho âm");
            }

            workItems.add(new ItemWork(product, delta));
        }

        // ── 3. Apply stock updates + create logs (all or nothing via @Transactional) ─
        List<InventoryLogs> savedLogs = new ArrayList<>();

        for (ItemWork work : workItems) {
            Product product = work.product();
            int     delta   = work.delta();
            int     newStock = product.getStock() + delta;

            // Update stock
            product.setStock(newStock);
            productRepository.save(product);

            // Write ADJUST inventory log
            InventoryLogs log = InventoryLogs.builder()
                    .product(product)
                    .adjustment(adjustment)
                    .quantityIn(delta  > 0 ? delta  : null)
                    .quantityOut(delta < 0 ? -delta : null)
                    .balanceAfter(newStock)
                    .currentStock(newStock)
                    .type(TypeInventoryEnum.ADJUST)
                    .createdAt(LocalDateTime.now())
                    .build();

            savedLogs.add(inventoryLogsRepository.save(log));
        }

        log.info("Đã tạo điều chỉnh kho id={}, items={}, by={}",
                adjustment.getId(), workItems.size(), adjustment.getCreatedBy());

        return DTOMapper.toResInventoryAdjustmentDTO(adjustment, savedLogs);
    }

    // ==================== FIND ALL ====================

    public List<ResInventoryAdjustmentDTO> findAll() {
        return adjustmentRepository.findAll().stream()
                .map(adj -> DTOMapper.toResInventoryAdjustmentDTO(
                        adj,
                        inventoryLogsRepository.findByAdjustment_Id(adj.getId())))
                .toList();
    }

    // ==================== FIND BY ID ====================

    public ResInventoryAdjustmentDTO findById(Integer id) {
        InventoryAdjustment adjustment = adjustmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy điều chỉnh kho id=" + id));
        List<InventoryLogs> logs = inventoryLogsRepository.findByAdjustment_Id(id);
        return DTOMapper.toResInventoryAdjustmentDTO(adjustment, logs);
    }
}
