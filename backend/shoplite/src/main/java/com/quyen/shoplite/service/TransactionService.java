package com.quyen.shoplite.service;

import com.quyen.shoplite.repository.FundAccountRepository;
import com.quyen.shoplite.repository.PaymentRepository;
import com.quyen.shoplite.repository.TransactionRepository;
import com.quyen.shoplite.util.DTOMapper;
import com.quyen.shoplite.util.constant.DirectionEnum;
import com.quyen.shoplite.util.error.IdInvalidException;

import com.quyen.shoplite.domain.FundAccount;
import com.quyen.shoplite.domain.Payment;
import com.quyen.shoplite.domain.Transaction;
import com.quyen.shoplite.domain.request.ReqTransactionDTO;
import com.quyen.shoplite.domain.response.ResTransactionDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final FundAccountRepository fundAccountRepository;
    private final PaymentRepository paymentRepository;
    private final CurrentStoreService currentStoreService;

    private static final DateTimeFormatter CODE_DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");

    /**
     * Tạo Transaction — cập nhật balance FundAccount trong cùng 1 DB transaction.
     * FundAccount sẽ bị pessimistic lock để đảm bảo consistency.
     */
    @Transactional
    public ResTransactionDTO create(ReqTransactionDTO req) {
        Long storeId = currentStoreService.getCurrentStoreId();
        // Validate amount
        if (req.getAmount() == null || req.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IdInvalidException("Số tiền giao dịch phải lớn hơn 0");
        }

        // Lock fund account
        FundAccount fundAccount = fundAccountRepository.findByIdAndStoreIdWithLock(req.getFundAccountId(), storeId)
                .orElseThrow(() -> new IdInvalidException(
                        "Không tìm thấy FundAccount id=" + req.getFundAccountId()));

        if (!Boolean.TRUE.equals(fundAccount.getIsActive())) {
            throw new IdInvalidException("FundAccount id=" + req.getFundAccountId() + " không hoạt động");
        }

        // Resolve optional payment
        Payment payment = null;
        if (req.getPaymentId() != null) {
            payment = paymentRepository.findById(req.getPaymentId())
                    .orElseThrow(() -> new IdInvalidException(
                            "Không tìm thấy Payment id=" + req.getPaymentId()));
            if (!payment.getStore().getId().equals(storeId)) {
                throw new IdInvalidException("Payment is not in the current store");
            }
        }

        // Calculate balance
        BigDecimal balanceBefore = fundAccount.getBalance();
        BigDecimal balanceAfter;

        if (req.getDirection() == DirectionEnum.IN) {
            balanceAfter = balanceBefore.add(req.getAmount());
        } else {
            balanceAfter = balanceBefore.subtract(req.getAmount());
            if (balanceAfter.compareTo(BigDecimal.ZERO) < 0) {
                throw new IdInvalidException("Số dư quỹ không đủ để thực hiện giao dịch (hiện tại: "
                        + balanceBefore + ", cần chi: " + req.getAmount() + ")");
            }
        }

        // Generate transaction code
        String transactionCode = generateTransactionCode();

        // Build & save Transaction
        Transaction transaction = Transaction.builder()
                .store(fundAccount.getStore())
                .type(req.getType())
                .direction(req.getDirection())
                .amount(req.getAmount())
                .content(req.getContent())
                .payment(payment)
                .fundAccount(fundAccount)
                .balanceBefore(balanceBefore)
                .balanceAfter(balanceAfter)
                .transactionCode(transactionCode)
                .transactionTime(req.getTransactionTime() != null ? req.getTransactionTime() : LocalDateTime.now())
                .build();

        Transaction saved = transactionRepository.save(transaction);

        // Update fund account balance
        fundAccount.setBalance(balanceAfter);
        fundAccountRepository.save(fundAccount);

        log.info("[Transaction] Created {} {} amount={} fund={} balance: {} -> {}",
                req.getDirection(), req.getType(), req.getAmount(),
                fundAccount.getName(), balanceBefore, balanceAfter);

        return DTOMapper.toResTransactionDTO(saved);
    }

    public ResTransactionDTO findById(Integer id) {
        Long storeId = currentStoreService.getCurrentStoreId();
        Transaction transaction = transactionRepository.findByIdAndStoreId(id, storeId)
                .orElseThrow(() -> new IdInvalidException("Không tìm thấy Transaction id=" + id));
        return DTOMapper.toResTransactionDTO(transaction);
    }

    public List<ResTransactionDTO> findAll() {
        Long storeId = currentStoreService.getCurrentStoreId();
        return transactionRepository.findAllByStoreIdOrderByTransactionTimeDesc(storeId).stream()
                .map(DTOMapper::toResTransactionDTO)
                .toList();
    }

    public List<ResTransactionDTO> findByFundAccountId(Integer fundAccountId) {
        Long storeId = currentStoreService.getCurrentStoreId();
        return transactionRepository.findAllByStoreIdAndFundAccount_Id(storeId, fundAccountId).stream()
                .map(DTOMapper::toResTransactionDTO)
                .toList();
    }

    public List<ResTransactionDTO> findByPaymentId(Integer paymentId) {
        Long storeId = currentStoreService.getCurrentStoreId();
        return transactionRepository.findAllByStoreIdAndPayment_Id(storeId, paymentId).stream()
                .map(DTOMapper::toResTransactionDTO)
                .toList();
    }

    /**
     * Generate unique transaction code: TXN-yyyyMMdd-NNNN
     * Dùng count từ DB + retry để tránh duplicate khi restart server hoặc concurrent requests.
     */
    private String generateTransactionCode() {
        String dateStr = LocalDate.now().format(CODE_DATE_FMT);
        String prefix = "TXN-" + dateStr + "-";
        // Lấy số records hiện có trong DB với prefix này
        long count = transactionRepository.countByTransactionCodeStartingWith(prefix);
        // Thử từ count+1 cho đến khi không bị trùng
        for (int attempt = 0; attempt < 100; attempt++) {
            String candidate = prefix + String.format("%04d", count + 1 + attempt);
            if (!transactionRepository.findByTransactionCode(candidate).isPresent()) {
                return candidate;
            }
        }
        // Fallback: thêm timestamp millis để đảm bảo unique tuyệt đối
        return prefix + System.currentTimeMillis();
    }
}
