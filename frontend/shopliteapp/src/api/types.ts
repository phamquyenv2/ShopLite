export type Category = {
    id: number;
    name: string;
};

export type Unit = {
    id: number;
    name: string;
    description?: string | null;
};

export type Product = {
    id: number;
    categoryId: number;
    categoryName?: string | null;
    unitId: number;
    unitName?: string | null;
    name: string;
    sku?: string | null;
    barcode?: string | null;
    stock: number;
    costPrice: number;
    sellingPrice: number;
    minStock?: number | null;
    maxStock?: number | null;
    image?: string | null;
    status?: 'ACTIVE' | 'INACTIVE' | 'OUT_OF_STOCK';
    deleted?: boolean;
    createdAt?: string;
    updatedAt?: string;
    version?: number | null;
};

export type ProductPage = {
    totalElements: number;
    totalPages: number;
    page: number;
    size: number;
    data: Product[];
};

export type ProductUpsert = {
    categoryId: number;
    unitId: number;
    name: string;
    sku?: string | null;
    barcode?: string | null;
    stock: number;
    sellingPrice: number;
    costPrice: number;
    minStock?: number | null;
    maxStock?: number | null;
    image?: string | null;
    status?: 'ACTIVE' | 'INACTIVE' | 'OUT_OF_STOCK';
    version?: number | null;
};

export type Customer = {
    id: number;
    name: string;
    phone: string;
    points?: number | null;
};

export type CustomerUpsert = {
    name: string;
    phone: string;
};

export type OrderItemUpsert = {
    productId: number;
    quantity: number;
    price: number;
};

export type OrderUpsert = {
    userId: number;
    requestId?: string | null;
    customerId?: number | null;
    discount?: number | null;
    items: OrderItemUpsert[];
};

export type OrderItem = {
    id: number;
    productId: number;
    productName?: string | null;
    quantity: number;
    price: number;
    totalPrice?: number | null;
};

export type Order = {
    id: number;
    code?: string | null;
    requestId?: string | null;
    customerId?: number | null;
    customerName?: string | null;
    totalAmount?: number | null;
    discount?: number | null;
    status?: 'DRAFT' | 'PENDING' | 'PENDING_PAYMENT' | 'COMPLETED' | 'FAIL' | 'CANCELLED';
    createdAt?: string;
    paidAt?: string | null;
    userId?: number | null;
    username?: string | null;
    items?: OrderItem[];
};

export type TransactionType = 'REVENUE' | 'EXPENSE' | 'REFUND' | 'SALARY';

export type TransactionUpsert = {
    externalId?: string | null;
    bankCode?: string | null;
    amount: number;
    type: TransactionType;
    content?: string | null;
    transactionTime?: string | null;
    orderId?: number | null;
};

export type Transaction = {
    id: number;
    amount: number;
    type: TransactionType;
    content?: string | null;
    transactionTime?: string | null;
    createdAt?: string;
    orderId?: number | null;
    orderCode?: string | null;
    importOrderId?: number | null;
    paymentId?: number | null;
    payrollId?: number | null;
};

export type InventoryType = 'IMPORT' | 'SALE' | 'ADJUST' | 'RETURN';

export type InventoryLogUpsert = {
    productId: number;
    changeQuantity: number;
    type: InventoryType;
    adjustmentId?: number | null;
};

export type InventoryLog = {
    id: number;
    quantityIn?: number | null;
    quantityOut?: number | null;
    balanceAfter?: number | null;
    currentStock?: number | null;
    type: InventoryType;
    createdAt?: string;
    productId: number;
    productName?: string | null;
    productSku?: string | null;
};

export type AdjustmentItemUpsert = {
    productId: number;
    actualQuantity: number;
};

export type InventoryAdjustmentUpsert = {
    reason: string;
    note?: string | null;
    createdBy: string;
    items: AdjustmentItemUpsert[];
};

export type InventoryAdjustment = {
    id: number;
    reason: string;
    note?: string | null;
    createdBy: string;
    createdAt?: string;
    logs?: InventoryLog[];
};
