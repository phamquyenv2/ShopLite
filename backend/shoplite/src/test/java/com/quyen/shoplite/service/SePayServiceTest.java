package com.quyen.shoplite.service;

import com.quyen.shoplite.domain.Order;
import com.quyen.shoplite.domain.OrderCompletedEvent;
import com.quyen.shoplite.domain.Transaction;
import com.quyen.shoplite.repository.OrderRepository;
import com.quyen.shoplite.repository.TransactionRepository;
import com.quyen.shoplite.util.constant.StatusEnum;
import com.quyen.shoplite.util.constant.TypeTransactionEnum;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SePayServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private SePayService sePayService;

    @Test
    void processWebhook_UsesLockedOrderAndCompletesPayment() {
        Order order = new Order();
        order.setId(1);
        order.setCode("ORDER_12345");
        order.setStatus(StatusEnum.PENDING);
        order.setTotalAmount(250.0);

        when(orderRepository.findByCodeWithLock("ORDER_12345")).thenReturn(Optional.of(order));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        sePayService.processWebhook(Map.of(
                "transaction_id", "TXN-1",
                "amount", 250.0,
                "content", "ORDER_12345"
        ));

        ArgumentCaptor<Transaction> txCaptor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository).save(txCaptor.capture());
        Transaction savedTx = txCaptor.getValue();
        assertEquals(order, savedTx.getOrder());
        assertEquals(250.0, savedTx.getAmount());
        assertEquals(TypeTransactionEnum.REVENUE, savedTx.getType());
        assertNotNull(savedTx.getTransactionTime());

        verify(orderRepository).save(order);
        assertEquals(StatusEnum.COMPLETED, order.getStatus());
        assertNotNull(order.getPaidAt());
        verify(eventPublisher).publishEvent(any(OrderCompletedEvent.class));
    }

    @Test
    void processWebhook_AlreadyCompletedOrderSkipsTransactionCreation() {
        Order order = new Order();
        order.setId(1);
        order.setCode("ORDER_12345");
        order.setStatus(StatusEnum.COMPLETED);
        order.setTotalAmount(250.0);

        when(orderRepository.findByCodeWithLock("ORDER_12345")).thenReturn(Optional.of(order));

        sePayService.processWebhook(Map.of(
                "transaction_id", "TXN-1",
                "amount", 250.0,
                "content", "ORDER_12345"
        ));

        verify(transactionRepository, never()).save(any(Transaction.class));
        verify(orderRepository, never()).save(any(Order.class));
        verify(eventPublisher, never()).publishEvent(any(OrderCompletedEvent.class));
    }

    @Test
    void processWebhook_OrderNotFoundReturnsWithoutWrite() {
        when(orderRepository.findByCodeWithLock("ORDER_12345")).thenReturn(Optional.empty());

        sePayService.processWebhook(Map.of(
                "transaction_id", "TXN-1",
                "amount", 250.0,
                "content", "ORDER_12345"
        ));

        verify(transactionRepository, never()).save(any(Transaction.class));
        verify(orderRepository, never()).save(any(Order.class));
        verify(eventPublisher, never()).publishEvent(any(OrderCompletedEvent.class));
    }
}
