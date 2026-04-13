package com.quyen.shoplite.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.quyen.shoplite.domain.*;
import com.quyen.shoplite.domain.request.ReqOrderDTO;
import com.quyen.shoplite.domain.request.ReqOrderItemDTO;
import com.quyen.shoplite.domain.request.ReqPaymentDTO;
import com.quyen.shoplite.repository.*;
import com.quyen.shoplite.util.constant.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@WithMockUser
class OrderControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private UnitRepository unitRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private InventoryLogsRepository inventoryLogsRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    // Shared test data IDs
    private Integer userId;
    private Integer customerId;
    private Integer productId;
    private Integer lowStockProductId;

    @BeforeEach
    void setup() {
        User user = userRepository.save(User.builder()
                .username("ituser_" + System.nanoTime())
                .password("pass")
                .isActive(true)
                .createdAt(LocalDateTime.now())
                .build());
        userId = user.getId();

        Customer customer = customerRepository.save(Customer.builder()
                .name("IT Customer")
                .phone("090" + System.nanoTime() % 10000000)
                .build());
        customerId = customer.getId();

        Category cat = categoryRepository.save(Category.builder().name("ITCat_" + System.nanoTime()).build());
        Unit unit = unitRepository.save(Unit.builder().name("ITUnit_" + System.nanoTime()).description("u").build());

        Product product = productRepository.save(Product.builder()
                .category(cat)
                .unit(unit)
                .name("IT Product")
                .sku("ITSKU_" + System.nanoTime())
                .stock(20)
                .price(50.0)
                .isDeleted(false)
                .createdAt(LocalDateTime.now())
                .build());
        productId = product.getId();

        Product lowStockProduct = productRepository.save(Product.builder()
                .category(cat)
                .unit(unit)
                .name("Low Stock Product")
                .sku("SKLW_" + System.nanoTime())
                .stock(1)
                .price(100.0)
                .isDeleted(false)
                .createdAt(LocalDateTime.now())
                .build());
        lowStockProductId = lowStockProduct.getId();
    }

    // ========== Helper: build valid CreateOrderRequest ===========

    private ReqOrderDTO validOrderRequest() {
        ReqOrderItemDTO item = new ReqOrderItemDTO();
        item.setProductId(productId);
        item.setQuantity(2L);
        item.setPrice(50.0);

        ReqOrderDTO req = new ReqOrderDTO();
        req.setUserId(userId);
        req.setCustomerId(customerId);
        req.setDiscount(0.0);
        req.setItems(List.of(item));
        return req;
    }

    // ======================= ORDER: SUCCESS CASES ========================

    @Test
    @DisplayName("POST /orders - create order success")
    void createOrder_Success() throws Exception {
        mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validOrderRequest())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andExpect(jsonPath("$.data.totalAmount").value(100.0))
                .andExpect(jsonPath("$.data.discount").value(0.0))
                .andExpect(jsonPath("$.data.customerId").value(customerId))
                .andExpect(jsonPath("$.data.items").isArray())
                .andExpect(jsonPath("$.data.items[0].productId").value(productId))
                .andExpect(jsonPath("$.data.items[0].quantity").value(2))
                .andExpect(jsonPath("$.data.items[0].price").value(50.0))
                .andExpect(jsonPath("$.data.items[0].totalPrice").value(100.0));
    }

    @Test
    @DisplayName("POST /orders - product stock decreases after successful sale")
    void createOrder_DecreasesProductStock() throws Exception {
        mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validOrderRequest())))
                .andExpect(status().isCreated());

        Product updated = productRepository.findById(productId).orElseThrow();
        assertThat(updated.getStock()).isEqualTo(18); // 20 - 2
    }

    @Test
    @DisplayName("POST /orders - inventory log SALE created after successful sale")
    void createOrder_CreatesInventoryLog() throws Exception {
        mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validOrderRequest())))
                .andExpect(status().isCreated());

        List<InventoryLogs> logs = inventoryLogsRepository.findAllByProduct_Id(productId);
        assertThat(logs).hasSize(1);
        assertThat(logs.get(0).getType()).isEqualTo(TypeInventoryEnum.SALE);
        assertThat(logs.get(0).getQuantityOut()).isEqualTo(2);
        assertThat(logs.get(0).getBalanceAfter()).isEqualTo(18);
        assertThat(logs.get(0).getCurrentStock()).isEqualTo(18);
    }

    @Test
    @DisplayName("GET /orders/{id} - get order by id success")
    void getOrderById_Success() throws Exception {
        // create one order first
        MvcResult result = mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validOrderRequest())))
                .andExpect(status().isCreated())
                .andReturn();

        Integer orderId = objectMapper.readTree(result.getResponse().getContentAsString())
                .path("data").path("id").asInt();

        mockMvc.perform(get("/api/v1/orders/" + orderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(orderId))
                .andExpect(jsonPath("$.data.customerId").value(customerId))
                .andExpect(jsonPath("$.data.items").isArray());
    }

    @Test
    @DisplayName("GET /orders - list orders success")
    void listOrders_Success() throws Exception {
        mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validOrderRequest())))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/orders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(org.hamcrest.Matchers.greaterThanOrEqualTo(1)));
    }

    @Test
    @DisplayName("DELETE /orders/{id} - cancel order success, stock restored, RETURN log created")
    void cancelOrder_Success() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validOrderRequest())))
                .andExpect(status().isCreated())
                .andReturn();

        Integer orderId = objectMapper.readTree(result.getResponse().getContentAsString())
                .path("data").path("id").asInt();

        // cancel
        mockMvc.perform(delete("/api/v1/orders/" + orderId))
                .andExpect(status().isNoContent());

        // Order status = CANCELLED
        Order cancelled = orderRepository.findById(orderId).orElseThrow();
        assertThat(cancelled.getStatus()).isEqualTo(StatusEnum.CANCELLED);

        // Stock restored
        Product restored = productRepository.findById(productId).orElseThrow();
        assertThat(restored.getStock()).isEqualTo(20); // restored to original

        // RETURN log created
        List<InventoryLogs> logs = inventoryLogsRepository.findAllByProduct_Id(productId);
        assertThat(logs).anyMatch(l -> l.getType() == TypeInventoryEnum.RETURN);
    }

    // ======================= ORDER: FAILURE CASES ========================

    @Test
    @DisplayName("POST /orders - missing customerId failure")
    void createOrder_MissingCustomer_Failure() throws Exception {
        ReqOrderDTO req = validOrderRequest();
        req.setCustomerId(null); // missing

        mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.statusCode").value(400))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    @DisplayName("POST /orders - customer not found failure")
    void createOrder_CustomerNotFound_Failure() throws Exception {
        ReqOrderDTO req = validOrderRequest();
        req.setCustomerId(99999); // non-existent

        mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.statusCode").value(400))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Customer")));
    }

    @Test
    @DisplayName("POST /orders - empty items failure")
    void createOrder_EmptyItems_Failure() throws Exception {
        ReqOrderDTO req = validOrderRequest();
        req.setItems(List.of()); // empty

        mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.statusCode").value(400))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("items")));
    }

    @Test
    @DisplayName("POST /orders - invalid item quantity (0) failure")
    void createOrder_InvalidItemQuantity_Failure() throws Exception {
        ReqOrderItemDTO item = new ReqOrderItemDTO();
        item.setProductId(productId);
        item.setQuantity(0L); // invalid
        item.setPrice(50.0);

        ReqOrderDTO req = validOrderRequest();
        req.setItems(List.of(item));

        mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.statusCode").value(400))
                .andExpect(jsonPath("$.errors").isArray());
    }

    @Test
    @DisplayName("POST /orders - product not found failure")
    void createOrder_ProductNotFound_Failure() throws Exception {
        ReqOrderItemDTO item = new ReqOrderItemDTO();
        item.setProductId(99999); // non-existent
        item.setQuantity(1L);
        item.setPrice(50.0);

        ReqOrderDTO req = validOrderRequest();
        req.setItems(List.of(item));

        mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.statusCode").value(400))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Product")));
    }

    @Test
    @DisplayName("POST /orders - insufficient stock failure")
    void createOrder_InsufficientStock_Failure() throws Exception {
        ReqOrderItemDTO item = new ReqOrderItemDTO();
        item.setProductId(lowStockProductId);
        item.setQuantity(999L); // stock is only 1
        item.setPrice(100.0);

        ReqOrderDTO req = validOrderRequest();
        req.setItems(List.of(item));

        mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.statusCode").value(400))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("tồn kho")));
    }

    @Test
    @DisplayName("GET /orders/{id} - order not found failure")
    void getOrderById_NotFound_Failure() throws Exception {
        mockMvc.perform(get("/api/v1/orders/99999"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.statusCode").value(400))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Order")));
    }

    // ======================= PAYMENT: SUCCESS CASES ========================

    @Test
    @DisplayName("POST /orders/{id}/payments - add payment success")
    void addPayment_Success() throws Exception {
        MvcResult orderResult = mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validOrderRequest())))
                .andExpect(status().isCreated())
                .andReturn();

        Integer orderId = objectMapper.readTree(orderResult.getResponse().getContentAsString())
                .path("data").path("id").asInt();

        ReqPaymentDTO payReq = new ReqPaymentDTO();
        payReq.setOrderId(orderId);
        payReq.setMethod(PaymentMethodEnum.CASH);
        payReq.setAmount(100.0);
        payReq.setStatus(StatusEnum.COMPLETED);

        mockMvc.perform(post("/api/v1/orders/" + orderId + "/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.orderId").value(orderId))
                .andExpect(jsonPath("$.data.amount").value(100.0))
                .andExpect(jsonPath("$.data.method").value("CASH"))
                .andExpect(jsonPath("$.data.status").value("COMPLETED"))
                .andExpect(jsonPath("$.data.createdAt").exists());
    }

    @Test
    @DisplayName("POST /orders/{id}/payments - revenue transaction created on completed payment")
    void addPayment_CreatesRevenueTransaction() throws Exception {
        MvcResult orderResult = mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validOrderRequest())))
                .andExpect(status().isCreated())
                .andReturn();

        Integer orderId = objectMapper.readTree(orderResult.getResponse().getContentAsString())
                .path("data").path("id").asInt();

        ReqPaymentDTO payReq = new ReqPaymentDTO();
        payReq.setOrderId(orderId);
        payReq.setMethod(PaymentMethodEnum.BANK);
        payReq.setAmount(100.0);
        payReq.setStatus(StatusEnum.COMPLETED);

        mockMvc.perform(post("/api/v1/orders/" + orderId + "/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payReq)))
                .andExpect(status().isCreated());

        // Assert REVENUE transaction created
        List<Transaction> txs = transactionRepository.findAllByOrder_Id(orderId);
        assertThat(txs).hasSize(1);
        assertThat(txs.get(0).getType()).isEqualTo(TypeTransactionEnum.REVENUE);
        assertThat(txs.get(0).getAmount()).isEqualTo(100.0);

        // Assert order status is now COMPLETED
        Order order = orderRepository.findById(orderId).orElseThrow();
        assertThat(order.getStatus()).isEqualTo(StatusEnum.COMPLETED);
        assertThat(order.getPaidAt()).isNotNull();
    }

    @Test
    @DisplayName("GET /orders/{id}/payments - get payment by order id success")
    void getPaymentByOrderId_Success() throws Exception {
        MvcResult orderResult = mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validOrderRequest())))
                .andExpect(status().isCreated())
                .andReturn();

        Integer orderId = objectMapper.readTree(orderResult.getResponse().getContentAsString())
                .path("data").path("id").asInt();

        ReqPaymentDTO payReq = new ReqPaymentDTO();
        payReq.setOrderId(orderId);
        payReq.setMethod(PaymentMethodEnum.CASH);
        payReq.setAmount(100.0);
        payReq.setStatus(StatusEnum.COMPLETED);

        mockMvc.perform(post("/api/v1/orders/" + orderId + "/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payReq)))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/orders/" + orderId + "/payments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.orderId").value(orderId))
                .andExpect(jsonPath("$.data.method").value("CASH"))
                .andExpect(jsonPath("$.data.amount").value(100.0));
    }

    // ======================= PAYMENT: FAILURE CASES ========================

    @Test
    @DisplayName("POST /orders/{id}/payments - order not found failure")
    void addPayment_OrderNotFound_Failure() throws Exception {
        ReqPaymentDTO payReq = new ReqPaymentDTO();
        payReq.setOrderId(99999);
        payReq.setMethod(PaymentMethodEnum.CASH);
        payReq.setAmount(100.0);

        mockMvc.perform(post("/api/v1/orders/99999/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payReq)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.statusCode").value(400))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Order")));
    }

    @Test
    @DisplayName("POST /orders/{id}/payments - invalid amount (negative) failure")
    void addPayment_InvalidAmount_Failure() throws Exception {
        MvcResult orderResult = mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validOrderRequest())))
                .andExpect(status().isCreated())
                .andReturn();

        Integer orderId = objectMapper.readTree(orderResult.getResponse().getContentAsString())
                .path("data").path("id").asInt();

        ReqPaymentDTO payReq = new ReqPaymentDTO();
        payReq.setOrderId(orderId);
        payReq.setMethod(PaymentMethodEnum.CASH);
        payReq.setAmount(-50.0); // invalid

        mockMvc.perform(post("/api/v1/orders/" + orderId + "/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payReq)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.statusCode").value(400))
                .andExpect(jsonPath("$.errors").isArray());
    }

    @Test
    @DisplayName("POST /orders/{id}/payments - malformed enum (invalid method) failure")
    void addPayment_MalformedEnum_Failure() throws Exception {
        MvcResult orderResult = mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validOrderRequest())))
                .andExpect(status().isCreated())
                .andReturn();

        Integer orderId = objectMapper.readTree(orderResult.getResponse().getContentAsString())
                .path("data").path("id").asInt();

        // Send invalid enum value as raw JSON string
        String invalidJson = """
            {"orderId": %d, "method": "INVALID_METHOD", "amount": 100.0}
            """.formatted(orderId);

        mockMvc.perform(post("/api/v1/orders/" + orderId + "/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.statusCode").value(400))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("thanh toán")));
    }

    @Test
    @DisplayName("POST /orders/{id}/payments - duplicate payment failure (idempotency)")
    void addPayment_Duplicate_Failure() throws Exception {
        MvcResult orderResult = mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validOrderRequest())))
                .andExpect(status().isCreated())
                .andReturn();

        Integer orderId = objectMapper.readTree(orderResult.getResponse().getContentAsString())
                .path("data").path("id").asInt();

        ReqPaymentDTO payReq = new ReqPaymentDTO();
        payReq.setOrderId(orderId);
        payReq.setMethod(PaymentMethodEnum.CASH);
        payReq.setAmount(100.0);
        payReq.setStatus(StatusEnum.COMPLETED);

        // First payment - success
        mockMvc.perform(post("/api/v1/orders/" + orderId + "/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payReq)))
                .andExpect(status().isCreated());

        // Second payment - duplicate
        mockMvc.perform(post("/api/v1/orders/" + orderId + "/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payReq)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.statusCode").value(400))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("giao dịch thanh toán")));

        // Assert only 1 transaction was created (no duplication)
        List<Transaction> txs = transactionRepository.findAllByOrder_Id(orderId);
        assertThat(txs).hasSize(1);
    }
}
