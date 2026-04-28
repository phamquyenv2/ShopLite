package com.quyen.shoplite.service.payment;

import com.quyen.shoplite.domain.Order;
import java.util.Map;

public interface PaymentProvider {
    /**
     * Tạo thông tin thanh toán (VD: QR code, link thanh toán)
     * @param order Đơn hàng cần thanh toán
     * @param amount Số tiền (nếu khác với tổng đơn)
     * @return Map chứa thông tin như qrUrl, transferContent, expiresAt, etc.
     */
    Map<String, Object> createPayment(Order order, Double amount);

    /**
     * Xác thực tính hợp lệ của webhook (chữ ký, header...)
     * @param headers Các header truyền lên
     * @param rawPayload Nội dung raw của webhook
     * @return true nếu webhook hợp lệ
     */
    boolean verifyWebhook(Map<String, String> headers, String rawPayload);

    /**
     * Trích xuất thông tin giao dịch từ webhook, phân tích nó thành một format chuẩn để xử lý.
     * @param payload Dữ liệu webhook dưới dạng Map hoặc Object tuỳ vào ngôn ngữ
     * @return Thông tin giao dịch chuẩn hoá (ví dụ: chứa orderId, trạng thái thành công, số tiền...)
     */
    TransactionResult parseWebhook(Map<String, Object> payload);
}
