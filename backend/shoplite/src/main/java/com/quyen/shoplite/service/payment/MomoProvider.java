package com.quyen.shoplite.service.payment;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.quyen.shoplite.domain.Order;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Formatter;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class MomoProvider implements PaymentProvider {

    @Value("${shoplite.momo.partnerCode:MOMO}")
    private String partnerCode;

    @Value("${shoplite.momo.accessKey:}")
    private String accessKey;

    @Value("${shoplite.momo.secretKey:}")
    private String secretKey;

    @Value("${shoplite.momo.endpoint:https://test-payment.momo.vn/v2/gateway/api/create}")
    private String endpoint;

    @Value("${shoplite.momo.returnUrl:}")
    private String returnUrl;

    @Value("${shoplite.momo.notifyUrl:}")
    private String notifyUrl;

    private final ObjectMapper objectMapper;

    @Override
    public Map<String, Object> createPayment(Order order, Double amount) {
        String requestId = String.valueOf(System.currentTimeMillis());
        String orderId = "SL_" + order.getId() + "_" + requestId;
        String orderInfo = "Thanh toan don hang " + order.getCode();
        String requestType = "captureWallet";
        String extraData = "";
        long amountLong = amount.longValue();

        String rawSignature = "accessKey=" + accessKey +
                "&amount=" + amountLong +
                "&extraData=" + extraData +
                "&ipnUrl=" + notifyUrl +
                "&orderId=" + orderId +
                "&orderInfo=" + orderInfo +
                "&partnerCode=" + partnerCode +
                "&redirectUrl=" + returnUrl +
                "&requestId=" + requestId +
                "&requestType=" + requestType;

        String signature = hmacSHA256(rawSignature, secretKey);

        Map<String, Object> request = new HashMap<>();
        request.put("partnerCode", partnerCode);
        request.put("partnerName", "ShopLite");
        request.put("storeId", "ShopLiteStore");
        request.put("requestId", requestId);
        request.put("amount", amountLong);
        request.put("orderId", orderId);
        request.put("orderInfo", orderInfo);
        request.put("redirectUrl", returnUrl);
        request.put("ipnUrl", notifyUrl);
        request.put("lang", "vi");
        request.put("extraData", extraData);
        request.put("requestType", requestType);
        request.put("signature", signature);

        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);

        try {
            Map<String, Object> response = restTemplate.postForObject(endpoint, entity, Map.class);
            log.info("Momo response: {}", response);

            if (response != null && Integer.valueOf(0).equals(response.get("resultCode"))) {
                Map<String, Object> result = new HashMap<>();
                result.put("provider", "MOMO");
                // Redirect to payUrl for MoMo gateway
                String qrUrl = (String) response.getOrDefault("qrCodeUrl", response.get("payUrl"));
                
                // If it's a deeplink string (momo://...), use a free QR generator to create an image URL
                if (qrUrl != null && qrUrl.startsWith("momo://")) {
                    try {
                        qrUrl = "https://api.qrserver.com/v1/create-qr-code/?size=300x300&data=" + java.net.URLEncoder.encode(qrUrl, StandardCharsets.UTF_8.toString());
                    } catch (Exception e) {
                        log.warn("Failed to encode MoMo deep link", e);
                    }
                }
                
                result.put("qrUrl", qrUrl);
                result.put("transferContent", orderId);
                result.put("expiresAt", LocalDateTime.now().plusMinutes(15));
                result.put("amount", amount);
                return result;
            } else {
                log.error("Failed to create MoMo payment: {}", response);
                throw new RuntimeException("Lỗi từ cổng thanh toán MoMo: " + (response != null ? response.get("message") : "No response"));
            }
        } catch (Exception e) {
            log.error("Exception calling MoMo API", e);
            throw new RuntimeException("Lỗi kết nối tới MoMo", e);
        }
    }

    @Override
    public boolean verifyWebhook(Map<String, String> headers, String rawPayload) {
        try {
            Map<String, Object> payload = objectMapper.readValue(rawPayload, new TypeReference<>() {});

            String amount = getValue(payload, "amount");
            String extraData = getValue(payload, "extraData");
            String message = getValue(payload, "message");
            String orderId = getValue(payload, "orderId");
            String orderInfo = getValue(payload, "orderInfo");
            String orderType = getValue(payload, "orderType");
            String partnerCodePayload = getValue(payload, "partnerCode");
            String payType = getValue(payload, "payType");
            String requestId = getValue(payload, "requestId");
            String responseTime = getValue(payload, "responseTime");
            String resultCode = getValue(payload, "resultCode");
            String transId = getValue(payload, "transId");
            String signature = getValue(payload, "signature");

            if (signature.isBlank()) {
                log.warn("MoMo webhook missing signature. Payload={}", payload);
                return false;
            }

            String rawSignature = "accessKey=" + accessKey +
                    "&amount=" + amount +
                    "&extraData=" + extraData +
                    "&message=" + message +
                    "&orderId=" + orderId +
                    "&orderInfo=" + orderInfo +
                    "&orderType=" + orderType +
                    "&partnerCode=" + partnerCodePayload +
                    "&payType=" + payType +
                    "&requestId=" + requestId +
                    "&responseTime=" + responseTime +
                    "&resultCode=" + resultCode +
                    "&transId=" + transId;

            String expectedSignature = hmacSHA256(rawSignature, secretKey);
            boolean matched = expectedSignature.equalsIgnoreCase(signature);
            if (!matched) {
                log.warn("MoMo webhook signature mismatch. expected={}, got={}, orderId={}, requestId={}",
                        expectedSignature, signature, orderId, requestId);
            }
            return matched;
        } catch (Exception e) {
            log.error("Error verifying MoMo webhook signature", e);
            return false;
        }
    }

    @Override
    public TransactionResult parseWebhook(Map<String, Object> payload) {
        log.info("Parsing MoMo Webhook: {}", payload);

        Integer resultCode = Integer.valueOf(String.valueOf(payload.get("resultCode")));
        if (resultCode != 0) {
            return TransactionResult.builder()
                    .success(false)
                    .errorMessage(String.valueOf(payload.get("message")))
                    .build();
        }

        String orderIdStr = String.valueOf(payload.get("orderId"));
        // Extract real orderId from format "SL_123_timestamp"
        String realOrderId = null;
        if (orderIdStr.startsWith("SL_")) {
            String[] parts = orderIdStr.split("_");
            if (parts.length >= 2) {
                realOrderId = parts[1];
            }
        }
        if (realOrderId == null || realOrderId.isBlank()) {
            realOrderId = orderIdStr;
        }

        Double amount = Double.valueOf(String.valueOf(payload.get("amount")));
        String transactionId = String.valueOf(payload.get("transId"));

        return TransactionResult.builder()
                .success(true)
                .orderCode(realOrderId)
                .amount(amount)
                .transactionId(transactionId)
                .provider("MOMO")
                .build();
    }

    private String getValue(Map<String, Object> payload, String key) {
        Object v = payload.get(key);
        return v == null ? "" : String.valueOf(v);
    }

    private String hmacSHA256(String data, String secret) {
        try {
            Mac sha256_HMAC = Mac.getInstance("HmacSHA256");
            SecretKeySpec secret_key = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            sha256_HMAC.init(secret_key);
            byte[] hash = sha256_HMAC.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(hash);
        } catch (Exception e) {
            throw new RuntimeException("Failed to calculate HMAC SHA256", e);
        }
    }

    private String bytesToHex(byte[] bytes) {
        Formatter formatter = new Formatter();
        for (byte b : bytes) {
            formatter.format("%02x", b);
        }
        String result = formatter.toString();
        formatter.close();
        return result;
    }
}
