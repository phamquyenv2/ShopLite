package com.quyen.shoplite.service.payment;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.quyen.shoplite.domain.Order;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class SepayProvider implements PaymentProvider {

    @Value("${shoplite.sepay.bank-account:}")
    private String bankAccount;

    @Value("${shoplite.sepay.bank-code:}")
    private String bankCode;

    @Value("${shoplite.sepay.account-name:}")
    private String accountName;

    @Value("${shoplite.sepay.webhook-secret:}")
    private String webhookSecret;

    @Value("${shoplite.sepay.qr-expire-minutes:15}")
    private Integer qrExpireMinutes;

    @Override
    public Map<String, Object> createPayment(Order order, Double amount) {
        String transferContent = "SL" + order.getId(); // E.g., SL123
        
        // Generate SePay VietQR string
        String qrUrl = String.format("https://qr.sepay.vn/img?acc=%s&bank=%s&amount=%s&des=%s",
                URLEncoder.encode(bankAccount, StandardCharsets.UTF_8),
                URLEncoder.encode(bankCode, StandardCharsets.UTF_8),
                amount.longValue(),
                URLEncoder.encode(transferContent, StandardCharsets.UTF_8));
        
        if (accountName != null && !accountName.isBlank()) {
            qrUrl += "&name=" + URLEncoder.encode(accountName, StandardCharsets.UTF_8);
        }

        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(qrExpireMinutes);

        Map<String, Object> result = new HashMap<>();
        result.put("provider", "SEPAY_QR");
        result.put("qrUrl", qrUrl);
        result.put("transferContent", transferContent);
        result.put("expiresAt", expiresAt);
        result.put("amount", amount);
        
        return result;
    }

    @Override
    public boolean verifyWebhook(Map<String, String> headers, String rawPayload) {
        // Implement SePay signature verification if they provide it in `headers`
        // Usually, in Bankhub/SePay, there is an `x-sepay-signature` or `Authorization` header
        // For simplicity, if webhookSecret is configured, we can check a simple custom token or let it pass for now.
        // Let's assume standard token header check for SePay.
        
        if (webhookSecret == null || webhookSecret.isBlank()) {
            return true; // No secret configured, bypass
        }
        
        // SePay sometimes sends Authorization: Apikey <your-api-key>
        String authorization = headers.getOrDefault("authorization", "");
        if (authorization.toLowerCase().startsWith("apikey ")) {
            String token = authorization.substring(7);
            return webhookSecret.equals(token);
        }
        
        return false;
    }

    @Override
    public TransactionResult parseWebhook(Map<String, Object> payload) {
        log.info("Parsing SePay Webhook: {}", payload);
        
        if (!payload.containsKey("transactionAmount") || !payload.containsKey("content") || !payload.containsKey("referenceCode")) {
            // SePay payload structure might differ, checking fallback keys if it's the old API vs new API.
            // SePay webhook JSON typically has:
            // "id": 1234, "gateway": "MB", "transactionDate": "2023...", "accountNumber": "...", 
            // "code": "...", "content": "HD1234", "transferType": "in", "transferAmount": 100000, ...
            // Let's assume the new standard JSON from SePay:
            log.warn("Payload missing expected keys, trying alternative extraction");
        }
        
        String transferType = String.valueOf(payload.getOrDefault("transferType", "in"));
        if (!"in".equalsIgnoreCase(transferType)) {
            return TransactionResult.builder()
                .success(false)
                .errorMessage("Not an incoming transaction")
                .build();
        }

        Double amount = null;
        Object amountObj = payload.get("transferAmount");
        if(amountObj != null) {
             amount = Double.valueOf(amountObj.toString());
        }

        String content = String.valueOf(payload.getOrDefault("content", ""));
        String transactionId = String.valueOf(payload.getOrDefault("id", payload.get("referenceCode")));

        String orderIdStr = extractOrderId(content);
        
        return TransactionResult.builder()
                .success(true)
                .orderCode(orderIdStr) // actually returning string ID parsed
                .amount(amount)
                .transactionId(transactionId)
                .provider("SEPAY_QR")
                .build();
    }

    private String extractOrderId(String content) {
        if (content == null) return null;
        // Looking for SL123 or sl123
        Pattern pattern = Pattern.compile("SL(\\d+)", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(content);
        if (matcher.find()) {
            return matcher.group(1);
        }
        
        // Fallback for DH
        Pattern dhPattern = Pattern.compile("DH(\\d+)", Pattern.CASE_INSENSITIVE);
        Matcher dhMatcher = dhPattern.matcher(content);
        if (dhMatcher.find()) {
            return dhMatcher.group(1);
        }
        return content.trim();
    }
}
