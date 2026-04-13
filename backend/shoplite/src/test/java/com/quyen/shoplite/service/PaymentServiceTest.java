package com.quyen.shoplite.service;

import com.quyen.shoplite.domain.Order;
import com.quyen.shoplite.domain.Payment;
import com.quyen.shoplite.domain.request.ReqPaymentDTO;
import com.quyen.shoplite.domain.request.ReqTransactionDTO;
import com.quyen.shoplite.domain.response.ResPaymentDTO;
import com.quyen.shoplite.repository.OrderRepository;
import com.quyen.shoplite.repository.PaymentRepository;
import com.quyen.shoplite.util.constant.PaymentMethodEnum;
import com.quyen.shoplite.util.constant.StatusEnum;
import com.quyen.shoplite.util.constant.TypeTransactionEnum;
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
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private OrderRepository orderRepository;
    @Mock
    private TransactionService transactionService;

    @InjectMocks
    private PaymentService paymentService;

    private Validator validator;

    @BeforeEach
    void setUp() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Test
    void createPayment_Success() {
        // Arrange
        Integer orderId = 1;
        ReqPaymentDTO req = new ReqPaymentDTO();
        req.setOrderId(orderId);
        req.setMethod(PaymentMethodEnum.BANK);
        req.setAmount(100.0);
        // Status is PENDING by default in DTO, but service explicitly saves as req.getStatus() or COMPLETED
        req.setStatus(StatusEnum.COMPLETED);

        Order order = new Order();
        order.setId(orderId);
        order.setCode("ORD-123");
        order.setStatus(StatusEnum.PENDING);

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(paymentRepository.findByOrder_Id(orderId)).thenReturn(Optional.empty());

        Payment savedPayment = new Payment();
        savedPayment.setId(10);
        savedPayment.setAmount(100.0);
        savedPayment.setMethod(PaymentMethodEnum.BANK);
        savedPayment.setStatus(StatusEnum.COMPLETED);
        savedPayment.setOrder(order);
        
        when(paymentRepository.save(any(Payment.class))).thenReturn(savedPayment);

        // Act
        ResPaymentDTO result = paymentService.createPayment(orderId, req);

        // Assert
        assertNotNull(result);
        assertEquals(10, result.getId());
        assertEquals(PaymentMethodEnum.BANK, result.getMethod());
        assertEquals(StatusEnum.COMPLETED, result.getStatus());

        // Verify Transaction
        ArgumentCaptor<ReqTransactionDTO> transactionCaptor = ArgumentCaptor.forClass(ReqTransactionDTO.class);
        verify(transactionService).create(transactionCaptor.capture());
        ReqTransactionDTO capturedTx = transactionCaptor.getValue();
        assertEquals(orderId, capturedTx.getOrderId());
        assertEquals(100.0, capturedTx.getAmount());
        assertEquals(TypeTransactionEnum.REVENUE, capturedTx.getType());
        assertTrue(capturedTx.getContent().contains("ORD-123"));
        assertNotNull(capturedTx.getTransactionTime());

        // Verify Order updated
        ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).save(orderCaptor.capture());
        Order updatedOrder = orderCaptor.getValue();
        assertEquals(StatusEnum.COMPLETED, updatedOrder.getStatus());
        assertNotNull(updatedOrder.getPaidAt());
    }

    @Test
    void createPayment_OrderNotFound_ThrowsException() {
        Integer orderId = 999;
        ReqPaymentDTO req = new ReqPaymentDTO();
        when(orderRepository.findById(orderId)).thenReturn(Optional.empty());

        IdInvalidException ex = assertThrows(IdInvalidException.class, () -> paymentService.createPayment(orderId, req));
        assertTrue(ex.getMessage().contains("Không tìm thấy Order"));
    }

    @Test
    void createPayment_DuplicatePayment_ThrowsException() {
        Integer orderId = 1;
        ReqPaymentDTO req = new ReqPaymentDTO();

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(new Order()));
        when(paymentRepository.findByOrder_Id(orderId)).thenReturn(Optional.of(new Payment()));

        IdInvalidException ex = assertThrows(IdInvalidException.class, () -> paymentService.createPayment(orderId, req));
        assertTrue(ex.getMessage().contains("Đơn hàng đã có giao dịch"));
    }

    @Test
    void validateReqPaymentDTO_InvalidAmount() {
        ReqPaymentDTO req = new ReqPaymentDTO();
        req.setOrderId(1);
        req.setMethod(PaymentMethodEnum.CASH);
        req.setAmount(-50.0); // should be strictly positive

        Set<ConstraintViolation<ReqPaymentDTO>> violations = validator.validate(req);
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("lớn hơn 0")));
    }
}
