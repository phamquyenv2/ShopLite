package com.quyen.shoplite.service;

import com.quyen.shoplite.repository.*;
import com.quyen.shoplite.util.constant.ImportOrderStatusEnum;
import com.quyen.shoplite.util.constant.TypeInventoryEnum;
import com.quyen.shoplite.util.constant.TypeInventoryEnum;
import com.quyen.shoplite.util.error.IdInvalidException;

import com.quyen.shoplite.domain.*;
import com.quyen.shoplite.domain.Store;
import com.quyen.shoplite.domain.request.ReqImportItemDTO;
import com.quyen.shoplite.domain.request.ReqImportOrderDTO;
import com.quyen.shoplite.domain.response.ResImportOrderDTO;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ImportOrderServiceTest {

    // ------------------------------------------------------------------ mocks
    @Mock
    private ImportOrderRepository importOrderRepository;
    @Mock
    private ImportItemRepository importItemRepository;
    @Mock
    private SupplierRepository supplierRepository;
    @Mock
    private ProductRepository productRepository;
    @Mock
    private InventoryLogsRepository inventoryLogsRepository;
    @Mock
    private PaymentService paymentService;
    @Mock
    private CurrentStoreService currentStoreService;

    @InjectMocks
    private ImportOrderService importOrderService;

    private Store testStore() {
        Store store = new Store();
        store.setId(1L);
        return store;
    }

    // Bean-validation validator (no Spring context needed)
    private Validator validator;

    // ---------------------------------------------------------------- helpers
    @BeforeEach
    void setUp() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    /**
     * Build a minimal valid Supplier stub
     */
    private Supplier makeSupplier(int id) {
        Supplier s = new Supplier();
        s.setId(id);
        s.setName("Supplier " + id);
        return s;
    }

    /**
     * Build a minimal valid Product stub
     */
    private Product makeProduct(int id, int stock) {
        Product p = new Product();
        p.setId(id);
        p.setName("Product " + id);
        p.setSku("SKU-" + id);
        p.setStock(stock);
        p.setSellingPrice(10.0);
        p.setCostPrice(0.0);
        return p;
    }

    /**
     * Build a single-item request with the given price / quantity
     */
    private ReqImportOrderDTO buildRequest(int supplierId, int productId,
            int qty, double price,
            Double tax, Double discount) {
        ReqImportItemDTO item = new ReqImportItemDTO();
        item.setProductId(productId);
        item.setQuantity(qty);
        item.setImportPrice(price);

        ReqImportOrderDTO req = new ReqImportOrderDTO();
        req.setSupplierId(supplierId);
        req.setItems(List.of(item));
        req.setTax(tax);
        req.setDiscount(discount);
        return req;
    }

    /**
     * Build an ImportOrder stub already saved in the DB
     */
    private ImportOrder makeOrder(int id, ImportOrderStatusEnum status, double total) {
        ImportOrder o = new ImportOrder();
        o.setId(id);
        o.setStatus(status);
        o.setTotalAmount(total);
        o.setAmountPaid(0.0);
        o.setStore(testStore()); // required by processCompletedImportOrder
        return o;
    }

    /**
     * Build an ImportItem linked to an ImportOrder
     */
    private ImportItem makeItem(int id, ImportOrder order, Product product,
            int qty, double price) {
        ImportItem item = new ImportItem();
        item.setId(id);
        item.setImportOrder(order);
        item.setProduct(product);
        item.setQuantity(qty);
        item.setImportPrice(price);
        item.setSubTotal(qty * price);
        return item;
    }

    // ==========================================================================
    // CREATE
    // ==========================================================================
    @Nested
    @DisplayName("create()")
    class CreateTests {

        @Test
        @DisplayName("Success – computes total correctly (subtotal + tax – discount)")
        void create_Success_TotalComputed() {
            // Arrange
            Supplier supplier = makeSupplier(1);
            Product product = makeProduct(1, 100);

            org.mockito.Mockito.lenient().when(currentStoreService.getCurrentStore()).thenReturn(testStore());
            when(supplierRepository.findByIdAndStoreId(1, 1L)).thenReturn(Optional.of(supplier));
            when(productRepository.findByIdAndStoreIdAndIsDeletedFalse(1, 1L)).thenReturn(Optional.of(product));

            ImportOrder savedOrder = makeOrder(10, ImportOrderStatusEnum.PENDING, 0);
            when(importOrderRepository.save(any(ImportOrder.class))).thenReturn(savedOrder);
            when(importItemRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));

            // qty=3, price=50 → subtotal=150  + tax=20 – discount=10 → total=160
            ReqImportOrderDTO req = buildRequest(1, 1, 3, 50.0, 20.0, 10.0);
            req.setNote("test note");

            // Act
            ResImportOrderDTO result = importOrderService.create(req);

            // Assert – result
            assertNotNull(result);
            assertEquals(10, result.getId());

            // Assert – ImportOrder persisted with correct values
            ArgumentCaptor<ImportOrder> orderCaptor = ArgumentCaptor.forClass(ImportOrder.class);
            verify(importOrderRepository).save(orderCaptor.capture());
            ImportOrder captured = orderCaptor.getValue();
            assertEquals(supplier, captured.getSupplier());
            assertEquals(160.0, captured.getTotalAmount(), 1e-9);
            assertEquals(20.0, captured.getTax(), 1e-9);
            assertEquals(10.0, captured.getDiscount(), 1e-9);
            assertEquals(0.0, captured.getAmountPaid(), 1e-9);
            assertEquals(ImportOrderStatusEnum.PENDING, captured.getStatus());
            assertEquals("test note", captured.getNote());
            assertNotNull(captured.getCreatedAt());

            // Assert – items batch-saved
            verify(importItemRepository).saveAll(any());
        }

        @Test
        @DisplayName("Success – null tax / discount treated as 0")
        void create_Success_NullTaxDiscount() {
            // Arrange
            org.mockito.Mockito.lenient().when(currentStoreService.getCurrentStore()).thenReturn(testStore());
            when(supplierRepository.findByIdAndStoreId(1, 1L)).thenReturn(Optional.of(makeSupplier(1)));
            when(productRepository.findByIdAndStoreIdAndIsDeletedFalse(1, 1L)).thenReturn(Optional.of(makeProduct(1, 50)));
            when(importOrderRepository.save(any(ImportOrder.class)))
                    .thenAnswer(inv -> inv.getArgument(0));
            when(importItemRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));

            // qty=2, price=100 → subtotal=200, tax=null→0, discount=null→0 → total=200
            ReqImportOrderDTO req = buildRequest(1, 1, 2, 100.0, null, null);

            // Act
            ResImportOrderDTO result = importOrderService.create(req);

            // Assert
            assertNotNull(result);
            ArgumentCaptor<ImportOrder> captor = ArgumentCaptor.forClass(ImportOrder.class);
            verify(importOrderRepository).save(captor.capture());
            assertEquals(200.0, captor.getValue().getTotalAmount(), 1e-9);
            assertEquals(0.0, captor.getValue().getTax(), 1e-9);
            assertEquals(0.0, captor.getValue().getDiscount(), 1e-9);
        }

        @Test
        @DisplayName("Success – multi-item order, subtotals accumulated correctly")
        void create_Success_MultipleItems() {
            // Arrange
            Supplier supplier = makeSupplier(1);
            Product p1 = makeProduct(1, 10);
            Product p2 = makeProduct(2, 20);

            ReqImportItemDTO i1 = new ReqImportItemDTO();
            i1.setProductId(1);
            i1.setQuantity(2);
            i1.setImportPrice(50.0);  // 100
            ReqImportItemDTO i2 = new ReqImportItemDTO();
            i2.setProductId(2);
            i2.setQuantity(3);
            i2.setImportPrice(30.0);  //  90
            // total = 190, no tax/discount

            ReqImportOrderDTO req = new ReqImportOrderDTO();
            req.setSupplierId(1);
            req.setItems(List.of(i1, i2));

            org.mockito.Mockito.lenient().when(currentStoreService.getCurrentStore()).thenReturn(testStore());
            when(supplierRepository.findByIdAndStoreId(1, 1L)).thenReturn(Optional.of(supplier));
            when(productRepository.findByIdAndStoreIdAndIsDeletedFalse(1, 1L)).thenReturn(Optional.of(p1));
            when(productRepository.findByIdAndStoreIdAndIsDeletedFalse(2, 1L)).thenReturn(Optional.of(p2));
            when(importOrderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(importItemRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));

            // Act
            importOrderService.create(req);

            // Assert – total
            ArgumentCaptor<ImportOrder> captor = ArgumentCaptor.forClass(ImportOrder.class);
            verify(importOrderRepository).save(captor.capture());
            assertEquals(190.0, captor.getValue().getTotalAmount(), 1e-9);

            // Items list saved with correct subtotals
            @SuppressWarnings("unchecked")
            ArgumentCaptor<List<ImportItem>> itemsCaptor
                    = ArgumentCaptor.forClass((Class) List.class);
            verify(importItemRepository).saveAll(itemsCaptor.capture());
            List<ImportItem> savedItems = itemsCaptor.getValue();
            assertEquals(2, savedItems.size());
            assertEquals(100.0, savedItems.get(0).getSubTotal(), 1e-9);
            assertEquals(90.0, savedItems.get(1).getSubTotal(), 1e-9);
        }

        @Test
        @DisplayName("Success – importPrice = 0 is allowed")
        void create_Success_ZeroImportPrice() {
            org.mockito.Mockito.lenient().when(currentStoreService.getCurrentStore()).thenReturn(testStore());
            when(supplierRepository.findByIdAndStoreId(1, 1L)).thenReturn(Optional.of(makeSupplier(1)));
            when(productRepository.findByIdAndStoreIdAndIsDeletedFalse(1, 1L)).thenReturn(Optional.of(makeProduct(1, 5)));
            when(importOrderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(importItemRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));

            ReqImportOrderDTO req = buildRequest(1, 1, 1, 0.0, null, null);

            ResImportOrderDTO result = importOrderService.create(req);

            assertNotNull(result);
            ArgumentCaptor<ImportOrder> captor = ArgumentCaptor.forClass(ImportOrder.class);
            verify(importOrderRepository).save(captor.capture());
            assertEquals(0.0, captor.getValue().getTotalAmount(), 1e-9);
        }

        // -------------------------------------------------------------- failure
        @Test
        @DisplayName("Failure – supplier not found throws IdInvalidException")
        void create_SupplierNotFound() {
            org.mockito.Mockito.lenient().when(currentStoreService.getCurrentStore()).thenReturn(testStore());
            when(supplierRepository.findByIdAndStoreId(999, 1L)).thenReturn(Optional.empty());

            ReqImportOrderDTO req = buildRequest(999, 1, 1, 10.0, null, null);

            IdInvalidException ex = assertThrows(IdInvalidException.class,
                    () -> importOrderService.create(req));
            assertTrue(ex.getMessage().contains("Supplier"));
            assertTrue(ex.getMessage().contains("999"));
            // No order should be persisted
            verify(importOrderRepository, never()).save(any());
        }

        @Test
        @DisplayName("Failure – product not found throws IdInvalidException")
        void create_ProductNotFound() {
            org.mockito.Mockito.lenient().when(currentStoreService.getCurrentStore()).thenReturn(testStore());
            when(supplierRepository.findByIdAndStoreId(1, 1L)).thenReturn(Optional.of(makeSupplier(1)));
            when(productRepository.findByIdAndStoreIdAndIsDeletedFalse(999, 1L)).thenReturn(Optional.empty());

            ReqImportOrderDTO req = buildRequest(1, 999, 1, 10.0, null, null);

            IdInvalidException ex = assertThrows(IdInvalidException.class,
                    () -> importOrderService.create(req));
            assertTrue(ex.getMessage().contains("Product"));
            assertTrue(ex.getMessage().contains("999"));
            verify(importOrderRepository, never()).save(any());
        }

        @Test
        @DisplayName("Failure – negative total (discount > subtotals) throws IdInvalidException")
        void create_NegativeTotal() {
            org.mockito.Mockito.lenient().when(currentStoreService.getCurrentStore()).thenReturn(testStore());
            when(supplierRepository.findByIdAndStoreId(1, 1L)).thenReturn(Optional.of(makeSupplier(1)));
            when(productRepository.findByIdAndStoreIdAndIsDeletedFalse(1, 1L)).thenReturn(Optional.of(makeProduct(1, 50)));

            // qty=1, price=10 → subtotal=10, discount=500 → total=-490
            ReqImportOrderDTO req = buildRequest(1, 1, 1, 10.0, null, 500.0);

            IdInvalidException ex = assertThrows(IdInvalidException.class,
                    () -> importOrderService.create(req));
            assertTrue(ex.getMessage().contains("âm"));
            verify(importOrderRepository, never()).save(any());
        }
    }

    // ==========================================================================
    // FIND BY ID
    // ==========================================================================
    @Nested
    @DisplayName("findById()")
    class FindByIdTests {

        @Test
        @DisplayName("Success – returns order with items")
        void findById_Success() {
            // Arrange
            ImportOrder order = makeOrder(5, ImportOrderStatusEnum.PENDING, 300.0);
            order.setSupplier(makeSupplier(1));
            ImportItem item = makeItem(20, order, makeProduct(1, 10), 3, 100.0);

            when(currentStoreService.getCurrentStoreId()).thenReturn(1L);
            when(importOrderRepository.findByIdAndStoreId(5, 1L)).thenReturn(Optional.of(order));
            when(importItemRepository.findByImportOrder_Id(5)).thenReturn(List.of(item));

            // Act
            ResImportOrderDTO result = importOrderService.findById(5);

            // Assert
            assertNotNull(result);
            assertEquals(5, result.getId());
            assertEquals(1, result.getItems().size());
            assertEquals(20, result.getItems().get(0).getId());
            assertEquals(300.0, result.getTotalAmount(), 1e-9);

            verify(importOrderRepository).findByIdAndStoreId(5, 1L);
            verify(importItemRepository).findByImportOrder_Id(5);
        }

        @Test
        @DisplayName("Failure – import order not found throws IdInvalidException")
        void findById_NotFound() {
            when(currentStoreService.getCurrentStoreId()).thenReturn(1L);
            when(importOrderRepository.findByIdAndStoreId(99, 1L)).thenReturn(Optional.empty());

            IdInvalidException ex = assertThrows(IdInvalidException.class,
                    () -> importOrderService.findById(99));
            assertTrue(ex.getMessage().contains("ImportOrder"));
            assertTrue(ex.getMessage().contains("99"));
        }
    }

    // ==========================================================================
    // FIND ALL
    // ==========================================================================
    @Nested
    @DisplayName("findAll()")
    class FindAllTests {

        @Test
        @DisplayName("Success – returns all orders with their items")
        void findAll_Success() {
            // Arrange
            ImportOrder o1 = makeOrder(1, ImportOrderStatusEnum.PENDING, 100.0);
            ImportOrder o2 = makeOrder(2, ImportOrderStatusEnum.COMPLETED, 200.0);
            o1.setSupplier(makeSupplier(1));
            o2.setSupplier(makeSupplier(2));

            when(currentStoreService.getCurrentStoreId()).thenReturn(1L);
            when(importOrderRepository.findAllByStoreIdOrderByCreatedAtDesc(1L)).thenReturn(List.of(o1, o2));
            when(importItemRepository.findByImportOrder_IdIn(any())).thenReturn(List.of());

            // Act
            List<ResImportOrderDTO> result = importOrderService.findAll();

            // Assert
            assertEquals(2, result.size());
            assertEquals(1, result.get(0).getId());
            assertEquals(2, result.get(1).getId());
            verify(importItemRepository).findByImportOrder_IdIn(any());
        }

        @Test
        @DisplayName("Success – empty repository returns empty list")
        void findAll_Empty() {
            when(currentStoreService.getCurrentStoreId()).thenReturn(1L);
            when(importOrderRepository.findAllByStoreIdOrderByCreatedAtDesc(1L)).thenReturn(List.of());

            List<ResImportOrderDTO> result = importOrderService.findAll();

            assertTrue(result.isEmpty());
            // when orderIds is empty the batch call is still made (with empty list)
            verify(importItemRepository).findByImportOrder_IdIn(any());
        }
    }

    // ==========================================================================
    // UPDATE STATUS  →  COMPLETED
    // ==========================================================================
    @Nested
    @DisplayName("updateStatus() – COMPLETED path")
    class CompleteOrderTests {

        @Test
        @DisplayName("Success – increases product stock for every item")
        void complete_IncreasesStock() {
            // Arrange
            ImportOrder order = makeOrder(1, ImportOrderStatusEnum.PENDING, 500.0);
            Product product = makeProduct(1, 10);   // initial stock = 10
            ImportItem item = makeItem(5, order, product, 4, 100.0); // add 4

            when(currentStoreService.getCurrentStoreId()).thenReturn(1L);
            when(importOrderRepository.findByIdAndStoreIdWithLock(1, 1L)).thenReturn(Optional.of(order));
            when(importOrderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(importItemRepository.findByImportOrder_Id(1)).thenReturn(List.of(item));
            when(productRepository.findByIdAndStoreIdWithLock(eq(1), eq(1L))).thenReturn(Optional.of(product));
            when(productRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(inventoryLogsRepository.save(any())).thenReturn(new InventoryLogs());

            // Act
            importOrderService.updateStatus(1, ImportOrderStatusEnum.COMPLETED);

            // Assert – stock incremented on the in-memory object
            assertEquals(14, product.getStock());    // 10 + 4
            verify(productRepository).save(product);
        }

        @Test
        @DisplayName("Success – creates IMPORT inventory log with correct fields")
        void complete_CreatesInventoryLog() {
            // Arrange
            ImportOrder order = makeOrder(1, ImportOrderStatusEnum.PENDING, 300.0);
            Product product = makeProduct(2, 5);
            ImportItem item = makeItem(7, order, product, 3, 100.0);

            when(currentStoreService.getCurrentStoreId()).thenReturn(1L);
            when(importOrderRepository.findByIdAndStoreIdWithLock(1, 1L)).thenReturn(Optional.of(order));
            when(importOrderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(importItemRepository.findByImportOrder_Id(1)).thenReturn(List.of(item));
            when(productRepository.findByIdAndStoreIdWithLock(eq(2), eq(1L))).thenReturn(Optional.of(product));
            when(productRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(inventoryLogsRepository.save(any())).thenReturn(new InventoryLogs());

            // Act
            importOrderService.updateStatus(1, ImportOrderStatusEnum.COMPLETED);

            // Assert – inventory log saved with correct values
            ArgumentCaptor<InventoryLogs> logCaptor = ArgumentCaptor.forClass(InventoryLogs.class);
            verify(inventoryLogsRepository).save(logCaptor.capture());
            InventoryLogs log = logCaptor.getValue();

            assertEquals(product, log.getProduct());
            assertEquals(item, log.getImportItem());
            assertEquals(3, log.getQuantityIn());
            assertNull(log.getQuantityOut()); // not an outgoing move
            assertEquals(8, log.getBalanceAfter());   // 5 + 3
            assertEquals(8, log.getCurrentStock());
            assertEquals(TypeInventoryEnum.IMPORT, log.getType());
            assertNotNull(log.getCreatedAt());
        }



        @Test
        @DisplayName("Success – multi-item completion: all stocks and logs updated")
        void complete_MultipleItems_AllStocksUpdated() {
            // Arrange
            ImportOrder order = makeOrder(1, ImportOrderStatusEnum.PENDING, 1000.0);
            Product p1 = makeProduct(1, 10);
            Product p2 = makeProduct(2, 20);
            ImportItem item1 = makeItem(1, order, p1, 5, 100.0);
            ImportItem item2 = makeItem(2, order, p2, 3, 200.0);

            when(currentStoreService.getCurrentStoreId()).thenReturn(1L);
            when(importOrderRepository.findByIdAndStoreIdWithLock(1, 1L)).thenReturn(Optional.of(order));
            when(importOrderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(importItemRepository.findByImportOrder_Id(1)).thenReturn(List.of(item1, item2));
            when(productRepository.findByIdAndStoreIdWithLock(eq(1), eq(1L))).thenReturn(Optional.of(p1));
            when(productRepository.findByIdAndStoreIdWithLock(eq(2), eq(1L))).thenReturn(Optional.of(p2));
            when(productRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(inventoryLogsRepository.save(any())).thenReturn(new InventoryLogs());

            // Act
            importOrderService.updateStatus(1, ImportOrderStatusEnum.COMPLETED);

            // Assert stocks
            assertEquals(15, p1.getStock()); // 10 + 5
            assertEquals(23, p2.getStock()); // 20 + 3
            verify(productRepository, times(2)).save(any(Product.class));
            verify(inventoryLogsRepository, times(2)).save(any(InventoryLogs.class));
        }

        @Test
        @DisplayName("Success – status is saved as COMPLETED on the order entity")
        void complete_SetsStatusToCompleted() {
            // Arrange
            ImportOrder order = makeOrder(1, ImportOrderStatusEnum.PENDING, 200.0);
            Product product = makeProduct(1, 5);
            ImportItem item = makeItem(1, order, product, 1, 200.0);

            when(currentStoreService.getCurrentStoreId()).thenReturn(1L);
            when(importOrderRepository.findByIdAndStoreIdWithLock(1, 1L)).thenReturn(Optional.of(order));
            when(importOrderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(importItemRepository.findByImportOrder_Id(1)).thenReturn(List.of(item));
            when(productRepository.findByIdAndStoreIdWithLock(eq(1), eq(1L))).thenReturn(Optional.of(product));
            when(productRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(inventoryLogsRepository.save(any())).thenReturn(new InventoryLogs());

            // Act
            importOrderService.updateStatus(1, ImportOrderStatusEnum.COMPLETED);

            // Assert
            ArgumentCaptor<ImportOrder> captor = ArgumentCaptor.forClass(ImportOrder.class);
            verify(importOrderRepository, atLeastOnce()).save(captor.capture());
            assertEquals(ImportOrderStatusEnum.COMPLETED, captor.getValue().getStatus());
        }

        // -------------------------------------------------------------- failure
        @Test
        @DisplayName("Failure – import order not found throws IdInvalidException")
        void complete_OrderNotFound() {
            when(currentStoreService.getCurrentStoreId()).thenReturn(1L);
            when(importOrderRepository.findByIdAndStoreIdWithLock(99, 1L)).thenReturn(Optional.empty());

            IdInvalidException ex = assertThrows(IdInvalidException.class,
                    () -> importOrderService.updateStatus(99, ImportOrderStatusEnum.COMPLETED));
            assertTrue(ex.getMessage().contains("ImportOrder"));
            assertTrue(ex.getMessage().contains("99"));
        }

        @Test
        @DisplayName("Failure – already COMPLETED order cannot be updated (status guard)")
        void complete_AlreadyCompleted_StatusGuardThrows() {
            ImportOrder order = makeOrder(1, ImportOrderStatusEnum.COMPLETED, 100.0);
            when(currentStoreService.getCurrentStoreId()).thenReturn(1L);
            when(importOrderRepository.findByIdAndStoreIdWithLock(1, 1L)).thenReturn(Optional.of(order));

            IdInvalidException ex = assertThrows(IdInvalidException.class,
                    () -> importOrderService.updateStatus(1, ImportOrderStatusEnum.COMPLETED));
            assertTrue(ex.getMessage().contains("hoàn tất"));

            // No side effects should run
            verify(importItemRepository, never()).findByImportOrder_Id(anyInt());
            verify(productRepository, never()).save(any());
            verify(inventoryLogsRepository, never()).save(any());
        }

        @Test
        @DisplayName("Failure – already CANCELLED order cannot be updated")
        void complete_AlreadyCancelled_Throws() {
            ImportOrder order = makeOrder(1, ImportOrderStatusEnum.CANCELLED, 100.0);
            when(currentStoreService.getCurrentStoreId()).thenReturn(1L);
            when(importOrderRepository.findByIdAndStoreIdWithLock(1, 1L)).thenReturn(Optional.of(order));

            IdInvalidException ex = assertThrows(IdInvalidException.class,
                    () -> importOrderService.updateStatus(1, ImportOrderStatusEnum.COMPLETED));
            assertTrue(ex.getMessage().contains("huỷ"));

            verify(productRepository, never()).save(any());
        }


    }

    // ==========================================================================
    // UPDATE STATUS  →  CANCELLED  (non-COMPLETED path)
    // ==========================================================================
    @Nested
    @DisplayName("updateStatus() – CANCELLED path")
    class CancelOrderTests {

        @Test
        @DisplayName("Success – PENDING → CANCELLED sets status, no side effects")
        void cancel_Success_NoSideEffects() {
            ImportOrder order = makeOrder(1, ImportOrderStatusEnum.PENDING, 200.0);
            when(currentStoreService.getCurrentStoreId()).thenReturn(1L);
            when(importOrderRepository.findByIdAndStoreIdWithLock(1, 1L)).thenReturn(Optional.of(order));
            when(importOrderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            importOrderService.updateStatus(1, ImportOrderStatusEnum.CANCELLED);

            ArgumentCaptor<ImportOrder> captor = ArgumentCaptor.forClass(ImportOrder.class);
            verify(importOrderRepository).save(captor.capture());
            assertEquals(ImportOrderStatusEnum.CANCELLED, captor.getValue().getStatus());

            // Stock, logs, transaction untouched
            verify(productRepository, never()).save(any());
            verify(inventoryLogsRepository, never()).save(any());
        }
    }

    // ==========================================================================
    // DTO BEAN-VALIDATION  (no Spring context – plain javax validator)
    // ==========================================================================
    @Nested
    @DisplayName("Bean-validation on request DTOs")
    class DtoValidationTests {

        @Test
        @DisplayName("ReqImportOrderDTO – null supplierId violates constraint")
        void validate_NullSupplierId() {
            ReqImportOrderDTO req = new ReqImportOrderDTO();
            req.setItems(List.of());   // empty but not null

            Set<ConstraintViolation<ReqImportOrderDTO>> violations = validator.validate(req);
            assertTrue(violations.stream()
                    .anyMatch(v -> v.getPropertyPath().toString().equals("supplierId")));
        }

        @Test
        @DisplayName("ReqImportOrderDTO – null / empty items violates constraint")
        void validate_EmptyItems() {
            ReqImportItemDTO item = new ReqImportItemDTO();
            item.setProductId(1);
            item.setQuantity(1);
            item.setImportPrice(10.0);

            ReqImportOrderDTO req = new ReqImportOrderDTO();
            req.setSupplierId(1);
            // items deliberately left null → @NotEmpty should trigger

            Set<ConstraintViolation<ReqImportOrderDTO>> violations = validator.validate(req);
            assertTrue(violations.stream()
                    .anyMatch(v -> v.getPropertyPath().toString().equals("items")));
        }

        @Test
        @DisplayName("ReqImportOrderDTO – negative tax violates @PositiveOrZero")
        void validate_NegativeTax() {
            ReqImportItemDTO item = new ReqImportItemDTO();
            item.setProductId(1);
            item.setQuantity(1);
            item.setImportPrice(10.0);

            ReqImportOrderDTO req = new ReqImportOrderDTO();
            req.setSupplierId(1);
            req.setItems(List.of(item));
            req.setTax(-5.0);

            Set<ConstraintViolation<ReqImportOrderDTO>> violations = validator.validate(req);
            assertTrue(violations.stream()
                    .anyMatch(v -> v.getPropertyPath().toString().equals("tax")));
        }

        @Test
        @DisplayName("ReqImportOrderDTO – negative discount violates @PositiveOrZero")
        void validate_NegativeDiscount() {
            ReqImportItemDTO item = new ReqImportItemDTO();
            item.setProductId(1);
            item.setQuantity(1);
            item.setImportPrice(10.0);

            ReqImportOrderDTO req = new ReqImportOrderDTO();
            req.setSupplierId(1);
            req.setItems(List.of(item));
            req.setDiscount(-1.0);

            Set<ConstraintViolation<ReqImportOrderDTO>> violations = validator.validate(req);
            assertTrue(violations.stream()
                    .anyMatch(v -> v.getPropertyPath().toString().equals("discount")));
        }

        @Test
        @DisplayName("ReqImportItemDTO – null productId violates @NotNull")
        void validate_NullProductId() {
            ReqImportItemDTO item = new ReqImportItemDTO();
            item.setQuantity(1);
            item.setImportPrice(10.0);

            Set<ConstraintViolation<ReqImportItemDTO>> violations = validator.validate(item);
            assertTrue(violations.stream()
                    .anyMatch(v -> v.getPropertyPath().toString().equals("productId")));
        }

        @Test
        @DisplayName("ReqImportItemDTO – quantity = 0 violates @Min(1)")
        void validate_ZeroQuantity() {
            ReqImportItemDTO item = new ReqImportItemDTO();
            item.setProductId(1);
            item.setQuantity(0);        // must be >= 1
            item.setImportPrice(10.0);

            Set<ConstraintViolation<ReqImportItemDTO>> violations = validator.validate(item);
            assertTrue(violations.stream()
                    .anyMatch(v -> v.getPropertyPath().toString().equals("quantity")));
        }

        @Test
        @DisplayName("ReqImportItemDTO – negative quantity violates @Min(1)")
        void validate_NegativeQuantity() {
            ReqImportItemDTO item = new ReqImportItemDTO();
            item.setProductId(1);
            item.setQuantity(-3);
            item.setImportPrice(10.0);

            Set<ConstraintViolation<ReqImportItemDTO>> violations = validator.validate(item);
            assertTrue(violations.stream()
                    .anyMatch(v -> v.getPropertyPath().toString().equals("quantity")));
        }

        @Test
        @DisplayName("ReqImportItemDTO – negative importPrice violates @PositiveOrZero")
        void validate_NegativeImportPrice() {
            ReqImportItemDTO item = new ReqImportItemDTO();
            item.setProductId(1);
            item.setQuantity(1);
            item.setImportPrice(-0.01); // must be >= 0

            Set<ConstraintViolation<ReqImportItemDTO>> violations = validator.validate(item);
            assertTrue(violations.stream()
                    .anyMatch(v -> v.getPropertyPath().toString().equals("importPrice")));
        }

        @Test
        @DisplayName("ReqImportItemDTO – zero importPrice passes validation")
        void validate_ZeroImportPrice_Passes() {
            ReqImportItemDTO item = new ReqImportItemDTO();
            item.setProductId(1);
            item.setQuantity(1);
            item.setImportPrice(0.0);   // >= 0 → allowed

            Set<ConstraintViolation<ReqImportItemDTO>> violations = validator.validate(item);
            assertTrue(violations.isEmpty());
        }

        @Test
        @DisplayName("ReqImportItemDTO – fully valid item has no violations")
        void validate_ValidItem_NoViolations() {
            ReqImportItemDTO item = new ReqImportItemDTO();
            item.setProductId(1);
            item.setQuantity(5);
            item.setImportPrice(25.0);

            Set<ConstraintViolation<ReqImportItemDTO>> violations = validator.validate(item);
            assertTrue(violations.isEmpty());
        }
    }
}
