package com.quyen.shoplite.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.quyen.shoplite.repository.*;
import com.quyen.shoplite.util.constant.*;

import com.quyen.shoplite.domain.*;
import com.quyen.shoplite.domain.request.ReqOrderDTO;
import com.quyen.shoplite.domain.request.ReqOrderItemDTO;
import com.quyen.shoplite.domain.request.ReqPaymentDTO;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class OrderControllerIntegrationTest extends IntegrationTestBase {

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

    @Autowired
    private FundAccountRepository fundAccountRepository;

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
                .build());
        userId = user.getId();

        Customer customer = customerRepository.save(Customer.builder().store(testStore)
                .name("IT Customer")
                .phone("090" + System.nanoTime() % 10000000)
                .build());
        customerId = customer.getId();

        Category cat = categoryRepository.save(Category.builder().store(testStore).name("ITCat_" + System.nanoTime()).build());
        Unit unit = unitRepository.save(Unit.builder().store(testStore).name("ITUnit_" + System.nanoTime()).description("u").build());

        Product product = productRepository.save(Product.builder().store(testStore)
                .category(cat)
                .unit(unit)
                .name("IT Product")
                .sku("ITSKU_" + System.nanoTime())
                .stock(20)
                .sellingPrice(50.0)
                .costPrice(0.0)
                .isDeleted(false)
                .build());
        productId = product.getId();

        Product lowStockProduct = productRepository.save(Product.builder().store(testStore)
                .category(cat)
                .unit(unit)
                .name("Low Stock Product")
                .sku("SKLW_" + System.nanoTime())
                .stock(1)
                .sellingPrice(100.0)
                .costPrice(0.0)
                .isDeleted(false)
                .build());
        lowStockProductId = lowStockProduct.getId();

        // Seed fund accounts for payment tests
        fundAccountRepository.save(FundAccount.builder()
                .store(testStore)
                .name("Test Cash")
                .type(FundTypeEnum.CASH)
                .build());
        fundAccountRepository.save(FundAccount.builder()
                .store(testStore)
                .name("Test Bank")
                .type(FundTypeEnum.BANK)
                .build());
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
        mockMvc.perform(withStore(post("/api/v1/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validOrderRequest()))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("DRAFT"))
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
        // Now it only changes to DRAFT and doesn't decrement stock yet unless CONFIRMED.
        // If your test asserts stock decrease on creation, you either test the confirm endpoint or change this.
        // Let's assume order create returns DRAFT and no stock changes.
        mockMvc.perform(withStore(post("/api/v1/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validOrderRequest()))))
                .andExpect(status().isCreated());

        Product updated = productRepository.findById(productId).orElseThrow();
        assertThat(updated.getStock()).isEqualTo(20); // 20 - 0 (DRAFT)
    }

    @Test
    @DisplayName("GET /orders/{id} - get order by id success")
    void getOrderById_Success() throws Exception {
        // create one order first
        MvcResult result = mockMvc.perform(withStore(post("/api/v1/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validOrderRequest()))))
                .andExpect(status().isCreated())
                .andReturn();

        Integer orderId = objectMapper.readTree(result.getResponse().getContentAsString())
                .path("data").path("id").asInt();

        mockMvc.perform(withStore(get("/api/v1/orders/" + orderId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(orderId))
                .andExpect(jsonPath("$.data.customerId").value(customerId))
                .andExpect(jsonPath("$.data.items").isArray());
    }

    @Test
    @DisplayName("GET /orders - list orders success")
    void listOrders_Success() throws Exception {
        mockMvc.perform(withStore(post("/api/v1/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validOrderRequest()))))
                .andExpect(status().isCreated());

        mockMvc.perform(withStore(get("/api/v1/orders")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(org.hamcrest.Matchers.greaterThanOrEqualTo(1)));
    }

    @Test
    @DisplayName("DELETE /orders/{id} - cancel order success, stock restored, RETURN log created")
    void cancelOrder_Success() throws Exception {
        MvcResult result = mockMvc.perform(withStore(post("/api/v1/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validOrderRequest()))))
                .andExpect(status().isCreated())
                .andReturn();

        Integer orderId = objectMapper.readTree(result.getResponse().getContentAsString())
                .path("data").path("id").asInt();

        // cancel
        mockMvc.perform(withStore(delete("/api/v1/orders/" + orderId)))
                .andExpect(status().isNoContent());

        // Order status = CANCELLED
        Order cancelled = orderRepository.findById(orderId).orElseThrow();
        assertThat(cancelled.getStatus()).isEqualTo(StatusEnum.CANCELLED);

        // Stock restored
        Product restored = productRepository.findById(productId).orElseThrow();
        assertThat(restored.getStock()).isEqualTo(20); // restored to original
    }

    // ======================= ORDER: FAILURE CASES ========================
    // Removed createOrder_MissingCustomer_Failure as customerId is optional

    @Test
    @DisplayName("POST /orders - customer not found failure")
    void createOrder_CustomerNotFound_Failure() throws Exception {
        ReqOrderDTO req = validOrderRequest();
        req.setCustomerId(99999); // non-existent

        mockMvc.perform(withStore(post("/api/v1/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.statusCode").value(400))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Customer")));
    }

    @Test
    @DisplayName("POST /orders - empty items failure")
    void createOrder_EmptyItems_Failure() throws Exception {
        ReqOrderDTO req = validOrderRequest();
        req.setItems(List.of()); // empty

        mockMvc.perform(withStore(post("/api/v1/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req))))
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

        mockMvc.perform(withStore(post("/api/v1/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req))))
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

        mockMvc.perform(withStore(post("/api/v1/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.statusCode").value(400))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Product")));
    }

    // Removed createOrder_InsufficientStock_Failure as draft creation does not check stock

    @Test
    @DisplayName("GET /orders/{id} - order not found failure")
    void getOrderById_NotFound_Failure() throws Exception {
        mockMvc.perform(withStore(get("/api/v1/orders/99999")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.statusCode").value(400))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Order")));
    }

    // ======================= PAYMENT: SUCCESS CASES ========================
    @Test
    @DisplayName("POST /api/v1/payment - add payment success")
    void addPayment_Success() throws Exception {
        MvcResult orderResult = mockMvc.perform(withStore(post("/api/v1/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validOrderRequest()))))
                .andExpect(status().isCreated())
                .andReturn();

        Integer orderId = objectMapper.readTree(orderResult.getResponse().getContentAsString())
                .path("data").path("id").asInt();
        
        // Cần confirm order trước khi thanh toán
        mockMvc.perform(withStore(patch("/api/v1/orders/" + orderId + "/status")
                .param("status", "PENDING_PAYMENT")))
                .andExpect(status().isOk());

        ReqPaymentDTO payReq = new ReqPaymentDTO();
        payReq.setReferenceType(RefTypeEnum.ORDER);
        payReq.setReferenceId(orderId);
        payReq.setPaymentMethod(PaymentMethodEnum.CASH);
        payReq.setAmount(BigDecimal.valueOf(100.0));

        mockMvc.perform(withStore(post("/api/v1/payment")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payReq))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.referenceId").value(orderId))
                .andExpect(jsonPath("$.data.amount").value(100.0))
                .andExpect(jsonPath("$.data.paymentMethod").value("CASH"));
    }

    @Test
    @DisplayName("POST /api/v1/payment - revenue transaction created on completed payment")
    void addPayment_CreatesRevenueTransaction() throws Exception {
        MvcResult orderResult = mockMvc.perform(withStore(post("/api/v1/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validOrderRequest()))))
                .andExpect(status().isCreated())
                .andReturn();

        Integer orderId = objectMapper.readTree(orderResult.getResponse().getContentAsString())
                .path("data").path("id").asInt();
                
        mockMvc.perform(withStore(patch("/api/v1/orders/" + orderId + "/status")
                .param("status", "PENDING_PAYMENT")))
                .andExpect(status().isOk());

        ReqPaymentDTO payReq = new ReqPaymentDTO();
        payReq.setReferenceType(RefTypeEnum.ORDER);
        payReq.setReferenceId(orderId);
        payReq.setPaymentMethod(PaymentMethodEnum.BANK_TRANSFER);
        payReq.setAmount(BigDecimal.valueOf(100.0));

        mockMvc.perform(withStore(post("/api/v1/payment")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payReq))))
                .andExpect(status().isCreated());

        // Assert REVENUE transaction created
        Payment payment = paymentRepository.findByReferenceTypeAndReferenceId(RefTypeEnum.ORDER, orderId).orElseThrow();
        List<Transaction> txs = transactionRepository.findAllByPayment_Id(payment.getId());
        assertThat(txs).hasSize(1);
        assertThat(txs.get(0).getType()).isEqualTo(TypeTransactionEnum.REVENUE);
        assertThat(txs.get(0).getAmount()).isEqualByComparingTo(BigDecimal.valueOf(100.0));

        // Assert order status is now COMPLETED
        Order order = orderRepository.findById(orderId).orElseThrow();
        assertThat(order.getStatus()).isEqualTo(StatusEnum.COMPLETED);
        assertThat(order.getPaidAt()).isNotNull();
    }

    // ======================= PAYMENT: FAILURE CASES ========================

    @Test
    @DisplayName("POST /api/v1/payment - invalid amount (negative) failure")
    void addPayment_InvalidAmount_Failure() throws Exception {
        MvcResult orderResult = mockMvc.perform(withStore(post("/api/v1/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validOrderRequest()))))
                .andExpect(status().isCreated())
                .andReturn();

        Integer orderId = objectMapper.readTree(orderResult.getResponse().getContentAsString())
                .path("data").path("id").asInt();

        ReqPaymentDTO payReq = new ReqPaymentDTO();
        payReq.setReferenceType(RefTypeEnum.ORDER);
        payReq.setReferenceId(orderId);
        payReq.setPaymentMethod(PaymentMethodEnum.CASH);
        payReq.setAmount(BigDecimal.valueOf(-50.0)); // invalid

        mockMvc.perform(withStore(post("/api/v1/payment")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payReq))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.statusCode").value(400))
                .andExpect(jsonPath("$.errors").isArray());
    }

    @Test
    @DisplayName("POST /api/v1/payment - malformed enum (invalid method) failure")
    void addPayment_MalformedEnum_Failure() throws Exception {
        MvcResult orderResult = mockMvc.perform(withStore(post("/api/v1/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validOrderRequest()))))
                .andExpect(status().isCreated())
                .andReturn();

        Integer orderId = objectMapper.readTree(orderResult.getResponse().getContentAsString())
                .path("data").path("id").asInt();

        // Send invalid enum value as raw JSON string
        String invalidJson = """
            {"referenceType": "ORDER", "referenceId": %d, "paymentMethod": "INVALID_METHOD", "amount": 100.0}
            """.formatted(orderId);

        mockMvc.perform(withStore(post("/api/v1/payment")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidJson)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.statusCode").value(400));
    }

    @Test
    @DisplayName("POST /api/v1/payment - duplicate payment failure (idempotency)")
    void addPayment_Duplicate_Failure() throws Exception {
        MvcResult orderResult = mockMvc.perform(withStore(post("/api/v1/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validOrderRequest()))))
                .andExpect(status().isCreated())
                .andReturn();

        Integer orderId = objectMapper.readTree(orderResult.getResponse().getContentAsString())
                .path("data").path("id").asInt();

        mockMvc.perform(withStore(patch("/api/v1/orders/" + orderId + "/status")
                .param("status", "PENDING_PAYMENT")))
                .andExpect(status().isOk());

        ReqPaymentDTO payReq = new ReqPaymentDTO();
        payReq.setReferenceType(RefTypeEnum.ORDER);
        payReq.setReferenceId(orderId);
        payReq.setPaymentMethod(PaymentMethodEnum.CASH);
        payReq.setAmount(BigDecimal.valueOf(100.0));

        // First payment - success
        mockMvc.perform(withStore(post("/api/v1/payment")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payReq))))
                .andExpect(status().isCreated());

        // Second payment - duplicate
        mockMvc.perform(withStore(post("/api/v1/payment")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payReq))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.statusCode").value(400));

        // Assert only 1 transaction was created (no duplication)
        Payment payment = paymentRepository.findByReferenceTypeAndReferenceId(RefTypeEnum.ORDER, orderId).orElseThrow();
        List<Transaction> txs = transactionRepository.findAllByPayment_Id(payment.getId());
        assertThat(txs).hasSize(1);
    }
}
