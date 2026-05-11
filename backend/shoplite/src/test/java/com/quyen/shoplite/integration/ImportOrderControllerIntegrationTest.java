package com.quyen.shoplite.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.quyen.shoplite.repository.*;
import com.quyen.shoplite.util.constant.ImportOrderStatusEnum;
import com.quyen.shoplite.util.constant.TypeInventoryEnum;
import com.quyen.shoplite.util.constant.TypeTransactionEnum;

import com.quyen.shoplite.domain.*;
import com.quyen.shoplite.domain.request.ReqImportItemDTO;
import com.quyen.shoplite.domain.request.ReqImportOrderDTO;
import com.quyen.shoplite.domain.request.ReqUpdateImportOrderStatusDTO;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class ImportOrderControllerIntegrationTest extends IntegrationTestBase {

    // ----------------------------------------------------------------- injects
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private SupplierRepository supplierRepository;
    @Autowired
    private CategoryRepository categoryRepository;
    @Autowired
    private UnitRepository unitRepository;
    @Autowired
    private ProductRepository productRepository;
    @Autowired
    private ImportOrderRepository importOrderRepository;
    @Autowired
    private ImportItemRepository importItemRepository;
    @Autowired
    private InventoryLogsRepository inventoryLogsRepository;
    @Autowired
    private TransactionRepository transactionRepository;

    // ---------------------------------------------------------- shared test IDs
    private Integer supplierId;
    private Integer productId;       // initial stock = 10
    private Integer product2Id;      // initial stock = 5

    @BeforeEach
    void setup() {
        Supplier supplier = supplierRepository.save(Supplier.builder().store(testStore)
                .name("Test Supplier " + System.nanoTime())
                .phone("0901234567")
                .build());
        supplierId = supplier.getId();

        Category cat = categoryRepository.save(
                Category.builder().store(testStore).name("ImportCat_" + System.nanoTime()).build());
        Unit unit = unitRepository.save(
                Unit.builder().store(testStore).name("ImportUnit_" + System.nanoTime()).description("u").build());

        Product p1 = productRepository.save(Product.builder().store(testStore)
                .category(cat).unit(unit)
                .name("Import Product A")
                .sku("IMP-SKU-A-" + System.nanoTime())
                .stock(10)
                .sellingPrice(50.0)
                .costPrice(0.0)
                .isDeleted(false)
                .build());
        productId = p1.getId();

        Product p2 = productRepository.save(Product.builder().store(testStore)
                .category(cat).unit(unit)
                .name("Import Product B")
                .sku("IMP-SKU-B-" + System.nanoTime())
                .stock(5)
                .sellingPrice(30.0)
                .costPrice(0.0)
                .isDeleted(false)
                .build());
        product2Id = p2.getId();
    }

    // ----------------------------------------------------------------- helpers
    private ReqImportOrderDTO validRequest() {
        ReqImportItemDTO item = new ReqImportItemDTO();
        item.setProductId(productId);
        item.setQuantity(3);
        item.setImportPrice(20.0);

        ReqImportOrderDTO req = new ReqImportOrderDTO();
        req.setSupplierId(supplierId);
        req.setItems(List.of(item));
        req.setTax(5.0);
        req.setDiscount(2.0);
        req.setNote("integration test order");
        return req;
    }

    /**
     * POST /import-orders and return the created order id from response
     */
    private Integer createImportOrder(ReqImportOrderDTO req) throws Exception {
        MvcResult result = mockMvc.perform(withStore(post("/api/v1/import-orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req))))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString())
                .path("data").path("id").asInt();
    }

    /**
     * PATCH /import-orders/{id}/status to COMPLETED
     */
    private void completeImportOrder(Integer id) throws Exception {
        ReqUpdateImportOrderStatusDTO statusReq = new ReqUpdateImportOrderStatusDTO();
        statusReq.setStatus(ImportOrderStatusEnum.COMPLETED);
        mockMvc.perform(withStore(put("/api/v1/import-orders/" + id + "/status")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(statusReq))))
                .andExpect(status().isOk());
    }

    // ==========================================================================
    // POST /import-orders
    // ==========================================================================
    @Nested
    @DisplayName("POST /api/v1/import-orders")
    class CreateImportOrderTests {

        @Test
        @DisplayName("Success – 201 with correct response body and computed total")
        void createImportOrder_Success() throws Exception {
            // Arrange:  qty=3, price=20 → subtotal=60, tax=5, discount=2 → total=63
            ReqImportOrderDTO req = validRequest();

            mockMvc.perform(withStore(post("/api/v1/import-orders")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(req))))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.statusCode").value(201))
                    .andExpect(jsonPath("$.data.id").isNumber())
                    .andExpect(jsonPath("$.data.supplierId").value(supplierId))
                    .andExpect(jsonPath("$.data.status").value("PENDING"))
                    .andExpect(jsonPath("$.data.totalAmount").value(63.0))
                    .andExpect(jsonPath("$.data.tax").value(5.0))
                    .andExpect(jsonPath("$.data.discount").value(2.0))
                    .andExpect(jsonPath("$.data.amountPaid").value(0.0))
                    .andExpect(jsonPath("$.data.note").value("integration test order"))
                    .andExpect(jsonPath("$.data.createdAt").isNotEmpty())
                    .andExpect(jsonPath("$.data.items").isArray())
                    .andExpect(jsonPath("$.data.items[0].productId").value(productId))
                    .andExpect(jsonPath("$.data.items[0].quantity").value(3))
                    .andExpect(jsonPath("$.data.items[0].importPrice").value(20.0))
                    .andExpect(jsonPath("$.data.items[0].subTotal").value(60.0));
        }

        @Test
        @DisplayName("Success – null tax/discount default to 0, total = subtotal")
        void createImportOrder_NullTaxDiscount_TotalEqualsSubtotal() throws Exception {
            ReqImportItemDTO item = new ReqImportItemDTO();
            item.setProductId(productId);
            item.setQuantity(2);
            item.setImportPrice(50.0);  // subtotal = 100

            ReqImportOrderDTO req = new ReqImportOrderDTO();
            req.setSupplierId(supplierId);
            req.setItems(List.of(item));
            // tax and discount intentionally omitted → null

            mockMvc.perform(withStore(post("/api/v1/import-orders")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(req))))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.data.totalAmount").value(100.0))
                    .andExpect(jsonPath("$.data.tax").value(0.0))
                    .andExpect(jsonPath("$.data.discount").value(0.0));
        }

        @Test
        @DisplayName("Success – multi-item order, each item's subTotal correct")
        void createImportOrder_MultiItem_Success() throws Exception {
            ReqImportItemDTO i1 = new ReqImportItemDTO();
            i1.setProductId(productId);
            i1.setQuantity(4);
            i1.setImportPrice(10.0); // 40
            ReqImportItemDTO i2 = new ReqImportItemDTO();
            i2.setProductId(product2Id);
            i2.setQuantity(2);
            i2.setImportPrice(15.0); // 30
            // total = 70

            ReqImportOrderDTO req = new ReqImportOrderDTO();
            req.setSupplierId(supplierId);
            req.setItems(List.of(i1, i2));

            mockMvc.perform(withStore(post("/api/v1/import-orders")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(req))))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.data.totalAmount").value(70.0))
                    .andExpect(jsonPath("$.data.items.length()").value(2));
        }

        // ---- failures ----
        @Test
        @DisplayName("Failure – missing supplierId → 400 with field error")
        void createImportOrder_NullSupplierId_Failure() throws Exception {
            ReqImportOrderDTO req = validRequest();
            req.setSupplierId(null);

            mockMvc.perform(withStore(post("/api/v1/import-orders")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(req))))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.statusCode").value(400))
                    .andExpect(jsonPath("$.message").value(containsString("supplierId")))
                    .andExpect(jsonPath("$.errors").isArray());
        }

        @Test
        @DisplayName("Failure – supplier not found → 400 with business message")
        void createImportOrder_SupplierNotFound_Failure() throws Exception {
            ReqImportOrderDTO req = validRequest();
            req.setSupplierId(99999);

            mockMvc.perform(withStore(post("/api/v1/import-orders")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(req))))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.statusCode").value(400))
                    .andExpect(jsonPath("$.message").value(containsString("Supplier")))
                    .andExpect(jsonPath("$.message").value(containsString("99999")));
        }

        @Test
        @DisplayName("Failure – empty items list → 400 with field error")
        void createImportOrder_EmptyItems_Failure() throws Exception {
            ReqImportOrderDTO req = validRequest();
            req.setItems(List.of());

            mockMvc.perform(withStore(post("/api/v1/import-orders")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(req))))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.statusCode").value(400))
                    .andExpect(jsonPath("$.message").value(containsString("items")))
                    .andExpect(jsonPath("$.errors").isArray());
        }

        @Test
        @DisplayName("Failure – product not found → 400 with business message")
        void createImportOrder_ProductNotFound_Failure() throws Exception {
            ReqImportItemDTO item = new ReqImportItemDTO();
            item.setProductId(99999);
            item.setQuantity(1);
            item.setImportPrice(10.0);

            ReqImportOrderDTO req = new ReqImportOrderDTO();
            req.setSupplierId(supplierId);
            req.setItems(List.of(item));

            mockMvc.perform(withStore(post("/api/v1/import-orders")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(req))))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.statusCode").value(400))
                    .andExpect(jsonPath("$.message").value(containsString("Product")))
                    .andExpect(jsonPath("$.message").value(containsString("99999")));
        }

        @Test
        @DisplayName("Failure – quantity = 0 → 400 with validation errors array")
        void createImportOrder_InvalidQuantity_Zero_Failure() throws Exception {
            ReqImportItemDTO item = new ReqImportItemDTO();
            item.setProductId(productId);
            item.setQuantity(0);        // must be >= 1
            item.setImportPrice(10.0);

            ReqImportOrderDTO req = new ReqImportOrderDTO();
            req.setSupplierId(supplierId);
            req.setItems(List.of(item));

            mockMvc.perform(withStore(post("/api/v1/import-orders")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(req))))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.statusCode").value(400))
                    .andExpect(jsonPath("$.errors").isArray())
                    .andExpect(jsonPath("$.errors[*].field",
                            hasItem(containsString("quantity"))));
        }

        @Test
        @DisplayName("Failure – quantity negative → 400 with validation errors array")
        void createImportOrder_NegativeQuantity_Failure() throws Exception {
            ReqImportItemDTO item = new ReqImportItemDTO();
            item.setProductId(productId);
            item.setQuantity(-5);
            item.setImportPrice(10.0);

            ReqImportOrderDTO req = new ReqImportOrderDTO();
            req.setSupplierId(supplierId);
            req.setItems(List.of(item));

            mockMvc.perform(withStore(post("/api/v1/import-orders")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(req))))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.statusCode").value(400))
                    .andExpect(jsonPath("$.errors").isArray());
        }

        @Test
        @DisplayName("Failure – importPrice negative → 400 with validation errors array")
        void createImportOrder_NegativeImportPrice_Failure() throws Exception {
            ReqImportItemDTO item = new ReqImportItemDTO();
            item.setProductId(productId);
            item.setQuantity(1);
            item.setImportPrice(-1.0);  // must be >= 0

            ReqImportOrderDTO req = new ReqImportOrderDTO();
            req.setSupplierId(supplierId);
            req.setItems(List.of(item));

            mockMvc.perform(withStore(post("/api/v1/import-orders")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(req))))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.statusCode").value(400))
                    .andExpect(jsonPath("$.errors").isArray())
                    .andExpect(jsonPath("$.errors[*].field",
                            hasItem(containsString("importPrice"))));
        }

        @Test
        @DisplayName("Failure – negative tax → 400 with validation errors array")
        void createImportOrder_NegativeTax_Failure() throws Exception {
            ReqImportOrderDTO req = validRequest();
            req.setTax(-10.0);

            mockMvc.perform(withStore(post("/api/v1/import-orders")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(req))))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.statusCode").value(400))
                    .andExpect(jsonPath("$.errors").isArray())
                    .andExpect(jsonPath("$.errors[*].field", hasItem("tax")));
        }

        @Test
        @DisplayName("Failure – negative discount → 400 with validation errors array")
        void createImportOrder_NegativeDiscount_Failure() throws Exception {
            ReqImportOrderDTO req = validRequest();
            req.setDiscount(-5.0);

            mockMvc.perform(withStore(post("/api/v1/import-orders")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(req))))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.statusCode").value(400))
                    .andExpect(jsonPath("$.errors").isArray())
                    .andExpect(jsonPath("$.errors[*].field", hasItem("discount")));
        }

        @Test
        @DisplayName("Failure – discount > subtotal → negative total → 400")
        void createImportOrder_NegativeTotal_Failure() throws Exception {
            ReqImportItemDTO item = new ReqImportItemDTO();
            item.setProductId(productId);
            item.setQuantity(1);
            item.setImportPrice(10.0); // subtotal = 10

            ReqImportOrderDTO req = new ReqImportOrderDTO();
            req.setSupplierId(supplierId);
            req.setItems(List.of(item));
            req.setDiscount(500.0);    // makes total negative

            mockMvc.perform(withStore(post("/api/v1/import-orders")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(req))))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.statusCode").value(400))
                    .andExpect(jsonPath("$.message").value(containsString("âm")));
        }
    }

    // ==========================================================================
    // GET /import-orders/{id}
    // ==========================================================================
    @Nested
    @DisplayName("GET /api/v1/import-orders/{id}")
    class GetByIdTests {

        @Test
        @DisplayName("Success – 200 with full order and items")
        void getImportOrderById_Success() throws Exception {
            Integer id = createImportOrder(validRequest());

            mockMvc.perform(withStore(get("/api/v1/import-orders/" + id)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.statusCode").value(200))
                    .andExpect(jsonPath("$.data.id").value(id))
                    .andExpect(jsonPath("$.data.supplierId").value(supplierId))
                    .andExpect(jsonPath("$.data.status").value("PENDING"))
                    .andExpect(jsonPath("$.data.totalAmount").value(63.0))
                    .andExpect(jsonPath("$.data.items").isArray())
                    .andExpect(jsonPath("$.data.items.length()").value(1));
        }

        @Test
        @DisplayName("Failure – non-existent id → 400 with message about ImportOrder")
        void getImportOrderById_NotFound_Failure() throws Exception {
            mockMvc.perform(withStore(get("/api/v1/import-orders/99999")))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.statusCode").value(400))
                    .andExpect(jsonPath("$.message").value(containsString("ImportOrder")))
                    .andExpect(jsonPath("$.message").value(containsString("99999")));
        }
    }

    // ==========================================================================
    // GET /import-orders
    // ==========================================================================
    @Nested
    @DisplayName("GET /api/v1/import-orders")
    class GetAllTests {

        @Test
        @DisplayName("Success – returns array with at least the created entries")
        void listImportOrders_Success() throws Exception {
            createImportOrder(validRequest());
            createImportOrder(validRequest());

            mockMvc.perform(withStore(get("/api/v1/import-orders")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.statusCode").value(200))
                    .andExpect(jsonPath("$.data").isArray())
                    .andExpect(jsonPath("$.data.length()",
                            greaterThanOrEqualTo(2)));
        }

        @Test
        @DisplayName("Success – empty DB returns empty array")
        void listImportOrders_Empty() throws Exception {
            // @Transactional rolls back; nothing persisted from other tests
            mockMvc.perform(withStore(get("/api/v1/import-orders")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data").isArray());
        }
    }

    // ==========================================================================
    // PUT /import-orders/{id}/status  →  COMPLETED
    // ==========================================================================
    @Nested
    @DisplayName("PUT /api/v1/import-orders/{id}/status → COMPLETED")
    class CompleteImportOrderTests {

        @Test
        @DisplayName("Success – 200 with status=COMPLETED in response body")
        void completeImportOrder_Success_ResponseBody() throws Exception {
            Integer id = createImportOrder(validRequest());

            ReqUpdateImportOrderStatusDTO statusReq = new ReqUpdateImportOrderStatusDTO();
            statusReq.setStatus(ImportOrderStatusEnum.COMPLETED);

            mockMvc.perform(withStore(put("/api/v1/import-orders/" + id + "/status")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(statusReq))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.statusCode").value(200))
                    .andExpect(jsonPath("$.data.id").value(id))
                    .andExpect(jsonPath("$.data.status").value("COMPLETED"));
        }

        @Test
        @DisplayName("Persistence – product stock increases by imported quantity")
        void completeImportOrder_IncreasesProductStock() throws Exception {
            // qty=3 imported, initial stock=10 → expect 13
            Integer id = createImportOrder(validRequest());
            completeImportOrder(id);

            Product updated = productRepository.findById(productId).orElseThrow();
            assertThat(updated.getStock()).isEqualTo(13); // 10 + 3
        }

        @Test
        @DisplayName("Persistence – IMPORT inventory log created with correct fields")
        void completeImportOrder_CreatesInventoryLog() throws Exception {
            Integer id = createImportOrder(validRequest());
            completeImportOrder(id);

            List<InventoryLogs> logs = inventoryLogsRepository.findAllByProduct_Id(productId);
            assertThat(logs).hasSize(1);

            InventoryLogs log = logs.get(0);
            assertThat(log.getType()).isEqualTo(TypeInventoryEnum.IMPORT);
            assertThat(log.getQuantityIn()).isEqualTo(3);
            assertThat(log.getQuantityOut()).isNull();
            assertThat(log.getBalanceAfter()).isEqualTo(13);  // 10 + 3
            assertThat(log.getCurrentStock()).isEqualTo(13);
            assertThat(log.getProduct().getId()).isEqualTo(productId);
            assertThat(log.getCreatedAt()).isNotNull();
        }



        @Test
        @DisplayName("Persistence – multi-item completion updates all product stocks and creats all logs")
        void completeImportOrder_MultiItem_AllStocksAndLogsUpdated() throws Exception {
            // item1: productId qty=3  (stock 10 → 13)
            // item2: product2Id qty=2 (stock 5 → 7)
            ReqImportItemDTO i1 = new ReqImportItemDTO();
            i1.setProductId(productId);
            i1.setQuantity(3);
            i1.setImportPrice(10.0);
            ReqImportItemDTO i2 = new ReqImportItemDTO();
            i2.setProductId(product2Id);
            i2.setQuantity(2);
            i2.setImportPrice(15.0);

            ReqImportOrderDTO req = new ReqImportOrderDTO();
            req.setSupplierId(supplierId);
            req.setItems(List.of(i1, i2));

            Integer id = createImportOrder(req);
            completeImportOrder(id);

            Product p1 = productRepository.findById(productId).orElseThrow();
            Product p2 = productRepository.findById(product2Id).orElseThrow();
            assertThat(p1.getStock()).isEqualTo(13);
            assertThat(p2.getStock()).isEqualTo(7);

            assertThat(inventoryLogsRepository.findAllByProduct_Id(productId)).hasSize(1);
            assertThat(inventoryLogsRepository.findAllByProduct_Id(product2Id)).hasSize(1);
        }

        // ---- failures ----
        @Test
        @DisplayName("Failure – duplicate completion → 400, side effects not doubled")
        void completeImportOrder_DuplicateCompletion_Failure() throws Exception {
            Integer id = createImportOrder(validRequest());

            // First completion – must succeed
            completeImportOrder(id);

            // Second attempt – must fail because status is now COMPLETED
            ReqUpdateImportOrderStatusDTO statusReq = new ReqUpdateImportOrderStatusDTO();
            statusReq.setStatus(ImportOrderStatusEnum.COMPLETED);

            mockMvc.perform(withStore(put("/api/v1/import-orders/" + id + "/status")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(statusReq))))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.statusCode").value(400))
                    .andExpect(jsonPath("$.message").value(containsString("hoàn tất")));

            // Stock must not be incremented a second time
            Product p = productRepository.findById(productId).orElseThrow();
            assertThat(p.getStock()).isEqualTo(13); // still 10 + 3, not 10 + 3 + 3

            // Still only 1 inventory log
            assertThat(inventoryLogsRepository.findAllByProduct_Id(productId)).hasSize(1);
        }

        @Test
        @DisplayName("Failure – import order not found → 400 with message")
        void completeImportOrder_OrderNotFound_Failure() throws Exception {
            ReqUpdateImportOrderStatusDTO statusReq = new ReqUpdateImportOrderStatusDTO();
            statusReq.setStatus(ImportOrderStatusEnum.COMPLETED);

            mockMvc.perform(withStore(put("/api/v1/import-orders/99999/status")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(statusReq))))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.statusCode").value(400))
                    .andExpect(jsonPath("$.message").value(containsString("ImportOrder")))
                    .andExpect(jsonPath("$.message").value(containsString("99999")));
        }

        @Test
        @DisplayName("Failure – null status body → 400 with validation error")
        void completeImportOrder_NullStatus_Failure() throws Exception {
            Integer id = createImportOrder(validRequest());

            mockMvc.perform(withStore(put("/api/v1/import-orders/" + id + "/status")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"status\": null}")))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.statusCode").value(400));
        }
    }

    // ==========================================================================
    // PUT /import-orders/{id}/status  →  CANCELLED
    // ==========================================================================
    @Nested
    @DisplayName("PUT /api/v1/import-orders/{id}/status → CANCELLED")
    class CancelImportOrderTests {

        @Test
        @DisplayName("Success – PENDING → CANCELLED, no stock change")
        void cancelImportOrder_Success_NoStockChange() throws Exception {
            Integer id = createImportOrder(validRequest());

            ReqUpdateImportOrderStatusDTO statusReq = new ReqUpdateImportOrderStatusDTO();
            statusReq.setStatus(ImportOrderStatusEnum.CANCELLED);

            mockMvc.perform(withStore(put("/api/v1/import-orders/" + id + "/status")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(statusReq))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.status").value("CANCELLED"));

            // Stock must remain unchanged
            Product p = productRepository.findById(productId).orElseThrow();
            assertThat(p.getStock()).isEqualTo(10);

            // No inventory logs created
            assertThat(inventoryLogsRepository.findAllByProduct_Id(productId)).isEmpty();
        }

        @Test
        @DisplayName("Failure – already CANCELLED order cannot change status → 400")
        void cancelImportOrder_AlreadyCancelled_Failure() throws Exception {
            Integer id = createImportOrder(validRequest());

            ReqUpdateImportOrderStatusDTO cancelReq = new ReqUpdateImportOrderStatusDTO();
            cancelReq.setStatus(ImportOrderStatusEnum.CANCELLED);

            // First cancel
            mockMvc.perform(withStore(put("/api/v1/import-orders/" + id + "/status")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(cancelReq))))
                    .andExpect(status().isOk());

            // Second cancel
            mockMvc.perform(withStore(put("/api/v1/import-orders/" + id + "/status")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(cancelReq))))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.statusCode").value(400))
                    .andExpect(jsonPath("$.message").value(containsString("huỷ")));
        }
    }
}
