package com.quyen.shoplite.service;

import com.quyen.shoplite.domain.Order;
import com.quyen.shoplite.domain.OrderCompletedEvent;
import com.quyen.shoplite.domain.Payment;
import com.quyen.shoplite.domain.request.ReqPaymentDTO;
import com.quyen.shoplite.domain.request.ReqTransactionDTO;
import com.quyen.shoplite.domain.response.ResPaymentDTO;
import com.quyen.shoplite.repository.OrderRepository;
import com.quyen.shoplite.repository.PaymentRepository;
import com.quyen.shoplite.util.DTOMapper;
import com.quyen.shoplite.util.constant.StatusEnum;
import com.quyen.shoplite.util.constant.TypeTransactionEnum;
import com.quyen.shoplite.util.error.IdInvalidException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final TransactionService transactionService;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * Tạo thanh toán thủ công (tiền mặt / chuyển khoản thủ công).
     * Chỉ cho phép trên đơn PENDING_PAYMENT.
     */
    @Transactional
    public ResPaymentDTO createPayment(Integer orderId, ReqPaymentDTO req) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IdInvalidException("Không tìm thấy Order id=" + orderId));

        // Only allow payment on PENDING_PAYMENT orders
        if (order.getStatus() != StatusEnum.PENDING_PAYMENT) {
            throw new IdInvalidException(
                    "Chỉ có thể thanh toán cho đơn hàng ở trạng thái PENDING_PAYMENT (hiện tại: " + order.getStatus() + ")");
        }

        // Application-level duplicate check (fast path)
        Optional<Payment> existingPayment = paymentRepository.findByOrder_Id(orderId);
        if (existingPayment.isPresent()) {
            throw new IdInvalidException("Đơn hàng đã có giao dịch thanh toán");
        }

        // Validate payment amount matches order total
        if (Math.abs(req.getAmount() - order.getTotalAmount()) > 0.01) {
            throw new IdInvalidException(
                    "Số tiền thanh toán (" + req.getAmount()
                    + ") không khớp với tổng đơn hàng (" + order.getTotalAmount() + ")");
        }

        Payment payment = Payment.builder()
                .order(order)
                .method(req.getMethod())
                .amount(req.getAmount())
                .status(StatusEnum.COMPLETED)
                .createdAt(LocalDateTime.now())
                .build();

        // DB unique constraint guard (race condition)
        Payment savedPayment;
        try {
            savedPayment = paymentRepository.save(payment);
        } catch (DataIntegrityViolationException e) {
            log.warn("[Payment] Duplicate payment detected for order id={} (concurrent request)", orderId);
            throw new IdInvalidException("Đơn hàng đã có giao dịch thanh toán");
        }

        // Create REVENUE transaction in cashbook
        ReqTransactionDTO transactionDTO = new ReqTransactionDTO();
        transactionDTO.setOrderId(orderId);
        transactionDTO.setAmount(savedPayment.getAmount());
        transactionDTO.setType(TypeTransactionEnum.REVENUE);
        transactionDTO.setContent("Thanh toán cho đơn hàng " + order.getCode());
        transactionDTO.setTransactionTime(LocalDateTime.now());
        transactionService.create(transactionDTO);

        // Update order to COMPLETED
        order.setPaidAt(LocalDateTime.now());
        order.setStatus(StatusEnum.COMPLETED);
        orderRepository.save(order);

        // Publish event — FCM notification fires AFTER DB commit
        eventPublisher.publishEvent(new OrderCompletedEvent(order));

        log.info("[Payment] Order id={} paid via {} amount={}", orderId, req.getMethod(), req.getAmount());
        return DTOMapper.toResPaymentDTO(savedPayment);
    }

    public ResPaymentDTO findByOrderId(Integer orderId) {
        Payment payment = paymentRepository.findByOrder_Id(orderId)
                .orElseThrow(() -> new IdInvalidException("Không tìm thấy thanh toán cho Order id=" + orderId));
        return DTOMapper.toResPaymentDTO(payment);
    }
}
