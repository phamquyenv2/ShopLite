package com.quyen.shoplite.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.quyen.shoplite.repository.OrderRepository;
import org.springframework.context.ApplicationEventPublisher;
import com.quyen.shoplite.repository.PaymentRepository;
import com.quyen.shoplite.service.payment.PaymentProvider;
import com.quyen.shoplite.service.payment.PaymentProviderFactory;
import com.quyen.shoplite.util.constant.PaymentMethodEnum;
import com.quyen.shoplite.util.constant.PaymentStatusEnum;
import com.quyen.shoplite.util.constant.RefTypeEnum;
import com.quyen.shoplite.util.error.IdInvalidException;

import com.quyen.shoplite.domain.Payment;
import com.quyen.shoplite.domain.Store;
import com.quyen.shoplite.domain.request.ReqPaymentDTO;
import com.quyen.shoplite.domain.response.ResPaymentDTO;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock private PaymentRepository paymentRepository;
    @Mock private OrderRepository orderRepository;
    @Mock private TransactionService transactionService;
    @Mock private PaymentProviderFactory paymentProviderFactory;
    @Mock private ObjectMapper objectMapper;
    @Mock private com.quyen.shoplite.repository.FundAccountRepository fundAccountRepository;
    @Mock private CurrentStoreService currentStoreService;
    @Mock private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private PaymentService paymentService;

    private Validator validator;

    private Store testStore() {
        Store s = new Store();
        s.setId(1L);
        return s;
    }

    @BeforeEach
    void setUp() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Test
    void createPaymentSession_DuplicatePaidPayment_ThrowsException() {
        Integer orderId = 1;
        ReqPaymentDTO req = new ReqPaymentDTO();
        req.setReferenceType(RefTypeEnum.ORDER);
        req.setReferenceId(orderId);
        req.setPaymentMethod(PaymentMethodEnum.CASH);
        req.setAmount(BigDecimal.valueOf(100.0));

        when(currentStoreService.getCurrentStore()).thenReturn(testStore());
        when(paymentRepository.existsByStoreIdAndReferenceTypeAndReferenceIdAndStatusIn(
                1L, RefTypeEnum.ORDER, orderId,
                List.of(PaymentStatusEnum.PENDING, PaymentStatusEnum.COMPLETED)))
                .thenReturn(true);

        assertThrows(IdInvalidException.class, () -> paymentService.createPaymentSession(req));
    }

    @Test
    void createPaymentSession_OrderNotFound_ThrowsException() {
        ReqPaymentDTO req = new ReqPaymentDTO();
        req.setReferenceType(RefTypeEnum.ORDER);
        req.setReferenceId(999);
        req.setPaymentMethod(PaymentMethodEnum.EWALLET);
        req.setAmount(BigDecimal.valueOf(100.0));

        when(currentStoreService.getCurrentStore()).thenReturn(testStore());
        when(paymentRepository.existsByStoreIdAndReferenceTypeAndReferenceIdAndStatusIn(
                any(), any(), any(), any())).thenReturn(false);

        PaymentProvider mockProvider = org.mockito.Mockito.mock(PaymentProvider.class);
        when(paymentProviderFactory.getProvider(PaymentMethodEnum.EWALLET)).thenReturn(mockProvider);
        when(orderRepository.findByIdAndStoreId(999, 1L)).thenReturn(Optional.empty());

        assertThrows(IdInvalidException.class, () -> paymentService.createPaymentSession(req));
    }

    @Test
    void validateReqPaymentDTO_MissingReferenceType() {
        ReqPaymentDTO req = new ReqPaymentDTO();
        req.setReferenceId(1);
        req.setPaymentMethod(PaymentMethodEnum.CASH);
        req.setAmount(BigDecimal.valueOf(100.0));

        Set<ConstraintViolation<ReqPaymentDTO>> violations = validator.validate(req);
        assertTrue(violations.stream().anyMatch(v ->
                v.getPropertyPath().toString().equals("referenceType")));
    }

    @Test
    void validateReqPaymentDTO_InvalidAmount() {
        ReqPaymentDTO req = new ReqPaymentDTO();
        req.setReferenceType(RefTypeEnum.ORDER);
        req.setReferenceId(1);
        req.setPaymentMethod(PaymentMethodEnum.CASH);
        req.setAmount(BigDecimal.valueOf(-50.0));

        Set<ConstraintViolation<ReqPaymentDTO>> violations = validator.validate(req);
        assertTrue(violations.stream().anyMatch(v ->
                v.getMessage().contains("lớn hơn 0")));
    }
}
