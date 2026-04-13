package com.quyen.shoplite.service;

import com.quyen.shoplite.domain.*;
import com.quyen.shoplite.domain.request.ReqOrderDTO;
import com.quyen.shoplite.domain.request.ReqOrderItemDTO;
import com.quyen.shoplite.domain.response.ResOrderDTO;
import com.quyen.shoplite.repository.*;
import com.quyen.shoplite.util.constant.StatusEnum;
import com.quyen.shoplite.util.constant.TypeInventoryEnum;
import com.quyen.shoplite.util.error.IdInvalidException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;
    @Mock
    private OrderItemsRepository orderItemsRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private ProductRepository productRepository;
    @Mock
    private InventoryLogsRepository inventoryLogsRepository;
    @Mock
    private CustomerRepository customerRepository;
    @Mock
    private FcmService fcmService;

    @InjectMocks
    private OrderService orderService;

    private Validator validator;

    @BeforeEach
    void setUp() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Test
    void createOrder_Success() {
        // Arrange
        ReqOrderDTO req = new ReqOrderDTO();
        req.setUserId(1);
        req.setCustomerId(1);
        req.setDiscount(10.0);

        ReqOrderItemDTO item = new ReqOrderItemDTO();
        item.setProductId(1);
        item.setQuantity(2L);
        item.setPrice(100.0);
        req.setItems(List.of(item));

        User user = new User();
        user.setId(1);

        Customer customer = new Customer();
        customer.setId(1);

        Product product = new Product();
        product.setId(1);
        product.setName("Product A");
        product.setStock(5);

        when(userRepository.findById(1)).thenReturn(Optional.of(user));
        when(customerRepository.findById(1)).thenReturn(Optional.of(customer));
        when(productRepository.findById(1)).thenReturn(Optional.of(product));
        
        Order savedOrder = new Order();
        savedOrder.setId(100);
        savedOrder.setCode("ORD-TEST");
        when(orderRepository.save(any(Order.class))).thenReturn(savedOrder);
        when(inventoryLogsRepository.save(any())).thenReturn(new InventoryLogs());

        // Act
        ResOrderDTO result = orderService.create(req);

        // Assert
        assertNotNull(result);
        assertEquals(100, result.getId());
        
        // Verify Order
        ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).save(orderCaptor.capture());
        Order capturedOrder = orderCaptor.getValue();
        assertEquals(1, capturedOrder.getUser().getId());
        assertEquals(1, capturedOrder.getCustomer().getId());
        assertEquals(190.0, capturedOrder.getTotalAmount()); // 2 * 100 - 10
        assertEquals(10.0, capturedOrder.getDiscount());
        assertEquals(StatusEnum.PENDING, capturedOrder.getStatus());
        
        // Verify OrderItems
        ArgumentCaptor<OrderItems> orderItemsCaptor = ArgumentCaptor.forClass(OrderItems.class);
        verify(orderItemsRepository).save(orderItemsCaptor.capture());
        OrderItems capturedItem = orderItemsCaptor.getValue();
        assertEquals(savedOrder, capturedItem.getOrder());
        assertEquals("Product A", capturedItem.getProductName());
        assertEquals(2L, capturedItem.getQuantity());
        assertEquals(100.0, capturedItem.getPrice());
        assertEquals(200.0, capturedItem.getTotalPrice());

        // Verify Product Stock
        assertEquals(3, product.getStock());
        verify(productRepository).save(product);

        // Verify InventoryLog
        ArgumentCaptor<InventoryLogs> logCaptor = ArgumentCaptor.forClass(InventoryLogs.class);
        verify(inventoryLogsRepository).save(logCaptor.capture());
        InventoryLogs log = logCaptor.getValue();
        assertEquals(1, log.getProduct().getId());
        assertEquals(capturedItem, log.getOrderItem());
        assertEquals(2, log.getQuantityOut());
        assertEquals(3, log.getBalanceAfter());
        assertEquals(3, log.getCurrentStock());
        assertEquals(TypeInventoryEnum.SALE, log.getType());
        
        // Verify FCM
        try {
            verify(fcmService).sendPaymentSuccessNotification(savedOrder);
        } catch (Exception e) {
            fail(e);
        }
    }

    @Test
    void createOrder_CustomerNotFound_ThrowsException() {
        ReqOrderDTO req = new ReqOrderDTO();
        req.setUserId(1);
        req.setCustomerId(999);
        req.setItems(new ArrayList<>());
        
        when(userRepository.findById(1)).thenReturn(Optional.of(new User()));
        when(customerRepository.findById(999)).thenReturn(Optional.empty());

        IdInvalidException ex = assertThrows(IdInvalidException.class, () -> orderService.create(req));
        assertTrue(ex.getMessage().contains("Không tìm thấy Customer"));
    }

    @Test
    void createOrder_ProductNotFound_ThrowsException() {
        ReqOrderDTO req = new ReqOrderDTO();
        req.setUserId(1);
        req.setCustomerId(1);
        ReqOrderItemDTO item = new ReqOrderItemDTO();
        item.setProductId(999);
        req.setItems(List.of(item));

        when(userRepository.findById(1)).thenReturn(Optional.of(new User()));
        when(customerRepository.findById(1)).thenReturn(Optional.of(new Customer()));
        when(productRepository.findById(999)).thenReturn(Optional.empty());

        IdInvalidException ex = assertThrows(IdInvalidException.class, () -> orderService.create(req));
        assertTrue(ex.getMessage().contains("Không tìm thấy Product"));
    }

    @Test
    void createOrder_InsufficientStock_ThrowsException() {
        ReqOrderDTO req = new ReqOrderDTO();
        req.setUserId(1);
        req.setCustomerId(1);
        ReqOrderItemDTO item = new ReqOrderItemDTO();
        item.setProductId(1);
        item.setQuantity(10L); // Need 10
        req.setItems(List.of(item));

        Product product = new Product();
        product.setId(1);
        product.setName("Product A");
        product.setStock(5); // Only have 5

        when(userRepository.findById(1)).thenReturn(Optional.of(new User()));
        when(customerRepository.findById(1)).thenReturn(Optional.of(new Customer()));
        when(productRepository.findById(1)).thenReturn(Optional.of(product));

        IdInvalidException ex = assertThrows(IdInvalidException.class, () -> orderService.create(req));
        assertTrue(ex.getMessage().contains("không đủ tồn kho"));
    }

    @Test
    void createOrder_InvalidNegativeTotal_ThrowsException() {
        ReqOrderDTO req = new ReqOrderDTO();
        req.setUserId(1);
        req.setCustomerId(1);
        req.setDiscount(500.0); // very high discount
        
        ReqOrderItemDTO item = new ReqOrderItemDTO();
        item.setProductId(1);
        item.setQuantity(1L);
        item.setPrice(100.0); // Total price 100
        req.setItems(List.of(item));

        Product product = new Product();
        product.setId(1);
        product.setStock(5);

        when(userRepository.findById(1)).thenReturn(Optional.of(new User()));
        when(customerRepository.findById(1)).thenReturn(Optional.of(new Customer()));
        when(productRepository.findById(1)).thenReturn(Optional.of(product));

        IdInvalidException ex = assertThrows(IdInvalidException.class, () -> orderService.create(req));
        assertTrue(ex.getMessage().contains("không được âm"));
    }

    @Test
    void validateReqOrderDTO_EmptyItems() {
        ReqOrderDTO req = new ReqOrderDTO();
        req.setUserId(1);
        req.setCustomerId(1);
        req.setItems(new ArrayList<>());

        Set<ConstraintViolation<ReqOrderDTO>> violations = validator.validate(req);
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("items must not be empty")));
    }

    @Test
    void validateReqOrderItemDTO_InvalidQuantity() {
        ReqOrderItemDTO item = new ReqOrderItemDTO();
        item.setProductId(1);
        item.setQuantity(0L); // invalid, must be positive
        item.setPrice(100.0);

        Set<ConstraintViolation<ReqOrderItemDTO>> violations = validator.validate(item);
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("greater than 0")));
    }

    @Test
    void findById_Success() {
        Order order = new Order();
        order.setId(1);
        when(orderRepository.findById(1)).thenReturn(Optional.of(order));

        OrderItems item = new OrderItems();
        item.setId(10);
        item.setQuantity(2L);
        when(orderItemsRepository.findAllByOrderId(1)).thenReturn(List.of(item));

        ResOrderDTO result = orderService.findById(1);

        assertNotNull(result);
        assertEquals(1, result.getId());
        assertEquals(1, result.getItems().size());
        assertEquals(10, result.getItems().get(0).getId());
    }

    @Test
    void findAll_Success() {
        Order order = new Order();
        order.setId(1);
        when(orderRepository.findAll()).thenReturn(List.of(order));
        when(orderItemsRepository.findAllByOrderId(1)).thenReturn(List.of());

        List<ResOrderDTO> result = orderService.findAll();

        assertEquals(1, result.size());
        assertEquals(1, result.get(0).getId());
    }

    @Test
    void cancelOrder_Success() {
        Order order = new Order();
        order.setId(1);
        order.setStatus(StatusEnum.PENDING);
        
        when(orderRepository.findById(1)).thenReturn(Optional.of(order));

        Product product = new Product();
        product.setId(2);
        product.setStock(5);

        OrderItems item = new OrderItems();
        item.setId(10);
        item.setProduct(product);
        item.setQuantity(3L); // Restore 3
        
        when(orderItemsRepository.findAllByOrderId(1)).thenReturn(List.of(item));

        orderService.cancel(1);

        assertEquals(StatusEnum.CANCELLED, order.getStatus());
        verify(orderRepository).save(order);

        // Verify product stock restored
        assertEquals(8, product.getStock());
        verify(productRepository).save(product);

        // Verify inventory log
        ArgumentCaptor<InventoryLogs> logCaptor = ArgumentCaptor.forClass(InventoryLogs.class);
        verify(inventoryLogsRepository).save(logCaptor.capture());
        InventoryLogs log = logCaptor.getValue();
        assertEquals(2, log.getProduct().getId());
        assertEquals(item, log.getOrderItem());
        assertEquals(3, log.getQuantityIn());
        assertEquals(8, log.getBalanceAfter());
        assertEquals(8, log.getCurrentStock());
        assertEquals(TypeInventoryEnum.RETURN, log.getType());
    }

    @Test
    void cancelOrder_AlreadyCancelled_ThrowsException() {
        Order order = new Order();
        order.setId(1);
        order.setStatus(StatusEnum.CANCELLED);
        
        when(orderRepository.findById(1)).thenReturn(Optional.of(order));

        IdInvalidException ex = assertThrows(IdInvalidException.class, () -> orderService.cancel(1));
        assertTrue(ex.getMessage().contains("đã được huỷ"));
    }
}
