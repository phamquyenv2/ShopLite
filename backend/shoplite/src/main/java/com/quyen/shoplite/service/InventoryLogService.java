package com.quyen.shoplite.service;

import com.quyen.shoplite.domain.InventoryLogs;
import com.quyen.shoplite.domain.Product;
import com.quyen.shoplite.domain.request.ReqInventoryLogDTO;
import com.quyen.shoplite.domain.response.ResInventoryLogDTO;
import com.quyen.shoplite.repository.InventoryLogsRepository;
import com.quyen.shoplite.repository.ProductRepository;
import com.quyen.shoplite.util.DTOMapper;
import com.quyen.shoplite.util.error.IdInvalidException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class InventoryLogService {

    private final InventoryLogsRepository inventoryLogsRepository;
    private final ProductRepository productRepository;
    private final CurrentStoreService currentStoreService;

    public ResInventoryLogDTO create(ReqInventoryLogDTO req) {
        Long storeId = currentStoreService.getCurrentStoreId();
        Product product = productRepository.findByIdAndStoreIdAndIsDeletedFalse(req.getProductId(), storeId)
                .orElseThrow(() -> new IdInvalidException("Không tìm thấy Product id=" + req.getProductId()));

        int changeQty = req.getChangeQuantity();
        int currentStock = product.getStock();
        int newStock = currentStock + changeQty;

        if (newStock < 0) {
            throw new IdInvalidException("Tồn kho không đủ cho Product id=" + req.getProductId());
        }

        InventoryLogs log = InventoryLogs.builder()
                .store(product.getStore())
                .product(product)
                .quantityIn(changeQty > 0 ? changeQty : null)
                .quantityOut(changeQty < 0 ? -changeQty : null)
                .balanceAfter(newStock)
                .currentStock(newStock)
                .type(req.getType())
                .createdAt(LocalDateTime.now())
                .build();

        product.setStock(newStock);
        productRepository.save(product);

        return DTOMapper.toResInventoryLogDTO(inventoryLogsRepository.save(log));
    }

    public List<ResInventoryLogDTO> findAll() {
        Long storeId = currentStoreService.getCurrentStoreId();
        return inventoryLogsRepository.findAllByStoreIdOrderByCreatedAtDesc(storeId).stream()
                .map(DTOMapper::toResInventoryLogDTO)
                .toList();
    }

    public List<ResInventoryLogDTO> findByProductId(Integer productId) {
        Long storeId = currentStoreService.getCurrentStoreId();
        return inventoryLogsRepository.findAllByStoreIdAndProduct_Id(storeId, productId).stream()
                .map(DTOMapper::toResInventoryLogDTO)
                .toList();
    }
}
