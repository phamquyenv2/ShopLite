package com.quyen.shoplite.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.quyen.shoplite.repository.OrderRepository;
import com.quyen.shoplite.repository.PaymentRepository;
import com.quyen.shoplite.service.payment.PaymentProvider;
import com.quyen.shoplite.service.payment.PaymentProviderFactory;
import com.quyen.shoplite.service.payment.TransactionResult;
import com.quyen.shoplite.util.DTOMapper;
import com.quyen.shoplite.util.constant.*;
import com.quyen.shoplite.util.error.IdInvalidException;

import com.quyen.shoplite.domain.FundAccount;
import com.quyen.shoplite.domain.Order;
import com.quyen.shoplite.event.OrderCompletedEvent;
import com.quyen.shoplite.domain.Payment;
import com.quyen.shoplite.domain.Store;
import com.quyen.shoplite.domain.request.ReqPaymentDTO;
import com.quyen.shoplite.domain.request.ReqTransactionDTO;
import com.quyen.shoplite.domain.response.ResPaymentDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.quyen.shoplite.repository.FundAccountRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final FundAccountRepository fundAccountRepository;
    private final TransactionService transactionService;
    private final ApplicationEventPublisher eventPublisher;
    private final PaymentProviderFactory paymentProviderFactory;
    private final ObjectMapper objectMapper;
    private final CurrentStoreService currentStoreService;

    /**
     * Tạo session thanh toán (thủ công hoặc qua cổng thanh toán).
     * Giờ sử dụng polymorphic reference (referenceType + referenceId).
     */
    /**
     * Overload: gọi từ OrderController với orderId từ path variable.
     * Tự động gán referenceType = ORDER, referenceId = orderId.
     */
    @Transactional
    public ResPaymentDTO createPaymentSession(Integer orderId, ReqPaymentDTO req) {
        req.setReferenceType(RefTypeEnum.ORDER);
        req.setReferenceId(orderId);
        return createPaymentSession(req);
    }

    @Transactional
    public ResPaymentDTO createPaymentSession(ReqPaymentDTO req) {
        Store store = currentStoreService.getCurrentStore();
        Long storeId = store.getId();
        // Validate: chưa có payment COMPLETED cho reference này, nếu có PENDING thì CANCEL
        List<Payment> existingPayments = paymentRepository.findAllByStoreIdAndReferenceTypeAndReferenceId(
                storeId, req.getReferenceType(), req.getReferenceId());
        
        for (Payment ep : existingPayments) {
            if (ep.getStatus() == PaymentStatusEnum.COMPLETED) {
                throw new IdInvalidException("Đơn hàng này đã được thanh toán hoàn tất.");
            }
            if (ep.getStatus() == PaymentStatusEnum.PENDING) {
                ep.setStatus(PaymentStatusEnum.CANCELLED);
                paymentRepository.save(ep);
            }
        }

        PaymentProvider provider = paymentProviderFactory.getProvider(req.getPaymentMethod());

        if (provider != null) {
            // Flow qua cổng thanh toán (VD: SePay/MoMo)
            // Cần resolve entity để pass vào provider (chỉ hỗ trợ ORDER hiện tại)
            Order order = null;
            if (req.getReferenceType() == RefTypeEnum.ORDER) {
                order = orderRepository.findByIdAndStoreId(req.getReferenceId(), storeId)
                        .orElseThrow(() -> new IdInvalidException("Không tìm thấy Order id=" + req.getReferenceId()));
            }

            Map<String, Object> paymentData = provider.createPayment(order, req.getAmount().doubleValue());

            Payment payment = Payment.builder()
                    .store(store)
                    .referenceType(req.getReferenceType())
                    .referenceId(req.getReferenceId())
                    .paymentMethod(req.getPaymentMethod())
                    .amount(req.getAmount())
                    .status(PaymentStatusEnum.PENDING)
                    .provider((String) paymentData.get("provider"))
                    .qrUrl((String) paymentData.get("qrUrl"))
                    .transferContent((String) paymentData.get("transferContent"))
                    .expiresAt((LocalDateTime) paymentData.get("expiresAt"))
                    .createdBy(req.getCreatedBy())
                    .build();

            try {
                paymentRepository.save(payment);
            } catch (DataIntegrityViolationException e) {
                throw new IdInvalidException("Đã có giao dịch thanh toán cho đối tượng này");
            }

            return DTOMapper.toResPaymentDTO(payment);
        } else {
            // Flow thanh toán thủ công (CASH, BANK_TRANSFER manual)
            Payment payment = Payment.builder()
                    .store(store)
                    .referenceType(req.getReferenceType())
                    .referenceId(req.getReferenceId())
                    .paymentMethod(req.getPaymentMethod())
                    .amount(req.getAmount())
                    .status(PaymentStatusEnum.COMPLETED)
                    .paidAt(LocalDateTime.now())
                    .createdBy(req.getCreatedBy())
                    .build();

            try {
                paymentRepository.save(payment);
            } catch (DataIntegrityViolationException e) {
                throw new IdInvalidException("Dã có giao dịch thanh toán cho đối tượng này");
            }

            // Xác định direction dựa trên referenceType
            DirectionEnum direction = resolveDirection(req.getReferenceType());

            // Tự resolve fundAccountId nếu không được truyền
            Integer fundAccountId = req.getFundAccountId() != null
                    ? req.getFundAccountId()
                    : resolveFundAccountId(req.getPaymentMethod());
            fundAccountRepository.findByIdAndStoreId(fundAccountId, storeId)
                    .orElseThrow(() -> new IdInvalidException("FundAccount is not in the current store"));

            // Tạo Transaction (cập nhật số dư FundAccount)
            createTransactionForPayment(payment, fundAccountId, direction,
                    "Thanh toán thủ công cho " + req.getReferenceType() + " #" + req.getReferenceId());

            // Nếu là ORDER → update order status
            if (req.getReferenceType() == RefTypeEnum.ORDER) {
                updateOrderOnPaymentCompleted(req.getReferenceId(), payment);
            }

            return DTOMapper.toResPaymentDTO(payment);
        }
    }

    /**
     * Xử lý Webhook (chỉ áp dụng cho ORDER hiện tại)
     */
    @Transactional
    public void processWebhook(PaymentMethodEnum method, Map<String, String> headers, Map<String, Object> payload) {
        log.info("Received Webhook for method {}: {}", method, payload);
        PaymentProvider provider = paymentProviderFactory.getProvider(method);
        if (provider == null) {
            log.error("No provider found for {}", method);
            return;
        }

        String rawPayload = "";
        try {
            rawPayload = objectMapper.writeValueAsString(payload);
        } catch (Exception e) {}

        if (!provider.verifyWebhook(headers, rawPayload)) {
            log.error("Invalid webhook signature for method {}", method);
            throw new IllegalArgumentException("Invalid webhook signature");
        }

        TransactionResult result = provider.parseWebhook(payload);
        if (!result.isSuccess()) {
            log.error("Transaction not successful: {}", result.getErrorMessage());
            return;
        }

        String orderIdStr = result.getOrderCode();
        if (orderIdStr == null || orderIdStr.isBlank()) {
            log.error("Cannot extract order id from webhook");
            return;
        }

        Integer orderId;
        try {
            orderId = Integer.parseInt(orderIdStr);
        } catch (NumberFormatException e) {
            log.error("Order format invalid: {}", orderIdStr);
            return;
        }

        Order order = orderRepository.findById(orderId).orElse(null);
        if (order == null) {
            log.error("Order not found: {}", orderId);
            return;
        }

        // Tìm payment qua polymorphic reference
        Payment payment = paymentRepository.findFirstByStoreIdAndReferenceTypeAndReferenceIdOrderByIdDesc(
                order.getStore().getId(), RefTypeEnum.ORDER, orderId).orElse(null);
        if (payment == null) {
            log.error("Payment not found for order {}", orderId);
            return;
        }

        // Idempotent check
        if (payment.getStatus() == PaymentStatusEnum.COMPLETED) {
            log.info("Payment already COMPLETED, skipping");
            return;
        }

        if (payment.getProviderTransactionId() != null && payment.getProviderTransactionId().equals(result.getTransactionId())) {
            log.info("Duplicate transaction log, skipping");
            return;
        }

        if (payment.getAmount().subtract(BigDecimal.valueOf(result.getAmount())).abs()
                .compareTo(BigDecimal.valueOf(0.01)) > 0) {
            log.error("Webhook amount mismatch. Expected {}, got {}", payment.getAmount(), result.getAmount());
            return;
        }

        // Update Payment
        payment.setStatus(PaymentStatusEnum.COMPLETED);
        payment.setPaidAt(LocalDateTime.now());
        payment.setProviderTransactionId(result.getTransactionId());
        payment.setRawWebhookPayload(rawPayload);
        paymentRepository.save(payment);

        // TODO: Khi webhook, fundAccountId cần được xác định từ config/default fund
        // Tạm thời không tạo Transaction cho webhook — cần thêm config fund mặc định
        // createTransactionForPayment(payment, defaultFundId, DirectionEnum.IN, "...");

        // Update Order
        updateOrderOnPaymentCompleted(orderId, payment);
        log.info("Successfully processed webhook for order {}", orderId);
    }

    /**
     * Tìm payment theo reference.
     */
    public ResPaymentDTO findByReference(RefTypeEnum referenceType, Integer referenceId) {
        Long storeId = currentStoreService.getCurrentStoreId();
        Payment payment = paymentRepository.findFirstByStoreIdAndReferenceTypeAndReferenceIdOrderByIdDesc(storeId, referenceType, referenceId)
                .orElseThrow(() -> new IdInvalidException(
                        "Không tìm thấy thanh toán cho " + referenceType + " id=" + referenceId));
        return DTOMapper.toResPaymentDTO(payment);
    }

    /**
     * Tiện ích: tìm payment của một Order theo orderId.
     */
    public ResPaymentDTO findByOrderId(Integer orderId) {
        return findByReference(RefTypeEnum.ORDER, orderId);
    }

    // ---- Private helpers ----

    private void createTransactionForPayment(Payment payment, Integer fundAccountId,
                                             DirectionEnum direction, String content) {
        ReqTransactionDTO txReq = new ReqTransactionDTO();
        txReq.setPaymentId(payment.getId());
        txReq.setAmount(payment.getAmount());
        txReq.setType(resolveTransactionType(payment.getReferenceType()));
        txReq.setDirection(direction);
        txReq.setFundAccountId(fundAccountId);
        txReq.setContent(content);
        txReq.setTransactionTime(LocalDateTime.now());
        transactionService.create(txReq);
    }

    private void updateOrderOnPaymentCompleted(Integer orderId, Payment payment) {
        Order order = orderRepository.findByIdAndStoreId(orderId, payment.getStore().getId()).orElse(null);
        if (order != null) {
            order.setPaidAt(LocalDateTime.now());
            order.setStatus(StatusEnum.COMPLETED);
            orderRepository.save(order);
            eventPublisher.publishEvent(new OrderCompletedEvent(order));
        }
    }

    /**
     * Xác định direction dựa trên loại nguồn:
     * - ORDER → IN (tiền vào)
     * - IMPORT_ORDER, PAYROLL → OUT (tiền ra)
     * - SUPPLIER_RETURN → IN (NCC trả tiền lại)
     * - MANUAL → IN mặc định (có thể override)
     */
    private DirectionEnum resolveDirection(RefTypeEnum refType) {
        return switch (refType) {
            case ORDER, SUPPLIER_RETURN -> DirectionEnum.IN;
            case IMPORT_ORDER, PAYROLL -> DirectionEnum.OUT;
            case MANUAL -> DirectionEnum.IN;
        };
    }

    /**
     * Map từ RefTypeEnum sang TypeTransactionEnum.
     */
    private TypeTransactionEnum resolveTransactionType(RefTypeEnum refType) {
        return switch (refType) {
            case ORDER -> TypeTransactionEnum.REVENUE;
            case IMPORT_ORDER -> TypeTransactionEnum.EXPENSE;
            case PAYROLL -> TypeTransactionEnum.SALARY;
            case SUPPLIER_RETURN -> TypeTransactionEnum.REFUND;
            case MANUAL -> TypeTransactionEnum.ADJUSTMENT;
        };
    }

    /**
     * Tự resolve fundAccountId dựa trên phương thức thanh toán:
     * - CASH              → quỹ CASH
     * - BANK_TRANSFER, BANK_QR, SEPAY, VNPAY, COD → quỹ BANK
     * - EWALLET (MoMo...) → quỹ EWALLET
     */
    private Integer resolveFundAccountId(PaymentMethodEnum method) {
        FundTypeEnum targetType = switch (method) {
            case CASH -> FundTypeEnum.CASH;
            case BANK_TRANSFER, BANK_QR -> FundTypeEnum.BANK;
            case EWALLET -> FundTypeEnum.EWALLET;
        };
        Long storeId = currentStoreService.getCurrentStoreId();
        FundAccount account = fundAccountRepository.findFirstByStoreIdAndTypeAndIsActiveTrue(storeId, targetType)
                .orElseThrow(() -> new IdInvalidException(
                        "Chưa có tài khoản quỹ loại " + targetType + " nào được khởi tạo. " +
                        "Vui lòng tạo tài khoản quỹ trong phần cài đặt trước khi thanh toán."));
        return account.getId();
    }
}
