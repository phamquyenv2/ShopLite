---
description: "Hiện thực hệ thống sổ bán hàng chuyên nghiệp theo API hiện có (Spring Boot + Ionic React)"
name: "Implement sổ bán hàng (theo API)"
argument-hint: "Mục tiêu nghiệp vụ, vai trò người dùng, ưu tiên màn hình (vd: bán hàng POS trước), ràng buộc UX"
agent: "agent"
model: ["GPT-5 (copilot)"]
---

Bạn đang làm việc trong repo ShopLite (backend Spring Boot, frontend Ionic React).

Nhiệm vụ: hiện thực hệ thống **tương tự sổ bán hàng** một cách chuyên nghiệp nhất, **thực hiện đầy đủ các chức năng tương ứng với API hiện có trong repo**.

Input người dùng (ARGUMENTS):
- Mục tiêu nghiệp vụ + phạm vi
- Vai trò người dùng (admin/nhân viên/…)
- Ưu tiên (màn hình/chức năng nào làm trước)
- Bất kỳ ràng buộc UI/UX hoặc non-functional (offline? performance?…)

Ràng buộc:
- Không phát minh API mới. Nguồn sự thật là các controller backend (đặc biệt các route `/api/v1/*`).
- Nếu `docs/api/openapi.yaml` trống/không cập nhật, hãy **suy ra hợp đồng API** bằng cách đọc controller + DTO/response trong backend.
- Tôn trọng cấu trúc & style hiện có của dự án (không đổi kiến trúc tuỳ tiện, không hard-code theme mới).

Cách làm (bắt buộc):
1) **Khảo sát API**
   - Liệt kê tất cả base routes trong `backend/shoplite/src/main/java/com/quyen/shoplite/controller/*Controller.java`.
   - Với mỗi controller: trích ra endpoints (method, path, request/response DTO, status codes) và nhóm theo module nghiệp vụ.

2) **Định nghĩa phạm vi “sổ bán hàng”**
   - Từ API đã có, map thành các module UI tối thiểu để vận hành bán hàng:
     - Đăng nhập/đăng xuất/refresh token (`/api/v1/auth`)
     - Sản phẩm + danh mục + đơn vị + nhà cung cấp (products/categories/units/suppliers)
     - Khách hàng (customers)
     - Đơn hàng + thanh toán (orders + payment)
     - Giao dịch/thu-chi (transactions)
     - Tồn kho: import orders, inventory adjustments, inventory logs
     - Quyền/role (roles/permissions) nếu API có và cần cho luồng UX
   - Nêu rõ màn hình nào, thao tác CRUD nào, và điều kiện lỗi.

3) **Thiết kế luồng UI (Ionic React)**
   - Tạo/điều chỉnh pages trong `frontend/shopliteapp/src/pages/` và dùng lại patterns hiện có.
   - Dùng lớp gọi API tập trung (ưu tiên mở rộng `frontend/shopliteapp/src/utils/Apis.tsx`).
   - Chuẩn hoá xử lý lỗi theo format backend đang trả (xem `backend/.../util/error/GlobalException.java`).
   - Bảo đảm auth: lưu token, gắn Authorization header, và refresh khi cần.

4) **Triển khai theo lát (slice) có thể chạy được**
   - Ưu tiên: Auth → Products → Customers → Orders/Payments → Transactions → Inventory.
   - Mỗi lát phải:
     - Có UI list + detail + create/edit (nếu API hỗ trợ)
     - Có validation phía client khớp với backend
     - Có loading/error states rõ ràng

5) **Kiểm chứng**
   - Frontend: chạy lint/test hiện có (Jest/Cypress nếu repo đã cấu hình).
   - Backend: không được làm hỏng build (`gradlew compileJava`).

Output mong muốn (trả lời theo format này):
- **Scope**: danh sách module/màn hình sẽ làm + mapping tới controller/routes
- **Plan**: 5–10 bước theo thứ tự triển khai
- **Changes**: danh sách file sẽ tạo/sửa theo từng slice
- **Notes**: giả định, điểm cần xác nhận (nếu API mơ hồ), và cách chạy dev

Bắt đầu ngay bằng việc:
- Ingest ARGUMENTS người dùng
- Quét controllers để lập bảng endpoints
- Đề xuất slice #1 và thực thi trong codebase
