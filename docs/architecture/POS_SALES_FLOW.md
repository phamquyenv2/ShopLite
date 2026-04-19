# POS Sales Flow Analysis

## 1. Muc tieu

Tai lieu nay mo ta luong ban hang cua he thong hien tai theo goc nhin API, doi chieu voi mot POS pho bien nhu KiotViet, va de xuat huong refactor de dat duoc luong ban hang chuan.

Pham vi:
- Ban tai quay
- Tao don
- Tru ton kho
- Thu tien
- Huy don / hoan kho / hoan tien

Khong bao gom:
- Khuyen mai phuc tap
- Tra hang tung phan
- Doi hang
- Dong ca / so quy theo ca

## 2. Luong POS chuan kieu KiotViet

Mot POS chuan thuong tach thanh 3 pha:

1. Chon hang vao gio / hoa don tam
2. Xac nhan ban hang
3. Thu tien

Luong chi tiet:

1. Nhan vien mo man hinh ban hang
2. Tim san pham bang ten / SKU / barcode
3. Them san pham vao gio tam
4. Chon hoac tao nhanh khach hang
5. Ap dung chiet khau / gia dac biet / ghi chu
6. Xac nhan don
7. He thong kiem tra ton, khoa ton hoac tru ton
8. Thu tien bang tien mat, chuyen khoan, QR, hoac ket hop nhieu hinh thuc
9. In hoa don / dong bo so quy / gui thong bao
10. Neu huy sau khi xac nhan thi phai hoan kho va neu da thu tien thi sinh nghiep vu refund

## 3. Luong hien tai cua ShopLite

### 3.1 API lien quan den ban hang

- `GET /api/v1/products`
- `GET /api/v1/products/{id}`
- `GET /api/v1/customers`
- `GET /api/v1/customers/{id}`
- `POST /api/v1/customers`
- `POST /api/v1/orders`
- `GET /api/v1/orders`
- `GET /api/v1/orders/{id}`
- `PATCH /api/v1/orders/{id}/status`
- `DELETE /api/v1/orders/{id}`
- `POST /api/v1/orders/{id}/payments`
- `GET /api/v1/orders/{id}/payments`
- `POST /api/v1/payment/create`
- `POST /api/webhook/sepay`
- `GET /api/v1/transactions`
- `GET /api/v1/transactions/order/{orderId}`

### 3.2 Sequence nghiep vu hien tai

#### A. Chon hang

Frontend goi:
- `GET /api/v1/products`

Du lieu tra ve co:
- `id`
- `name`
- `sku`
- `barcode`
- `stock`
- `sellingPrice`

Nhan xet:
- Du de hien thi danh sach hang hoa
- Chua co API scan barcode rieng cho POS nhanh

#### B. Chon khach hang

Frontend co the:
- lay danh sach khach bang `GET /api/v1/customers`
- tao nhanh khach bang `POST /api/v1/customers`

Nhan xet:
- `customerId` la bat buoc khi tao order
- Chua co API tim khach theo so dien thoai toi uu cho POS

#### C. Tao don ban

Frontend goi:
- `POST /api/v1/orders`

Payload:

```json
{
  "userId": 1,
  "customerId": 10,
  "discount": 5000,
  "items": [
    {
      "productId": 101,
      "quantity": 2,
      "price": 45000
    }
  ]
}
```

Backend trong `OrderService.create(...)` thuc hien:
- kiem tra `user`
- kiem tra `customer`
- lock tung `product` bang pessimistic lock
- kiem tra du ton
- tinh tong tien
- tao `Order` voi `status = PENDING`
- tao `OrderItems`
- tru ton kho ngay
- ghi `InventoryLogs` loai `SALE`

Nhan xet quan trong:
- He thong tru ton ngay khi tao order
- Nghia la `POST /orders` da co tac dung "chot ban hang" mot phan
- Don `PENDING` hien tai la "da tru kho, chua thu tien", khong phai "don tam"

#### D. Thu tien truc tiep

Frontend goi:
- `POST /api/v1/orders/{id}/payments`

Backend trong `PaymentService.createPayment(...)`:
- lock `order`
- chan neu order da `CANCELLED`
- chan neu order da `COMPLETED`
- chan neu order da co payment
- validate so tien thanh toan phai khop `order.totalAmount`
- tao `Payment`
- neu `payment.status = COMPLETED`:
  - tao `Transaction` loai `REVENUE`
  - cap nhat `Order.status = COMPLETED`
  - set `paidAt`

Nhan xet:
- 1 don chi co 1 payment
- khong ho tro split payment
- khong ho tro partial payment

#### E. Thu tien qua QR / SePay

Frontend goi:
- `POST /api/v1/payment/create`

Backend tra ve:
- `payment_url`
- `order_code`

Sau do SePay webhook vao:
- `POST /api/webhook/sepay`

Backend trong `SePayService.processWebhook(...)`:
- doc `transaction_id`, `amount`, `content`
- tach `orderCode` tu noi dung chuyen khoan
- lock `order` theo `code`
- neu don da `COMPLETED` thi bo qua
- neu amount khong khop thi bo qua
- tao `Transaction` loai `REVENUE`
- update `Order.status = COMPLETED`
- set `paidAt`

Nhan xet:
- Luong webhook va luong manual payment chua dong nhat hoan toan
- Luong webhook hien tai khong tao `Payment`, chu yeu tao `Transaction`

#### F. Huy don

Frontend goi:
- `DELETE /api/v1/orders/{id}`

Backend trong `OrderService.cancel(...)`:
- lock `order`
- set `status = CANCELLED`
- lock tung `product`
- cong lai stock
- ghi `InventoryLogs` loai `RETURN`
- neu don da thu tien thi tao `Transaction` loai `REFUND`

Nhan xet:
- He thong co ho tro huy don va hoan kho
- Neu da thu tien thi co sinh nghiep vu hoan tien o muc so quy

## 4. Mapping voi luong KiotViet

| Buoc POS chuan | ShopLite hien tai | Danh gia |
|---|---|---|
| Tim hang | `GET /products` | Co, co ban |
| Scan barcode | Chua co API rieng | Thieu |
| Gio hang tam | Chua co | Thieu |
| Chon khach | `GET/POST /customers` | Co |
| Don tam / luu tam | Chua co | Thieu |
| Chot ban hang | `POST /orders` | Co, nhung dang tru ton ngay |
| Thu tien | `POST /orders/{id}/payments` | Co |
| Thanh toan QR | `POST /payment/create` + webhook | Co |
| Nhieu lan thanh toan | Chua co | Thieu |
| Hoan / huy don | `DELETE /orders/{id}` | Co |
| Tra hang tung phan | Chua co | Thieu |

## 5. Danh gia nghiep vu hien tai

### 5.1 Diem dung

- Da co luong end-to-end de ban hang
- Da co pessimistic locking cho ton kho va order
- Da co inventory log khi ban va khi huy
- Da co cashbook qua `Transaction`
- Da co payment manual va payment webhook

### 5.2 Diem lech so voi POS chuan

#### A. Chua co tang "gio hang / hoa don tam"

He thong hien tai di thang tu:
- chon hang
- sang `POST /orders`

Dieu nay lam mat kha nang:
- luu nhap
- treo don
- tao nhieu hoa don tam
- thao tac nhanh tai quay

#### B. Tru ton qua som

Ngay khi tao order da:
- tru stock
- ghi inventory sale

Dieu nay phu hop voi "xac nhan ban hang" nhung khong phu hop voi "don tam".  
Neu frontend moi nhan nut tao don de luu tam, nghiep vu se sai.

#### C. Chua tach ro "don ban" va "thanh toan"

Hien tai:
- `Order.PENDING` = da tru kho, chua thu tien
- `Order.COMPLETED` = da thu tien

Kieu model nay dung duoc, nhung can quy uoc rat ro.  
Neu mo rong sau nay se de roi vao nham lan giua:
- don tam
- don da xuat kho
- don da thu tien

#### D. Luong payment chua dong nhat

Manual payment:
- tao `Payment`
- tao `Transaction`
- cap nhat order

Webhook payment:
- tao `Transaction`
- cap nhat order

Khac biet nay se gay kho khi:
- can bao cao payment theo kenh
- can doi soat
- can xem order da thanh toan bang phuong thuc nao theo cung mot model

## 6. De xuat refactor de ra POS chuan

### Option 1: Refactor it nhat, giu lai mo hinh hien tai

Muc tieu:
- van dung `Order` lam aggregate chinh
- bo sung trang thai ro hon

De xuat:
- `DRAFT`: don tam, chua tru kho
- `CONFIRMED` hoac `PENDING_PAYMENT`: da tru kho, chua thanh toan
- `COMPLETED`: da thanh toan
- `CANCELLED`: da huy

Luong moi:

1. `POST /api/v1/orders`
   tao don o `DRAFT`, chua tru ton
2. `PATCH /api/v1/orders/{id}/confirm`
   lock san pham, validate ton, tru ton, ghi inventory log, chuyen sang `PENDING_PAYMENT`
3. `POST /api/v1/orders/{id}/payments`
   thu tien, tao payment/transaction, chuyen sang `COMPLETED`
4. `DELETE /api/v1/orders/{id}`
   neu `DRAFT` thi huy mem
   neu `PENDING_PAYMENT` hoac `COMPLETED` thi hoan kho, refund neu can

Uu diem:
- thay doi vua phai
- frontend POS de xay hon
- nghiep vu ro hon hien tai

### Option 2: Tach them bang `cart` hoac `sales_session`

Muc tieu:
- giong POS chuan hon
- ho tro treo don / giu gio hang theo thu ngan

Them aggregate:
- `SaleCart`
- `SaleCartItem`

Luong:
- thao tac tai quay tren cart
- khi bam "Thanh toan" moi sinh `Order`
- luc do moi tru ton va tao chung tu thu tien

Uu diem:
- rat gan KiotViet
- de them barcode, treo don, phuc vu nhanh

Nhuoc diem:
- ton cong refactor hon

## 7. API de xuat cho POS chuan

Neu di theo Option 1, toi de xuat them:

- `POST /api/v1/orders`
  tao `DRAFT`
- `PATCH /api/v1/orders/{id}/confirm`
  chot don va tru kho
- `PATCH /api/v1/orders/{id}/status?status=DRAFT|PENDING_PAYMENT|COMPLETED|CANCELLED`
  chi giu cho admin/backoffice
- `POST /api/v1/orders/{id}/payments`
  thu tien thu cong
- `POST /api/v1/orders/{id}/payments/qr`
  tao QR / link thanh toan
- `GET /api/v1/orders/{id}/transactions`
  xem tien da thu / refund
- `GET /api/v1/products/search?keyword=&barcode=`
  toi uu man hinh POS
- `GET /api/v1/customers/search?phone=`
  tim nhanh khach tai quay

Neu di theo Option 2, them:

- `POST /api/v1/carts`
- `POST /api/v1/carts/{id}/items`
- `PUT /api/v1/carts/{id}/items/{itemId}`
- `DELETE /api/v1/carts/{id}/items/{itemId}`
- `PATCH /api/v1/carts/{id}/customer`
- `POST /api/v1/carts/{id}/checkout`

## 8. Kien truc nghiep vu khuyen nghi

Cho du an nay, huong thuc te nhat la:

### Giai doan 1

Refactor theo Option 1:
- bo sung `DRAFT`
- doi `POST /orders` thanh tao don tam
- tao endpoint `confirm`
- chi tru ton tai `confirm`
- giu payment o buoc sau

### Giai doan 2

Chuan hoa payment:
- webhook cung tao `Payment` thay vi chi tao `Transaction`
- `Transaction` dong vai tro cashbook / ledger
- `Payment` dong vai tro nghiep vu thu tien

### Giai doan 3

Bo sung POS nhanh:
- search barcode
- search customer theo phone
- treo don / mo lai don tam
- split payment neu can

## 9. Ket luan

He thong hien tai da co mot luong ban hang co ban:
- tao don
- tru kho
- thu tien
- huy don
- hoan kho

Nhung no chua la POS chuan kieu KiotViet vi:
- chua co gio hang tam
- chua co don nhap
- chua tach ro xac nhan ban va thu tien
- chua dong nhat model payment giua manual va webhook

Huong phu hop nhat la:
- giu `Order` lam trung tam
- them pha `DRAFT -> CONFIRM -> PAYMENT`
- sau do moi mo rong them cart/session neu can POS nhanh thuc su

