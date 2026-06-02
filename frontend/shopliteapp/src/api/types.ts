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
    paymentMethod?: 'CASH' | 'BANK' | 'SEPAY_QR' | 'COD' | 'VNPAY' | 'MOMO' | null;
    status?: 'DRAFT' | 'PENDING' | 'PENDING_PAYMENT' | 'COMPLETED' | 'FAIL' | 'CANCELLED';
    createdAt?: string;
    paidAt?: string | null;
    userId?: number | null;
    username?: string | null;
    items?: OrderItem[];
};

export type TransactionType = 'REVENUE' | 'EXPENSE' | 'REFUND' | 'SALARY' | 'ADJUSTMENT';

export type FundType = 'CASH' | 'BANK' | 'EWALLET';

export type FundAccount = {
    id: number;
    name: string;
    type: FundType;
    openingBalance: number;
    balance: number;
    isActive: boolean;
};

export type TransactionUpsert = {
    externalId?: string | null;
    bankCode?: string | null;
    amount: number;
    type: TransactionType;
    content?: string | null;
    transactionTime?: string | null;
    orderId?: number | null;
};

export type DirectionEnum = 'IN' | 'OUT';

export type Transaction = {
    id: number;
    type: TransactionType;
    direction: DirectionEnum;
    amount: number;
    content?: string | null;
    transactionCode?: string | null;
    transactionTime?: string | null;
    createdAt?: string;
    paymentId?: number | null;
    fundAccountId?: number | null;
    fundAccountName?: string | null;
    balanceBefore?: number | null;
    balanceAfter?: number | null;
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

export type Employee = {
    id: number;
    userId: number;
    username: string;
    phone?: string | null;
    roleName?: string | null;
    salaryRate: number;
    qr?: string | null;
    note?: string | null;
    deleted: boolean;
    officeId?: number | null;
    officeName?: string | null;
};

export type Office = {
    id: number;
    name: string;
    officeLat: number;
    officeLng: number;
    radius: number;
};

export type SalaryType = 'HOURLY' | 'DAILY' | 'MONTHLY';

export type EmployeeSalaryHistory = {
    id: number;
    employeeId: number;
    employeeUsername?: string | null;
    salaryType: SalaryType;
    baseRate: number;
    allowance: number;
    commission: number;
    recurringBonus: number;
    recurringDeduction: number;
    effectiveFrom: string;
    effectiveTo?: string | null;
    reason?: string | null;
    createdBy?: string | null;
    createdAt?: string | null;
    current: boolean;
};

export type EmployeeSalaryHistoryPayload = {
    salaryType: SalaryType;
    baseRate: number;
    allowance?: number;
    commission?: number;
    recurringBonus?: number;
    recurringDeduction?: number;
    effectiveFrom?: string | null;
    reason?: string | null;
};

export type RosterType = 'WORKING' | 'LEAVE_APPROVED' | 'LEAVE_UNAPPROVED';

export type Roster = {
    id: number;
    employeeId: number;
    employeeUsername?: string | null;
    workingDay: string;
    startTime?: string | null;
    endTime?: string | null;
    checkInAllowedFrom?: string | null;
    checkInAllowedTo?: string | null;
    checkOutAllowedFrom?: string | null;
    checkOutAllowedTo?: string | null;
    expectedHours?: number | null;
    type: RosterType;
    note?: string | null;
    unpaidBreakMinutes?: number | null;
    expired?: boolean | null;
};

export type RosterUpsert = {
    employeeId: number;
    workingDay: string;
    startTime?: string | null;
    endTime?: string | null;
    checkInAllowedFrom?: string | null;
    checkInAllowedTo?: string | null;
    checkOutAllowedFrom?: string | null;
    checkOutAllowedTo?: string | null;
    type: RosterType;
    note?: string | null;
    unpaidBreakMinutes?: number;
};

export type AttendanceStatus = 'VALID' | 'LATE' | 'EARLY_LEAVE' | 'ABSENT' | 'AUTO_CLOSED' | 'INVALID';

export type Attendance = {
    id: number;
    employeeId: number;
    employeeUsername?: string | null;
    officeId?: number | null;
    officeName?: string | null;
    rosterId?: number | null;
    checkIn?: string | null;
    checkOut?: string | null;
    workedMinutes?: number | null;
    payableMinutes?: number | null;
    workingDay?: string | null;
    walkIn?: boolean;
    latitude?: number | null;
    longitude?: number | null;
    distance?: number | null;
    checkOutLatitude?: number | null;
    checkOutLongitude?: number | null;
    checkOutDistance?: number | null;
    lateMinutes?: number | null;
    earlyLeaveMinutes?: number | null;
    closedAutomatically?: boolean;
    status?: AttendanceStatus | string | null;
};

export type AttendanceLocationPayload = {
    latitude: number;
    longitude: number;
    rosterId?: number | null;
    deviceId?: string | null;
};

export type Payroll = {
    id: number;
    employeeId: number;
    employeeUsername?: string | null;
    period: string;
    salaryType?: SalaryType | null;
    salaryRate?: number | null;
    allowance?: number | null;
    commission?: number | null;
    totalHours?: number | null;
    bonus?: number | null;
    penalty?: number | null;
    deduction?: number | null;
    totalSalary?: number | null;
    scheduledWorkingDays: number;
    actualPresentDays: number;
    approvedLeaveDays: number;
    absentWithoutLeaveDays: number;
};

export type PayrollSyncPayload = {
    employeeId?: number | null;
    period: string;
    bonus?: number;
    penalty?: number;
    penaltyPerAbsent?: number;
};

export type Permission = {
    id: number;
    name: string;
    apiPath: string;
    method: string;
    module: string;
};

export type MenuType = 'TAB' | 'SHORTCUT' | 'GROUP' | 'ITEM';

export type Menu = {
    id: number;
    code: string;
    title: string;
    route?: string | null;
    icon?: string | null;
    menuType: MenuType;
    parentId?: number | null;
    sortOrder?: number | null;
};

export type StoreInvitationStatus = 'PENDING' | 'ACCEPTED' | 'DECLINED' | 'EXPIRED';

export type StoreInvitation = {
    id: number;
    storeId: number;
    storeName: string;
    invitedUserId: number;
    invitedUsername: string;
    phone: string;
    roleId: number;
    roleName: string;
    officeId?: number | null;
    officeName?: string | null;
    status: StoreInvitationStatus;
    expiresAt: string;
    createdAt: string;
    respondedAt?: string | null;
};

export type Notification = {
    id: number;
    type: 'STORE_INVITATION' | 'IMPORT_ORDER_INSPECTION' | 'IMPORT_ORDER_DISCREPANCY_APPROVAL';
    title: string;
    message: string;
    referenceId?: number | null;
    read: boolean;
    actionTaken: boolean;
    createdAt: string;
    invitation?: {
        id: number;
        storeId: number;
        storeName: string;
        roleName: string;
        invitedByUsername: string;
        status: StoreInvitationStatus;
        expiresAt: string;
    } | null;
};

export type AcceptInvitationResponse = {
    currentStore: {
        id: number;
        name: string;
        memberRole: string;
        membershipStatus: string;
        permissions?: Permission[];
        menus?: Menu[];
    };
    permissions: Permission[];
};

export type Role = {
    id: number;
    name: string;
    description: string;
    active: boolean;
    permissions: Permission[];
};

export type Supplier = {
    id: number;
    name: string;
    phone?: string | null;
    address?: string | null;
    email?: string | null;
    createdAt?: string;
};

export type ImportItem = {
    id: number;
    productId: number;
    productName?: string;
    productSku?: string;
    quantity: number;
    receivedQuantity?: number;
    discrepancyQuantity?: number;
    discrepancyType?: 'MATCHED' | 'SHORTAGE' | 'EXCESS';
    inspectionNote?: string;
    returnedQuantity?: number;
    importPrice: number;
    subTotal?: number;
};

export type ImportOrder = {
    id: number;
    supplierId: number;
    supplierName?: string;
    supplierPhone?: string;
    username?: string;
    tax?: number;
    discount?: number;
    totalAmount?: number;
    amountPaid?: number;
    status: 'PENDING' | 'WAITING_FOR_INSPECTION' | 'PENDING_DISCREPANCY_APPROVAL' | 'PENDING_PAYMENT' | 'COMPLETED' | 'CANCELLED';
    returnStatus?: 'UNRETURNED' | 'PARTIAL_RETURNED' | 'FULL_RETURNED';
    note?: string;
    createdAt?: string;
    sentAt?: string;
    inspectedAt?: string;
    approvedAt?: string;
    stockAppliedAt?: string;
    inspectedBy?: string;
    approvedBy?: string;
    discrepancyNote?: string;
    items?: ImportItem[];
};

export type ImportOrderUpsert = {
    supplierId: number;
    items: { productId: number; quantity: number; importPrice: number }[];
    tax?: number;
    discount?: number;
    note?: string;
    paidAmount?: number;
    paymentMethod?: string;
    status?: 'PENDING' | 'CANCELLED';
};

export type InspectImportOrderPayload = {
    note?: string;
    items: {
        importItemId: number;
        receivedQuantity: number;
        note?: string;
    }[];
};

export type ImportReturnItem = {
    id?: number;
    productId: number;
    productName?: string;
    productSku?: string;
    productImage?: string;
    quantity: number;
    returnPrice: number;
    subTotal?: number;
};

export type ImportReturnOrder = {
    id: number;
    supplierId: number;
    supplierName?: string;
    supplierPhone?: string;
    importOrderId?: number;
    totalAmount?: number;
    discount?: number;
    amountPaid?: number;
    note?: string;
    createdByUsername?: string;
    receivedByUsername?: string;
    createdAt?: string;
    items?: ImportReturnItem[];
};

export type ImportReturnOrderUpsert = {
    supplierId: number;
    importOrderId?: number;
    items: { productId: number; quantity: number; returnPrice: number }[];
    discount?: number;
    amountPaid?: number;
    note?: string;
    createdByUsername?: string;
    receivedByUsername?: string;
};
