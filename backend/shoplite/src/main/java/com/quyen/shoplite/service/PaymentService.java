package com.quyen.shoplite.service;

import com.quyen.shoplite.domain.Order;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final TransactionService transactionService;

    @Transactional
    public ResPaymentDTO createPayment(Integer orderId, ReqPaymentDTO req) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IdInvalidException("Không tìm thấy Order id=" + orderId));

        Optional<Payment> existingPayment = paymentRepository.findByOrder_Id(orderId);
        if (existingPayment.isPresent()) {
            throw new IdInvalidException("Đơn hàng đã có giao dịch thanh toán");
        }

        Payment payment = Payment.builder()
                .order(order)
                .method(req.getMethod())
                .amount(req.getAmount())
                .status(req.getStatus() != null ? req.getStatus() : StatusEnum.COMPLETED)
                .createdAt(LocalDateTime.now())
                .build();
        
        Payment savedPayment = paymentRepository.save(payment);

        if (savedPayment.getStatus() == StatusEnum.COMPLETED) {
            ReqTransactionDTO transactionDTO = new ReqTransactionDTO();
            transactionDTO.setOrderId(orderId);
            transactionDTO.setAmount(savedPayment.getAmount());
            transactionDTO.setType(TypeTransactionEnum.REVENUE);
            transactionDTO.setContent("Thanh toán cho đơn hàng " + order.getCode());
            transactionDTO.setTransactionTime(LocalDateTime.now());
            transactionService.create(transactionDTO);
            
            order.setPaidAt(LocalDateTime.now());
            order.setStatus(StatusEnum.COMPLETED);
            orderRepository.save(order);
        }

        return DTOMapper.toResPaymentDTO(savedPayment);
    }

    public ResPaymentDTO findByOrderId(Integer orderId) {
        Payment payment = paymentRepository.findByOrder_Id(orderId)
                .orElseThrow(() -> new IdInvalidException("Không tìm thấy thanh toán cho Order id=" + orderId));
        return DTOMapper.toResPaymentDTO(payment);
    }
}
