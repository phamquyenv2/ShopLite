"""Rewrite InventoryAdjustmentServiceTest to match real service signatures."""

content = '''\
package com.quyen.shoplite.service;

import com.quyen.shoplite.repository.InventoryAdjustmentRepository;
import com.quyen.shoplite.repository.InventoryLogsRepository;
import com.quyen.shoplite.repository.ProductRepository;
import com.quyen.shoplite.util.constant.TypeInventoryEnum;
import com.quyen.shoplite.util.error.BadRequestException;
import com.quyen.shoplite.util.error.ResourceNotFoundException;

import com.quyen.shoplite.domain.*;
import com.quyen.shoplite.domain.request.ReqAdjustmentItemDTO;
import com.quyen.shoplite.domain.request.ReqInventoryAdjustmentDTO;
import com.quyen.shoplite.domain.response.ResInventoryAdjustmentDTO;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InventoryAdjustmentServiceTest {

    @Mock private InventoryAdjustmentRepository adjustmentRepository;
    @Mock private InventoryLogsRepository inventoryLogsRepository;
    @Mock private ProductRepository productRepository;
    @Mock private CurrentStoreService currentStoreService;

    @InjectMocks
    private InventoryAdjustmentService service;

    private Store testStore() {
        Store store = new Store();
        store.setId(1L);
        return store;
    }

    private Validator validator;

    @BeforeEach
    void setUp() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
        lenient().when(currentStoreService.getCurrentStore()).thenReturn(testStore());
        lenient().when(currentStoreService.getCurrentStoreId()).thenReturn(1L);
    }

    private InventoryAdjustment savedAdjustment(int id) {
        InventoryAdjustment a = new InventoryAdjustment();
        a.setId(id);
        a.setReason("Stock count Q1");
        a.setNote("annual audit");
        a.setCreatedBy("admin");
        return a;
    }

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

    private ReqInventoryAdjustmentDTO singleItemRequest(int productId, int actualQty) {
        ReqAdjustmentItemDTO item = new ReqAdjustmentItemDTO();
        item.setProductId(productId);
        item.setActualQuantity(actualQty);

        ReqInventoryAdjustmentDTO req = new ReqInventoryAdjustmentDTO();
        req.setReason("Cycle count");
        req.setNote("shelf audit");
        req.setCreatedBy("warehouse_user");
        req.setItems(List.of(item));
        return req;
    }

    // ==========================================================================
    // CREATE  -  success cases
    // ==========================================================================
    @Nested
    @DisplayName("create() - success cases")
    class CreateSuccessTests {

        @Test
        @DisplayName("Stock INCREASE - actualQty > currentStock, stock updated correctly")
        void create_StockIncrease_Success() {
            Product product = makeProduct(1, 10);
            InventoryAdjustment savedAdj = savedAdjustment(1);

            when(adjustmentRepository.save(any(InventoryAdjustment.class))).thenReturn(savedAdj);
            when(productRepository.findByIdAndStoreIdWithLock(1, 1L)).thenReturn(Optional.of(product));
            when(productRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(inventoryLogsRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            ResInventoryAdjustmentDTO result = service.create(singleItemRequest(1, 15));

            assertNotNull(result);
            assertEquals(1, result.getId());
            assertEquals(15, product.getStock());
            verify(productRepository).save(product);
        }

        @Test
        @DisplayName("Stock DECREASE - actualQty < currentStock, stock updated correctly")
        void create_StockDecrease_Success() {
            Product product = makeProduct(2, 20);
            InventoryAdjustment savedAdj = savedAdjustment(2);

            when(adjustmentRepository.save(any())).thenReturn(savedAdj);
            when(productRepository.findByIdAndStoreIdWithLock(2, 1L)).thenReturn(Optional.of(product));
            when(productRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(inventoryLogsRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.create(singleItemRequest(2, 14));

            assertEquals(14, product.getStock());
            verify(productRepository).save(product);
        }

        @Test
        @DisplayName("Adjustment header saved with correct reason / note / createdBy / timestamp")
        void create_AdjustmentHeader_PersistedCorrectly() {
            Product product = makeProduct(1, 5);
            when(adjustmentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(productRepository.findByIdAndStoreIdWithLock(1, 1L)).thenReturn(Optional.of(product));
            when(productRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(inventoryLogsRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            ReqInventoryAdjustmentDTO req = singleItemRequest(1, 8);
            req.setReason("Year-end count");
            req.setNote("full warehouse check");
            req.setCreatedBy("manager");

            service.create(req);

            ArgumentCaptor<InventoryAdjustment> adjCaptor = ArgumentCaptor.forClass(InventoryAdjustment.class);
            verify(adjustmentRepository).save(adjCaptor.capture());
            InventoryAdjustment captured = adjCaptor.getValue();

            assertEquals("Year-end count", captured.getReason());
            assertEquals("full warehouse check", captured.getNote());
            assertEquals("manager", captured.getCreatedBy());
            assertNotNull(captured.getCreatedAt());
        }

        @Test
        @DisplayName("ADJUST inventory log created with correct quantityIn for increase")
        void create_InventoryLog_Increase_CorrectFields() {
            Product product = makeProduct(1, 10);
            InventoryAdjustment savedAdj = savedAdjustment(5);

            when(adjustmentRepository.save(any())).thenReturn(savedAdj);
            when(productRepository.findByIdAndStoreIdWithLock(1, 1L)).thenReturn(Optional.of(product));
            when(productRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(inventoryLogsRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.create(singleItemRequest(1, 13));

            ArgumentCaptor<InventoryLogs> logCaptor = ArgumentCaptor.forClass(InventoryLogs.class);
            verify(inventoryLogsRepository).save(logCaptor.capture());
            InventoryLogs log = logCaptor.getValue();

            assertEquals(product, log.getProduct());
            assertEquals(savedAdj, log.getAdjustment());
            assertEquals(3, log.getQuantityIn());
            assertNull(log.getQuantityOut());
            assertEquals(13, log.getBalanceAfter());
            assertEquals(13, log.getCurrentStock());
            assertEquals(TypeInventoryEnum.ADJUST, log.getType());
            assertNotNull(log.getCreatedAt());
        }

        @Test
        @DisplayName("ADJUST inventory log created with correct quantityOut for decrease")
        void create_InventoryLog_Decrease_CorrectFields() {
            Product product = makeProduct(1, 15);
            InventoryAdjustment savedAdj = savedAdjustment(6);

            when(adjustmentRepository.save(any())).thenReturn(savedAdj);
            when(productRepository.findByIdAndStoreIdWithLock(1, 1L)).thenReturn(Optional.of(product));
            when(productRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(inventoryLogsRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.create(singleItemRequest(1, 9));

            ArgumentCaptor<InventoryLogs> logCaptor = ArgumentCaptor.forClass(InventoryLogs.class);
            verify(inventoryLogsRepository).save(logCaptor.capture());
            InventoryLogs log = logCaptor.getValue();

            assertNull(log.getQuantityIn());
            assertEquals(6, log.getQuantityOut());
            assertEquals(9, log.getBalanceAfter());
            assertEquals(TypeInventoryEnum.ADJUST, log.getType());
        }

        @Test
        @DisplayName("balanceAfter and currentStock both equal the new stock value")
        void create_BalanceAfterEqualsCurrentStock() {
            Product product = makeProduct(1, 8);
            when(adjustmentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(productRepository.findByIdAndStoreIdWithLock(1, 1L)).thenReturn(Optional.of(product));
            when(productRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(inventoryLogsRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.create(singleItemRequest(1, 11));

            ArgumentCaptor<InventoryLogs> logCaptor = ArgumentCaptor.forClass(InventoryLogs.class);
            verify(inventoryLogsRepository).save(logCaptor.capture());
            InventoryLogs log = logCaptor.getValue();

            assertEquals(log.getBalanceAfter(), log.getCurrentStock());
            assertEquals(11, log.getBalanceAfter());
        }

        @Test
        @DisplayName("Multi-item adjustment - all products updated and all logs persisted")
        void create_MultiItem_AllUpdated() {
            Product p1 = makeProduct(1, 10);
            Product p2 = makeProduct(2, 20);

            ReqAdjustmentItemDTO i1 = new ReqAdjustmentItemDTO();
            i1.setProductId(1); i1.setActualQuantity(12);
            ReqAdjustmentItemDTO i2 = new ReqAdjustmentItemDTO();
            i2.setProductId(2); i2.setActualQuantity(17);

            ReqInventoryAdjustmentDTO req = new ReqInventoryAdjustmentDTO();
            req.setReason("Multi-product count");
            req.setCreatedBy("admin");
            req.setItems(List.of(i1, i2));

            InventoryAdjustment savedAdj = savedAdjustment(7);

            when(adjustmentRepository.save(any())).thenReturn(savedAdj);
            when(productRepository.findByIdAndStoreIdWithLock(1, 1L)).thenReturn(Optional.of(p1));
            when(productRepository.findByIdAndStoreIdWithLock(2, 1L)).thenReturn(Optional.of(p2));
            when(productRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(inventoryLogsRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            ResInventoryAdjustmentDTO result = service.create(req);

            assertEquals(12, p1.getStock());
            assertEquals(17, p2.getStock());
            verify(productRepository, times(2)).save(any(Product.class));
            verify(inventoryLogsRepository, times(2)).save(any(InventoryLogs.class));
            assertEquals(2, result.getLogs().size());
        }

        @Test
        @DisplayName("Adjustment to exactly 0 stock is allowed (full physical depletion)")
        void create_StockGoesToZero_Allowed() {
            Product product = makeProduct(1, 5);
            when(adjustmentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(productRepository.findByIdAndStoreIdWithLock(1, 1L)).thenReturn(Optional.of(product));
            when(productRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(inventoryLogsRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            assertDoesNotThrow(() -> service.create(singleItemRequest(1, 0)));
            assertEquals(0, product.getStock());
        }
    }

    // ==========================================================================
    // CREATE  -  failure cases
    // ==========================================================================
    @Nested
    @DisplayName("create() - failure cases")
    class CreateFailureTests {

        @Test
        @DisplayName("Product not found -> ResourceNotFoundException")
        void create_ProductNotFound_Throws() {
            when(adjustmentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(productRepository.findByIdAndStoreIdWithLock(999, 1L)).thenReturn(Optional.empty());

            ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                    () -> service.create(singleItemRequest(999, 10)));

            assertTrue(ex.getMessage().contains("999"));
            verify(productRepository, never()).save(any());
            verify(inventoryLogsRepository, never()).save(any());
        }

        @Test
        @DisplayName("Zero delta (actual == current) -> BadRequestException")
        void create_ZeroDelta_Throws() {
            Product product = makeProduct(1, 10);
            when(adjustmentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(productRepository.findByIdAndStoreIdWithLock(1, 1L)).thenReturn(Optional.of(product));

            BadRequestException ex = assertThrows(BadRequestException.class,
                    () -> service.create(singleItemRequest(1, 10)));

            assertTrue(ex.getMessage().contains("1"));
            verify(productRepository, never()).save(any());
            verify(inventoryLogsRepository, never()).save(any());
            assertEquals(10, product.getStock());
        }

        @Test
        @DisplayName("Boundary: result=0 is accepted (not negative)")
        void create_BoundaryZeroStock_Allowed() {
            Product product = makeProduct(1, 3);
            when(adjustmentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(productRepository.findByIdAndStoreIdWithLock(1, 1L)).thenReturn(Optional.of(product));
            when(productRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(inventoryLogsRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            assertDoesNotThrow(() -> service.create(singleItemRequest(1, 0)));
            assertEquals(0, product.getStock());
        }

        @Test
        @DisplayName("Second product missing in multi-item request -> no partial updates")
        void create_SecondProductNotFound_NoPartialUpdate() {
            Product p1 = makeProduct(1, 10);

            ReqAdjustmentItemDTO i1 = new ReqAdjustmentItemDTO();
            i1.setProductId(1); i1.setActualQuantity(15);
            ReqAdjustmentItemDTO i2 = new ReqAdjustmentItemDTO();
            i2.setProductId(999); i2.setActualQuantity(5);

            ReqInventoryAdjustmentDTO req = new ReqInventoryAdjustmentDTO();
            req.setReason("Count"); req.setCreatedBy("admin");
            req.setItems(List.of(i1, i2));

            when(adjustmentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(productRepository.findByIdAndStoreIdWithLock(1, 1L)).thenReturn(Optional.of(p1));
            when(productRepository.findByIdAndStoreIdWithLock(999, 1L)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class, () -> service.create(req));
            verify(productRepository, never()).save(any(Product.class));
            verify(inventoryLogsRepository, never()).save(any());
            assertEquals(10, p1.getStock());
        }

        @Test
        @DisplayName("Zero delta in multi-item -> exception, no stock saved at all")
        void create_ZeroDeltaInMultiItem_NoStockTouched() {
            Product p1 = makeProduct(1, 10);
            Product p2 = makeProduct(2, 8);

            ReqAdjustmentItemDTO i1 = new ReqAdjustmentItemDTO();
            i1.setProductId(1); i1.setActualQuantity(15);
            ReqAdjustmentItemDTO i2 = new ReqAdjustmentItemDTO();
            i2.setProductId(2); i2.setActualQuantity(8); // zero delta

            ReqInventoryAdjustmentDTO req = new ReqInventoryAdjustmentDTO();
            req.setReason("Count"); req.setCreatedBy("admin");
            req.setItems(List.of(i1, i2));

            when(adjustmentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(productRepository.findByIdAndStoreIdWithLock(1, 1L)).thenReturn(Optional.of(p1));
            when(productRepository.findByIdAndStoreIdWithLock(2, 1L)).thenReturn(Optional.of(p2));

            BadRequestException ex = assertThrows(BadRequestException.class, () -> service.create(req));
            assertTrue(ex.getMessage().contains("2"));
            verify(productRepository, never()).save(any());
            verify(inventoryLogsRepository, never()).save(any());
            assertEquals(10, p1.getStock());
            assertEquals(8, p2.getStock());
        }
    }

    // ==========================================================================
    // FIND BY ID
    // ==========================================================================
    @Nested
    @DisplayName("findById()")
    class FindByIdTests {

        @Test
        @DisplayName("Success - returns adjustment with its logs")
        void findById_Success() {
            InventoryAdjustment adj = savedAdjustment(1);
            InventoryLogs logEntry = new InventoryLogs();
            logEntry.setId(10);
            logEntry.setType(TypeInventoryEnum.ADJUST);

            when(adjustmentRepository.findByIdAndStoreId(1, 1L)).thenReturn(Optional.of(adj));
            when(inventoryLogsRepository.findByStoreIdAndAdjustment_Id(1L, 1)).thenReturn(List.of(logEntry));

            ResInventoryAdjustmentDTO result = service.findById(1);

            assertNotNull(result);
            assertEquals(1, result.getId());
            assertEquals("Stock count Q1", result.getReason());
            assertEquals(1, result.getLogs().size());
        }

        @Test
        @DisplayName("Failure - adjustment not found -> ResourceNotFoundException")
        void findById_NotFound_Throws() {
            when(adjustmentRepository.findByIdAndStoreId(99, 1L)).thenReturn(Optional.empty());

            ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                    () -> service.findById(99));

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
        @DisplayName("Success - maps all adjustments with their respective logs")
        void findAll_Success() {
            InventoryAdjustment a1 = savedAdjustment(1);
            InventoryAdjustment a2 = savedAdjustment(2);
            a2.setId(2);

            when(adjustmentRepository.findAllByStoreIdOrderByCreatedAtDesc(1L)).thenReturn(List.of(a1, a2));
            when(inventoryLogsRepository.findByStoreIdAndAdjustment_IdIn(eq(1L), any())).thenReturn(List.of());

            var result = service.findAll();

            assertEquals(2, result.size());
        }

        @Test
        @DisplayName("Success - empty repository returns empty list")
        void findAll_Empty() {
            when(adjustmentRepository.findAllByStoreIdOrderByCreatedAtDesc(1L)).thenReturn(List.of());

            var result = service.findAll();

            assertTrue(result.isEmpty());
            verify(inventoryLogsRepository, never()).findByStoreIdAndAdjustment_IdIn(anyLong(), any());
        }
    }

    // ==========================================================================
    // DTO BEAN-VALIDATION
    // ==========================================================================
    @Nested
    @DisplayName("Bean-validation on request DTOs")
    class DtoValidationTests {

        @Test
        @DisplayName("ReqInventoryAdjustmentDTO - blank reason fails @NotBlank")
        void validate_BlankReason() {
            ReqAdjustmentItemDTO item = new ReqAdjustmentItemDTO();
            item.setProductId(1); item.setActualQuantity(5);
            ReqInventoryAdjustmentDTO req = new ReqInventoryAdjustmentDTO();
            req.setReason("   "); req.setCreatedBy("user"); req.setItems(List.of(item));
            Set<ConstraintViolation<ReqInventoryAdjustmentDTO>> v = validator.validate(req);
            assertTrue(v.stream().anyMatch(c -> c.getPropertyPath().toString().equals("reason")));
        }

        @Test
        @DisplayName("ReqInventoryAdjustmentDTO - fully valid request has no violations")
        void validate_FullyValid_NoViolations() {
            ReqAdjustmentItemDTO item = new ReqAdjustmentItemDTO();
            item.setProductId(1); item.setActualQuantity(10);
            ReqInventoryAdjustmentDTO req = new ReqInventoryAdjustmentDTO();
            req.setReason("Q1 Count"); req.setNote("optional note");
            req.setCreatedBy("auditor"); req.setItems(List.of(item));
            Set<ConstraintViolation<ReqInventoryAdjustmentDTO>> v = validator.validate(req);
            assertTrue(v.isEmpty());
        }
    }
}
'''

path = "backend/shoplite/src/test/java/com/quyen/shoplite/service/InventoryAdjustmentServiceTest.java"
with open(path, 'w', encoding='utf-8', newline='\r\n') as f:
    f.write(content)
print("InventoryAdjustmentServiceTest rewritten!")
