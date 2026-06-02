package com.quyen.shoplite.service;

import com.quyen.shoplite.domain.*;
import com.quyen.shoplite.domain.request.ReqImportOrderDecisionDTO;
import com.quyen.shoplite.domain.request.ReqInspectImportItemDTO;
import com.quyen.shoplite.domain.request.ReqInspectImportOrderDTO;
import com.quyen.shoplite.repository.*;
import com.quyen.shoplite.util.constant.ImportOrderStatusEnum;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ImportOrderInspectionWorkflowTest {

    @Mock private ImportOrderRepository importOrderRepository;
    @Mock private ImportItemRepository importItemRepository;
    @Mock private SupplierRepository supplierRepository;
    @Mock private ProductRepository productRepository;
    @Mock private InventoryLogsRepository inventoryLogsRepository;
    @Mock private PaymentService paymentService;
    @Mock private CurrentStoreService currentStoreService;
    @Mock private ImportOrderNotificationService importOrderNotificationService;
    @InjectMocks private ImportOrderService service;

    @AfterEach
    void clearAuthentication() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void matchingInspectionCompletesAndAppliesReceivedStock() {
        Fixture f = fixture(ImportOrderStatusEnum.WAITING_FOR_INSPECTION, 4);
        authenticate("warehouse");
        mockLockedOrder(f);
        when(productRepository.findByIdAndStoreIdWithLock(10, 1L)).thenReturn(Optional.of(f.product));

        service.inspect(1, inspectRequest(100, 4));

        assertEquals(ImportOrderStatusEnum.COMPLETED, f.order.getStatus());
        assertEquals(14, f.product.getStock());
        assertNotNull(f.order.getStockAppliedAt());
        verify(inventoryLogsRepository).save(any(InventoryLogs.class));
    }

    @Test
    void discrepancyWaitsForManagerWithoutApplyingStock() {
        Fixture f = fixture(ImportOrderStatusEnum.WAITING_FOR_INSPECTION, 4);
        authenticate("warehouse");
        mockLockedOrder(f);

        service.inspect(1, inspectRequest(100, 2));

        assertEquals(ImportOrderStatusEnum.PENDING_DISCREPANCY_APPROVAL, f.order.getStatus());
        assertEquals(2, f.item.getReceivedQuantity());
        assertEquals(10, f.product.getStock());
        verify(productRepository, never()).save(any());
    }

    @Test
    void managerApprovalAppliesActualQuantity() {
        Fixture f = fixture(ImportOrderStatusEnum.PENDING_DISCREPANCY_APPROVAL, 4);
        f.item.setReceivedQuantity(2);
        authenticate("manager");
        mockLockedOrder(f);
        when(productRepository.findByIdAndStoreIdWithLock(10, 1L)).thenReturn(Optional.of(f.product));

        service.approveDiscrepancy(1, new ReqImportOrderDecisionDTO());

        assertEquals(ImportOrderStatusEnum.COMPLETED, f.order.getStatus());
        assertEquals(12, f.product.getStock());
        assertEquals("manager", f.order.getApprovedBy());
    }

    @Test
    void managerRejectionReturnsOrderForAnotherInspection() {
        Fixture f = fixture(ImportOrderStatusEnum.PENDING_DISCREPANCY_APPROVAL, 4);
        f.item.setReceivedQuantity(2);
        mockLockedOrder(f);

        service.rejectDiscrepancy(1, new ReqImportOrderDecisionDTO());

        assertEquals(ImportOrderStatusEnum.WAITING_FOR_INSPECTION, f.order.getStatus());
        assertNull(f.item.getReceivedQuantity());
        verify(productRepository, never()).save(any());
    }

    @Test
    void legacyStatusEndpointCannotCompleteWithoutInspection() {
        Fixture f = fixture(ImportOrderStatusEnum.PENDING, 4);
        mockLockedOrder(f);

        assertThrows(RuntimeException.class,
                () -> service.updateStatus(1, ImportOrderStatusEnum.COMPLETED));
        verify(productRepository, never()).save(any());
    }

    private void mockLockedOrder(Fixture f) {
        when(currentStoreService.getCurrentStoreId()).thenReturn(1L);
        when(importOrderRepository.findByIdAndStoreIdWithLock(1, 1L)).thenReturn(Optional.of(f.order));
        lenient().when(importOrderRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        lenient().when(importItemRepository.findByImportOrder_Id(1)).thenReturn(List.of(f.item));
        lenient().when(productRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        lenient().when(inventoryLogsRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    private ReqInspectImportOrderDTO inspectRequest(int itemId, int receivedQuantity) {
        ReqInspectImportItemDTO item = new ReqInspectImportItemDTO();
        item.setImportItemId(itemId);
        item.setReceivedQuantity(receivedQuantity);
        ReqInspectImportOrderDTO request = new ReqInspectImportOrderDTO();
        request.setItems(List.of(item));
        return request;
    }

    private void authenticate(String username) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(username, "password", List.of()));
    }

    private Fixture fixture(ImportOrderStatusEnum status, int orderedQuantity) {
        Store store = new Store();
        store.setId(1L);
        ImportOrder order = new ImportOrder();
        order.setId(1);
        order.setStore(store);
        order.setStatus(status);
        Product product = new Product();
        product.setId(10);
        product.setStock(10);
        ImportItem item = new ImportItem();
        item.setId(100);
        item.setImportOrder(order);
        item.setProduct(product);
        item.setQuantity(orderedQuantity);
        return new Fixture(order, item, product);
    }

    private record Fixture(ImportOrder order, ImportItem item, Product product) {}
}
