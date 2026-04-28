package com.quyen.shoplite.service.payment;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class TransactionResult {
    private boolean success;          // Giao dịch thành công hay không
    private String orderCode;         // Extracted order ID/Code từ nội dung chuyển khoản
    private Double amount;            // Số tiền trong giao dịch
    private String transactionId;     // Mã giao dịch từ Provider
    private String provider;          // Tên provider (SEPAY...)
    private String errorMessage;      // Tin nhắn lỗi nếu có
}
