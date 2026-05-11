# Test Migration Scripts – Multi-Store Refactor

Các script trong thư mục này được dùng **một lần** để tự động refactor toàn bộ
`ServiceTest` sau khi chuyển sang kiến trúc **multi-store (multi-tenant)**.

## Bối cảnh

Sau khi migrate, mọi Repository method đều yêu cầu thêm `storeId` làm tham số,
và mọi Service đều phụ thuộc vào `CurrentStoreService`. Các script dưới đây đã
cập nhật hàng loạt test class để:

- Thêm `@Mock CurrentStoreService currentStoreService`  
- Stub `currentStoreService.getCurrentStoreId()` / `getCurrentStore()` trả về store id=1  
- Đổi tên repository method sang dạng `findByIdAndStoreId`, `existsByStoreIdAnd...`, v.v.  
- Dùng `findByIdAndStoreIdWithLock` cho các method có lock (ImportOrder, Product)  
- Dùng batch query (`findByImportOrder_IdIn`, `findAllByOrderIdIn`) thay per-row  

## Scripts

| File | Mục đích |
|------|----------|
| `fix_all_service_tests.py` | Refactor đợt 1: Unit, Supplier, Category, Product, Auth, Attendance, Employee, Order, Role |
| `fix_adj_v2.py` | Rewrite hoàn toàn `InventoryAdjustmentServiceTest` (logic phức tạp với locking) |

> **Lưu ý:** Các script này đã chạy xong. Chỉ giữ lại để tham khảo nếu cần refactor
> service mới trong tương lai theo cùng pattern.
