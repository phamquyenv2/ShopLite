package com.quyen.shoplite.service;

import com.quyen.shoplite.domain.*;
import com.quyen.shoplite.domain.request.ReqOrderDTO;
import com.quyen.shoplite.domain.request.ReqOrderItemDTO;
import com.quyen.shoplite.domain.response.ResOrderDTO;
import com.quyen.shoplite.service.OrderService.CreateOrderResult;
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
    private PaymentRepository paymentRepository;
    @Mock
    private FcmService fcmService;
    @Mock
    private CurrentStoreService currentStoreService;

    @InjectMocks
    private OrderService orderService;

    private Validator validator;

    @BeforeEach
    void setUp() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    private Store testStore() {
        Store store = new Store();
        store.setId(1L);
        return store;
    }

    @Test
    void createOrder_Success() {
        Store store = testStore();
        when(currentStoreService.getCurrentStore()).thenReturn(store);

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
        when(customerRepository.findByIdAndStoreId(1, 1L)).thenReturn(Optional.of(customer));
        when(productRepository.findByIdAndStoreIdAndIsDeletedFalse(1, 1L)).thenReturn(Optional.of(product));
        
        Order savedOrder = new Order();
        savedOrder.setId(100);
        savedOrder.setCode("ORD-TEST");
        savedOrder.setStore(store);
        when(orderRepository.save(any(Order.class))).thenReturn(savedOrder);

        // Act
        CreateOrderResult resultWrapper = orderService.create(req);
        ResOrderDTO result = resultWrapper.order();

        // Assert
        assertNotNull(result);
        assertEquals(100, result.getId());
        
        // Verify Order
        ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).save(orderCaptor.capture());
        Order capturedOrder = orderCaptor.getValue();
    }

    @Test
    void createOrder_CustomerNotFound_ThrowsException() {
        when(currentStoreService.getCurrentStore()).thenReturn(testStore());

        ReqOrderDTO req = new ReqOrderDTO();
        req.setUserId(1);
        req.setCustomerId(999);
        req.setItems(new ArrayList<>());
        
        when(userRepository.findById(1)).thenReturn(Optional.of(new User()));
        when(customerRepository.findByIdAndStoreId(999, 1L)).thenReturn(Optional.empty());

        IdInvalidException ex = assertThrows(IdInvalidException.class, () -> orderService.create(req).order());
        assertTrue(ex.getMessage().contains("Không tìm thấy Customer"));
    }

    @Test
    void createOrder_ProductNotFound_ThrowsException() {
        when(currentStoreService.getCurrentStore()).thenReturn(testStore());

        ReqOrderDTO req = new ReqOrderDTO();
        req.setUserId(1);
        req.setCustomerId(1);
        ReqOrderItemDTO item = new ReqOrderItemDTO();
        item.setProductId(999);
        req.setItems(List.of(item));

        when(userRepository.findById(1)).thenReturn(Optional.of(new User()));
        when(customerRepository.findByIdAndStoreId(1, 1L)).thenReturn(Optional.of(new Customer()));
        when(productRepository.findByIdAndStoreIdAndIsDeletedFalse(999, 1L)).thenReturn(Optional.empty());

        IdInvalidException ex = assertThrows(IdInvalidException.class, () -> orderService.create(req).order());
        assertTrue(ex.getMessage().contains("Không tìm thấy Product"));
    }

    @Test
    void createOrder_InvalidNegativeTotal_ThrowsException() {
        when(currentStoreService.getCurrentStore()).thenReturn(testStore());

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
        when(customerRepository.findByIdAndStoreId(1, 1L)).thenReturn(Optional.of(new Customer()));
        when(productRepository.findByIdAndStoreIdAndIsDeletedFalse(1, 1L)).thenReturn(Optional.of(product));

        IdInvalidException ex = assertThrows(IdInvalidException.class, () -> orderService.create(req).order());
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
        when(currentStoreService.getCurrentStoreId()).thenReturn(1L);

        Order order = new Order();
        order.setId(1);
        order.setStore(testStore());
        when(orderRepository.findByIdAndStoreId(1, 1L)).thenReturn(Optional.of(order));

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
        when(currentStoreService.getCurrentStoreId()).thenReturn(1L);

        Order order = new Order();
        order.setId(1);
        order.setStore(testStore());
        when(orderRepository.findAllByStoreId(eq(1L), any(org.springframework.data.domain.Sort.class))).thenReturn(List.of(order));
        when(orderItemsRepository.findAllByOrderId(1)).thenReturn(List.of());

        List<ResOrderDTO> result = orderService.findAll(null, null, null);

        assertEquals(1, result.size());
        assertEquals(1, result.get(0).getId());
    }

    @Test
    void cancelOrder_Success() {
        when(currentStoreService.getCurrentStoreId()).thenReturn(1L);

        Order order = new Order();
        order.setId(1);
        order.setStore(testStore());
        order.setStatus(StatusEnum.PENDING);
        order.setConfirmedAt(java.time.LocalDateTime.now());
        
        when(orderRepository.findByIdAndStoreIdWithLock(1, 1L)).thenReturn(Optional.of(order));

        Product product = new Product();
        product.setId(2);
        product.setStock(5);

        OrderItems item = new OrderItems();
        item.setId(10);
        item.setProduct(product);
        item.setQuantity(3L); // Restore 3
        
        when(orderItemsRepository.findAllByOrderId(1)).thenReturn(List.of(item));
        when(productRepository.findByIdAndStoreIdWithLock(2, 1L)).thenReturn(Optional.of(product));

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
        when(currentStoreService.getCurrentStoreId()).thenReturn(1L);

        Order order = new Order();
        order.setId(1);
        order.setStatus(StatusEnum.CANCELLED);
        
        when(orderRepository.findByIdAndStoreIdWithLock(1, 1L)).thenReturn(Optional.of(order));

        IdInvalidException ex = assertThrows(IdInvalidException.class, () -> orderService.cancel(1));
        assertTrue(ex.getMessage().contains("đã được huỷ"));
    }
}
