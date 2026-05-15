package com.quyen.shoplite.config;

import com.quyen.shoplite.repository.EmployeeRepository;
import com.quyen.shoplite.repository.FundAccountRepository;
import com.quyen.shoplite.repository.MenuRepository;
import com.quyen.shoplite.repository.OfficeRepository;
import com.quyen.shoplite.repository.PermissionRepository;
import com.quyen.shoplite.repository.RoleRepository;
import com.quyen.shoplite.repository.StoreMemberRepository;
import com.quyen.shoplite.repository.StoreRepository;
import com.quyen.shoplite.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.jdbc.core.JdbcTemplate;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import com.quyen.shoplite.util.constant.MenuType;

import com.quyen.shoplite.domain.Employee;
import com.quyen.shoplite.domain.FundAccount;
import com.quyen.shoplite.domain.Menu;
import com.quyen.shoplite.domain.Office;
import com.quyen.shoplite.domain.Permission;
import com.quyen.shoplite.domain.Role;
import com.quyen.shoplite.domain.Store;
import com.quyen.shoplite.domain.StoreMember;
import com.quyen.shoplite.domain.User;

@Component
@Order(0)
@RequiredArgsConstructor
@Slf4j
public class DatabaseInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final EmployeeRepository employeeRepository;
    private final OfficeRepository officeRepository;
    private final FundAccountRepository fundAccountRepository;
    private final StoreRepository storeRepository;
    private final StoreMemberRepository storeMemberRepository;
    private final MenuRepository menuRepository;
    private final PasswordEncoder passwordEncoder;
    private final JdbcTemplate jdbcTemplate;

    @Override
    @Transactional
    public void run(String... args) {
        jdbcTemplate_safeUpdateRoles();
        cleanUpRedundantColumns();
        ensureSeedJoinTables();
        seedPermissionsAndRoles();
        seedMenus();
        seedAdminUser();
        seedEmployees();
        seedFundAccounts();
    }

    /** Safe no-op — version column is on roles only */
    private void jdbcTemplate_safeUpdateRoles() {
        // roles still have @Version — patch nulls if any
        roleRepository.findAll().forEach(r -> {
            if (r.getVersion() == null) {
                r.setVersion(0);
                roleRepository.save(r);
            }
        });
    }

    private void cleanUpRedundantColumns() {
        dropOldRosterEmployeeDayConstraint();
    }

    private void ensureSeedJoinTables() {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS permission_role (
                    role_id BIGINT NOT NULL,
                    permission_id BIGINT NOT NULL,
                    PRIMARY KEY (role_id, permission_id),
                    CONSTRAINT fk_permission_role_role
                        FOREIGN KEY (role_id) REFERENCES roles(id),
                    CONSTRAINT fk_permission_role_permission
                        FOREIGN KEY (permission_id) REFERENCES permissions(id)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
                """);

        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS menu_permissions (
                    menu_id BIGINT NOT NULL,
                    permission_id BIGINT NOT NULL,
                    PRIMARY KEY (menu_id, permission_id),
                    CONSTRAINT fk_menu_permissions_menu
                        FOREIGN KEY (menu_id) REFERENCES menus(id),
                    CONSTRAINT fk_menu_permissions_permission
                        FOREIGN KEY (permission_id) REFERENCES permissions(id)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
                """);
    }

    private void dropOldRosterEmployeeDayConstraint() {
        ensureRosterEmployeeIndex();

        try {
            List<String> matchingUniqueIndexes = jdbcTemplate.queryForList(
                    """
                    SELECT s.INDEX_NAME
                    FROM information_schema.STATISTICS s
                    WHERE s.TABLE_SCHEMA = DATABASE()
                      AND s.TABLE_NAME = 'rosters'
                      AND s.NON_UNIQUE = 0
                      AND s.COLUMN_NAME IN ('employee_id', 'working_day')
                    GROUP BY s.INDEX_NAME
                    HAVING COUNT(DISTINCT s.COLUMN_NAME) = 2
                    """,
                    String.class
            );
            for (String indexName : matchingUniqueIndexes) {
                try {
                    jdbcTemplate.execute("ALTER TABLE rosters DROP INDEX `" + indexName + "`");
                    log.info("Dropped old roster employee/day unique index from information_schema: {}", indexName);
                } catch (Exception e) {
                    log.warn("Could not drop roster unique index {}: {}", indexName, e.getMessage());
                }
            }
        } catch (Exception e) {
            log.debug("Could not inspect roster indexes from information_schema: {}", e.getMessage());
        }

        List<String> statements = List.of(
                "ALTER TABLE rosters DROP CONSTRAINT uk_roster_employee_day",
                "ALTER TABLE rosters DROP INDEX uk_roster_employee_day",
                "DROP INDEX uk_roster_employee_day ON rosters",
                "DROP INDEX IF EXISTS uk_roster_employee_day"
        );

        for (String statement : statements) {
            try {
                jdbcTemplate.execute(statement);
                log.info("Dropped old roster employee/day uniqueness with: {}", statement);
            } catch (Exception e) {
                log.debug("Could not execute '{}': {}", statement, e.getMessage());
            }
        }
    }

    private void ensureRosterEmployeeIndex() {
        try {
            Integer count = jdbcTemplate.queryForObject(
                    """
                    SELECT COUNT(*)
                    FROM information_schema.STATISTICS s
                    WHERE s.TABLE_SCHEMA = DATABASE()
                      AND s.TABLE_NAME = 'rosters'
                      AND s.INDEX_NAME = 'idx_rosters_employee_id'
                    """,
                    Integer.class
            );
            if (count == null || count == 0) {
                jdbcTemplate.execute("CREATE INDEX idx_rosters_employee_id ON rosters (employee_id)");
                log.info("Created non-unique roster employee index before dropping old uniqueness.");
            }
        } catch (Exception e) {
            log.debug("Could not ensure roster employee index: {}", e.getMessage());
        }
    }

    private void seedPermissionsAndRoles() {
        List<Permission> desiredPermissions = List.of(
                // AUTH
                perm("Login", "/api/v1/auth/login", "POST", "AUTH"),
                perm("Register", "/api/v1/auth/register", "POST", "AUTH"),
                perm("Refresh token", "/api/v1/auth/refresh", "POST", "AUTH"),
                perm("Get current user", "/api/v1/auth/me", "GET", "AUTH"),
                perm("Logout", "/api/v1/auth/logout", "POST", "AUTH"),
                perm("Send OTP", "/api/v1/auth/register/otp/send", "POST", "AUTH"),
                perm("Verify OTP", "/api/v1/auth/register/otp/verify", "POST", "AUTH"),
                perm("Set store name", "/api/v1/auth/register/store", "POST", "AUTH"),
                perm("Complete register", "/api/v1/auth/register/complete", "POST", "AUTH"),
                // PRODUCTS
                perm("View products", "/api/v1/products", "GET", "PRODUCTS"),
                perm("Search products for POS", "/api/v1/products/search", "GET", "PRODUCTS"),
                perm("View product by id", "/api/v1/products/{id}", "GET", "PRODUCTS"),
                perm("Create product", "/api/v1/products", "POST", "PRODUCTS"),
                perm("Update product", "/api/v1/products/{id}", "PUT", "PRODUCTS"),
                perm("Delete product", "/api/v1/products/{id}", "DELETE", "PRODUCTS"),
                // CATEGORIES
                perm("View categories", "/api/v1/categories", "GET", "CATEGORIES"),
                perm("View category by id", "/api/v1/categories/{id}", "GET", "CATEGORIES"),
                perm("Create category", "/api/v1/categories", "POST", "CATEGORIES"),
                perm("Update category", "/api/v1/categories/{id}", "PUT", "CATEGORIES"),
                perm("Delete category", "/api/v1/categories/{id}", "DELETE", "CATEGORIES"),
                // CUSTOMERS
                perm("View customers", "/api/v1/customers", "GET", "CUSTOMERS"),
                perm("View customer by id", "/api/v1/customers/{id}", "GET", "CUSTOMERS"),
                perm("Create customer", "/api/v1/customers", "POST", "CUSTOMERS"),
                perm("Update customer", "/api/v1/customers/{id}", "PUT", "CUSTOMERS"),
                perm("Delete customer", "/api/v1/customers/{id}", "DELETE", "CUSTOMERS"),
                // SUPPLIERS
                perm("View suppliers", "/api/v1/suppliers", "GET", "SUPPLIERS"),
                perm("View supplier by id", "/api/v1/suppliers/{id}", "GET", "SUPPLIERS"),
                perm("Create supplier", "/api/v1/suppliers", "POST", "SUPPLIERS"),
                perm("Update supplier", "/api/v1/suppliers/{id}", "PUT", "SUPPLIERS"),
                perm("Delete supplier", "/api/v1/suppliers/{id}", "DELETE", "SUPPLIERS"),
                // UNITS
                perm("View units", "/api/v1/units", "GET", "UNITS"),
                perm("View unit by id", "/api/v1/units/{id}", "GET", "UNITS"),
                perm("Create unit", "/api/v1/units", "POST", "UNITS"),
                perm("Update unit", "/api/v1/units/{id}", "PUT", "UNITS"),
                perm("Delete unit", "/api/v1/units/{id}", "DELETE", "UNITS"),
                // SALES INIT
                perm("Get sales init data", "/api/v1/sales/init", "GET", "SALES"),
                // ORDERS
                perm("View orders", "/api/v1/orders", "GET", "ORDERS"),
                perm("Confirm order", "/api/v1/orders/{id}/confirm", "PATCH", "ORDERS"),
                perm("View order by id", "/api/v1/orders/{id}", "GET", "ORDERS"),
                perm("Create order", "/api/v1/orders", "POST", "ORDERS"),
                perm("Update order", "/api/v1/orders/{id}", "PUT", "ORDERS"),
                perm("Update order status PUT", "/api/v1/orders/{id}/status", "PUT", "ORDERS"),
                perm("Update order status PATCH", "/api/v1/orders/{id}/status", "PATCH", "ORDERS"),
                perm("Cancel order", "/api/v1/orders/{id}", "DELETE", "ORDERS"),
                perm("Create payment for order", "/api/v1/orders/{id}/payments", "POST", "ORDERS"),
                perm("View payments for order", "/api/v1/orders/{id}/payments", "GET", "ORDERS"),
                // TRANSACTIONS
                perm("Create transaction", "/api/v1/transactions", "POST", "TRANSACTIONS"),
                perm("View transactions", "/api/v1/transactions", "GET", "TRANSACTIONS"),
                perm("View transaction by id", "/api/v1/transactions/{id}", "GET", "TRANSACTIONS"),
                perm("View transactions by order", "/api/v1/transactions/order/{orderId}", "GET", "TRANSACTIONS"),
                perm("View transactions by fund", "/api/v1/transactions/fund-account/{fundAccountId}", "GET", "TRANSACTIONS"),
                perm("View transactions by payment", "/api/v1/transactions/payment/{paymentId}", "GET", "TRANSACTIONS"),
                // PAYMENT
                perm("Create payment session", "/api/v1/payment/create", "POST", "PAYMENT"),
                // DASHBOARD
                perm("View dashboard today", "/api/v1/dashboard/today", "GET", "DASHBOARD"),
                // DEVICE TOKENS
                perm("Register device token", "/api/v1/device-tokens/register", "POST", "DEVICE_TOKENS"),
                perm("Delete device token", "/api/v1/device-tokens", "DELETE", "DEVICE_TOKENS"),
                perm("Test notification", "/api/v1/device-tokens/test-notification", "POST", "DEVICE_TOKENS"),
                // ATTENDANCE
                perm("Check in", "/api/v1/attendance/check-in", "POST", "ATTENDANCE"),
                perm("Check out", "/api/v1/attendance/check-out", "POST", "ATTENDANCE"),
                perm("View my today attendance", "/api/v1/attendance/me/today", "GET", "ATTENDANCE"),
                perm("View my today roster", "/api/v1/attendance/me/rosters/today", "GET", "ATTENDANCE"),
                perm("View attendances", "/api/v1/attendance", "GET", "ATTENDANCE"),
                perm("View attendance by id", "/api/v1/attendance/{id}", "GET", "ATTENDANCE"),
                // ROSTER
                perm("Create roster", "/api/v1/roster", "POST", "ROSTER"),
                perm("View roster by id", "/api/v1/roster/{id}", "GET", "ROSTER"),
                perm("View roster by employee", "/api/v1/roster/employee/{employeeId}", "GET", "ROSTER"),
                perm("View roster by day", "/api/v1/roster/day", "GET", "ROSTER"),
                perm("View roster by month", "/api/v1/roster/month", "GET", "ROSTER"),
                perm("Update roster", "/api/v1/roster/{id}", "PUT", "ROSTER"),
                perm("Delete roster", "/api/v1/roster/{id}", "DELETE", "ROSTER"),
                // PAYROLLS
                perm("Sync payroll monthly", "/api/v1/payrolls/sync-monthly", "POST", "PAYROLLS"),
                perm("View payrolls", "/api/v1/payrolls", "GET", "PAYROLLS"),
                perm("View my payrolls", "/api/v1/payrolls/me", "GET", "PAYROLLS"),
                perm("View payroll by id", "/api/v1/payrolls/{id}", "GET", "PAYROLLS"),
                perm("View payroll by employee", "/api/v1/payrolls/employee/{employeeId}", "GET", "PAYROLLS"),
                // EMPLOYEES
                perm("Create employee", "/api/v1/employees", "POST", "EMPLOYEES"),
                perm("View employees", "/api/v1/employees", "GET", "EMPLOYEES"),
                perm("View employee by id", "/api/v1/employees/{id}", "GET", "EMPLOYEES"),
                perm("Update employee", "/api/v1/employees/{id}", "PUT", "EMPLOYEES"),
                perm("Delete employee", "/api/v1/employees/{id}", "DELETE", "EMPLOYEES"),
                perm("Create employee salary history", "/api/v1/employees/{employeeId}/salary-histories", "POST", "EMPLOYEE_SALARIES"),
                perm("View employee salary histories", "/api/v1/employees/{employeeId}/salary-histories", "GET", "EMPLOYEE_SALARIES"),
                perm("View current employee salary", "/api/v1/employees/{employeeId}/salary-histories/current", "GET", "EMPLOYEE_SALARIES"),
                perm("View my current salary", "/api/v1/employee-salaries/me", "GET", "EMPLOYEE_SALARIES"),
                perm("View my salary history", "/api/v1/employee-salaries/me/history", "GET", "EMPLOYEE_SALARIES"),
                // OFFICES
                perm("Create office", "/api/v1/offices", "POST", "OFFICES"),
                perm("View offices", "/api/v1/offices", "GET", "OFFICES"),
                perm("View office by id", "/api/v1/offices/{id}", "GET", "OFFICES"),
                perm("Update office", "/api/v1/offices/{id}", "PUT", "OFFICES"),
                perm("Delete office", "/api/v1/offices/{id}", "DELETE", "OFFICES"),
                // IMPORT ORDERS
                perm("Create import order", "/api/v1/import-orders", "POST", "IMPORT_ORDERS"),
                perm("View import orders", "/api/v1/import-orders", "GET", "IMPORT_ORDERS"),
                perm("View import order by id", "/api/v1/import-orders/{id}", "GET", "IMPORT_ORDERS"),
                perm("Update import order", "/api/v1/import-orders/{id}", "PUT", "IMPORT_ORDERS"),
                perm("Update import order status", "/api/v1/import-orders/{id}/status", "PUT", "IMPORT_ORDERS"),
                perm("Pay import order", "/api/v1/import-orders/{id}/pay", "POST", "IMPORT_ORDERS"),
                // IMPORT RETURN ORDERS
                perm("Create import return", "/api/v1/import-return-orders", "POST", "IMPORT_RETURN_ORDERS"),
                perm("View import returns", "/api/v1/import-return-orders", "GET", "IMPORT_RETURN_ORDERS"),
                perm("View import return by id", "/api/v1/import-return-orders/{id}", "GET", "IMPORT_RETURN_ORDERS"),
                // INVENTORY
                perm("View inventory logs", "/api/v1/inventory-logs", "GET", "INVENTORY"),
                perm("Create inventory log", "/api/v1/inventory-logs", "POST", "INVENTORY"),
                perm("View inventory by product", "/api/v1/inventory-logs/product/{productId}", "GET", "INVENTORY"),
                perm("Create inventory adjustment", "/api/v1/inventory-adjustments", "POST", "INVENTORY"),
                perm("View inventory adjustments", "/api/v1/inventory-adjustments", "GET", "INVENTORY"),
                perm("View inventory adjustment by id", "/api/v1/inventory-adjustments/{id}", "GET", "INVENTORY"),
                // USERS
                perm("View users", "/api/v1/users", "GET", "USERS"),
                perm("View user by id", "/api/v1/users/{id}", "GET", "USERS"),
                perm("Create user", "/api/v1/users", "POST", "USERS"),
                perm("Update user", "/api/v1/users/{id}", "PUT", "USERS"),
                perm("Delete user", "/api/v1/users/{id}", "DELETE", "USERS"),
                // ROLES
                perm("View roles", "/api/v1/roles", "GET", "ROLES"),
                perm("View role by id", "/api/v1/roles/{id}", "GET", "ROLES"),
                perm("Create role", "/api/v1/roles", "POST", "ROLES"),
                perm("Update role", "/api/v1/roles/{id}", "PUT", "ROLES"),
                perm("Delete role", "/api/v1/roles/{id}", "DELETE", "ROLES"),
                // PERMISSIONS
                perm("View permissions", "/api/v1/permissions", "GET", "PERMISSIONS"),
                perm("View all permissions", "/api/v1/permissions/all", "GET", "PERMISSIONS"),
                perm("View permission by id", "/api/v1/permissions/{id}", "GET", "PERMISSIONS"),
                perm("Create permission", "/api/v1/permissions", "POST", "PERMISSIONS"),
                perm("Update permission", "/api/v1/permissions/{id}", "PUT", "PERMISSIONS"),
                perm("Delete permission", "/api/v1/permissions/{id}", "DELETE", "PERMISSIONS"),
                // STORE INVITATIONS
                perm("Create store invitation", "/api/v1/store-invitations", "POST", "STORE_INVITATIONS"),
                perm("Accept store invitation", "/api/v1/store-invitations/{id}/accept", "POST", "STORE_INVITATIONS"),
                perm("Decline store invitation", "/api/v1/store-invitations/{id}/decline", "POST", "STORE_INVITATIONS"),
                // NOTIFICATIONS
                perm("View notifications", "/api/v1/notifications", "GET", "NOTIFICATIONS"),
                perm("Mark notification read", "/api/v1/notifications/{id}/read", "PATCH", "NOTIFICATIONS"),
                // FUND ACCOUNTS
                perm("Create fund account", "/api/v1/fund-accounts", "POST", "FUND_ACCOUNTS"),
                perm("View fund accounts", "/api/v1/fund-accounts", "GET", "FUND_ACCOUNTS"),
                perm("View active fund accounts", "/api/v1/fund-accounts/active", "GET", "FUND_ACCOUNTS"),
                perm("View fund account by id", "/api/v1/fund-accounts/{id}", "GET", "FUND_ACCOUNTS"),
                perm("Deactivate fund account", "/api/v1/fund-accounts/{id}/deactivate", "PATCH", "FUND_ACCOUNTS")
        );

        List<Permission> allPermissions = new ArrayList<>();
        int created = 0;
        for (Permission desired : desiredPermissions) {
            var existing = permissionRepository.findByApiPathAndMethod(desired.getApiPath(), desired.getMethod());
            Permission persisted = existing.isPresent() ? existing.get() : permissionRepository.save(desired);
            if (existing.isEmpty()) created++;
            allPermissions.add(persisted);
        }
        if (created > 0) log.info("Added {} new permissions", created);

        // ORDER_STAFF
        List<Permission> orderStaff = allPermissions.stream()
                .filter(p -> p.getApiPath().startsWith("/api/v1/auth")
                        || ("GET".equals(p.getMethod()) && (p.getApiPath().startsWith("/api/v1/products") || p.getApiPath().startsWith("/api/v1/categories") || p.getApiPath().startsWith("/api/v1/units")))
                        || p.getApiPath().startsWith("/api/v1/sales/init")
                        || (p.getApiPath().startsWith("/api/v1/orders") && ("GET".equals(p.getMethod()) || "POST".equals(p.getMethod())))
                        || ("GET".equals(p.getMethod()) && p.getApiPath().startsWith("/api/v1/customers"))
                        || ("GET".equals(p.getMethod()) && (p.getApiPath().equals("/api/v1/roster/day") || p.getApiPath().equals("/api/v1/roster/month")))
                        || ("GET".equals(p.getMethod()) && p.getApiPath().equals("/api/v1/payrolls/me"))
                        || ("GET".equals(p.getMethod()) && p.getApiPath().startsWith("/api/v1/employee-salaries/me"))
                        || p.getApiPath().contains("/attendance/check-")
                        || p.getApiPath().startsWith("/api/v1/attendance/me")
                        || p.getApiPath().startsWith("/api/v1/dashboard"))
                .collect(java.util.stream.Collectors.toCollection(ArrayList::new));
        ensureRole("ORDER_STAFF", "Nhan vien ghi don - tao don, xem san pham", orderStaff);

        // CASHIER
        List<Permission> cashier = allPermissions.stream()
                .filter(p -> p.getApiPath().startsWith("/api/v1/auth")
                        || ("GET".equals(p.getMethod()) && (p.getApiPath().startsWith("/api/v1/products") || p.getApiPath().startsWith("/api/v1/categories") || p.getApiPath().startsWith("/api/v1/units")))
                        || p.getApiPath().startsWith("/api/v1/sales/init")
                        || p.getApiPath().startsWith("/api/v1/orders")
                        || p.getApiPath().startsWith("/api/v1/payment")
                        || p.getApiPath().startsWith("/api/v1/transactions")
                        || (p.getApiPath().startsWith("/api/v1/customers") && ("GET".equals(p.getMethod()) || "POST".equals(p.getMethod())))
                        || ("GET".equals(p.getMethod()) && (p.getApiPath().equals("/api/v1/roster/day") || p.getApiPath().equals("/api/v1/roster/month")))
                        || ("GET".equals(p.getMethod()) && p.getApiPath().equals("/api/v1/payrolls/me"))
                        || ("GET".equals(p.getMethod()) && p.getApiPath().startsWith("/api/v1/employee-salaries/me"))
                        || p.getApiPath().contains("/attendance/check-")
                        || p.getApiPath().startsWith("/api/v1/attendance/me")
                        || p.getApiPath().startsWith("/api/v1/dashboard"))
                .collect(java.util.stream.Collectors.toCollection(ArrayList::new));
        ensureRole("CASHIER", "Nhan vien thu ngan - thanh toan, quan ly quy", cashier);

        // WAREHOUSE
        List<Permission> warehouse = allPermissions.stream()
                .filter(p -> p.getApiPath().startsWith("/api/v1/auth")
                        || p.getApiPath().startsWith("/api/v1/products")
                        || ("GET".equals(p.getMethod()) && (p.getApiPath().startsWith("/api/v1/categories") || p.getApiPath().startsWith("/api/v1/units")))
                        || p.getApiPath().startsWith("/api/v1/import-orders")
                        || p.getApiPath().startsWith("/api/v1/import-return-orders")
                        || p.getApiPath().startsWith("/api/v1/inventory")
                        || p.getApiPath().startsWith("/api/v1/suppliers")
                        || ("GET".equals(p.getMethod()) && (p.getApiPath().equals("/api/v1/roster/day") || p.getApiPath().equals("/api/v1/roster/month")))
                        || ("GET".equals(p.getMethod()) && p.getApiPath().equals("/api/v1/payrolls/me"))
                        || ("GET".equals(p.getMethod()) && p.getApiPath().startsWith("/api/v1/employee-salaries/me"))
                        || p.getApiPath().contains("/attendance/check-")
                        || p.getApiPath().startsWith("/api/v1/attendance/me")
                        || p.getApiPath().startsWith("/api/v1/dashboard"))
                .collect(java.util.stream.Collectors.toCollection(ArrayList::new));
        ensureRole("WAREHOUSE", "Nhan vien kho - kiem kho, nhap xuat hang", warehouse);

        // STORE_MANAGER
        ensureRole("STORE_MANAGER", "Quan ly cua hang - toan quyen truy cap", new ArrayList<>(allPermissions));
    }

    private void seedAdminUser() {
        if (userRepository.existsByUsername("1") || userRepository.existsByPhone("1")) {
            userRepository.findByUsername("1").ifPresent(this::ensureDefaultStoreForUser);
            return;
        }

        User admin = User.builder()
                .username("1")
                .phone("1")
                .password(passwordEncoder.encode("1"))
                .isActive(true)
                .build();
        admin = userRepository.save(admin);
        ensureDefaultStoreForUser(admin);
        log.info("Created default account: username=1 / password=1 (role=STORE_MANAGER)");
    }

    private Store ensureDefaultStoreForUser(User user) {
        var memberships = storeMemberRepository.findAllByUserIdAndStatusFetchStore(
                user.getId(), com.quyen.shoplite.util.constant.StoreMemberStatus.ACTIVE);
        if (!memberships.isEmpty()) {
            return memberships.get(0).getStore();
        }

        Store store = Store.builder()
                .name("Default Store")
                .owner(user)
                .isActive(true)
                .createdAt(LocalDateTime.now())
                .build();
        store = storeRepository.save(store);

        Role managerRole = roleRepository.findByName("STORE_MANAGER").orElse(null);
        storeMemberRepository.save(StoreMember.builder()
                .store(store)
                .user(user)
                .role(managerRole)
                .status(com.quyen.shoplite.util.constant.StoreMemberStatus.ACTIVE)
                .joinedAt(LocalDateTime.now())
                .build());
        return store;
    }

    private void seedEmployees() {
        if (employeeRepository.count() > 0) return;

        Store defaultStore = userRepository.findByUsername("1")
                .map(this::ensureDefaultStoreForUser)
                .orElse(null);

        Office office = officeRepository.findAll().stream().findFirst().orElse(null);
        if (office == null) {
            office = Office.builder()
                    .store(defaultStore)
                    .name("Chi nhanh chinh")
                    .officeLat(new BigDecimal("10.77620900"))
                    .officeLng(new BigDecimal("106.70076200"))
                    .radius(200)
                    .build();
            office = officeRepository.save(office);
            log.info("Created default office: {}", office.getName());
        }

        // {username, phone, roleName}
        String[][] samples = {
                {"Nguyen Van A", "0901234567", "STORE_MANAGER"},
                {"Tran Thi B",   "0919876543", "ORDER_STAFF"},
                {"Le Van C",     "0987654321", "WAREHOUSE"},
                {"Pham Thi D",   "0933334444", "CASHIER"},
                {"Hoang Van E",  "0971112222", "ORDER_STAFF"},
        };

        int count = 0;
        for (String[] s : samples) {
            String username = s[0];
            String phone    = s[1];
            String roleName = s[2];

            if (userRepository.existsByUsername(username)) continue;

            Role role = roleRepository.findByName(roleName).orElse(null);

            User user = User.builder()
                    .username(username)
                    .phone(phone)
                    .password(passwordEncoder.encode("123456"))
                    .isActive(true)
                    .build();
            user = userRepository.save(user);

            StoreMember storeMember = StoreMember.builder()
                    .store(defaultStore)
                    .user(user)
                    .role(role)
                    .status(com.quyen.shoplite.util.constant.StoreMemberStatus.ACTIVE)
                    .joinedAt(LocalDateTime.now())
                    .build();
            storeMemberRepository.save(storeMember);

            Employee employee = Employee.builder()
                    .storeMember(storeMember)
                    .store(defaultStore)
                    .office(office)
                    .salaryRate(0.0)
                    .build();
            employeeRepository.save(employee);
            count++;
        }

        if (count > 0) log.info("Created {} sample employees (default password: 123456)", count);
    }

    private void seedFundAccounts() {
        if (fundAccountRepository.count() > 0) return;

        Store defaultStore = userRepository.findByUsername("1")
                .map(this::ensureDefaultStoreForUser)
                .orElse(null);

        fundAccountRepository.save(FundAccount.builder()
                .store(defaultStore)
                .name("Tien mat tai quay")
                .type(com.quyen.shoplite.util.constant.FundTypeEnum.CASH)
                .openingBalance(BigDecimal.ZERO)
                .balance(BigDecimal.ZERO)
                .build());

        fundAccountRepository.save(FundAccount.builder()
                .store(defaultStore)
                .name("Nam A Bank - Tai khoan ngan hang")
                .type(com.quyen.shoplite.util.constant.FundTypeEnum.BANK)
                .openingBalance(BigDecimal.ZERO)
                .balance(BigDecimal.ZERO)
                .build());

        fundAccountRepository.save(FundAccount.builder()
                .store(defaultStore)
                .name("MoMo - Vi dien tu")
                .type(com.quyen.shoplite.util.constant.FundTypeEnum.EWALLET)
                .openingBalance(BigDecimal.ZERO)
                .balance(BigDecimal.ZERO)
                .build());

        log.info("Created 3 default fund accounts: Cash, Bank, E-wallet");
    }

    private void seedMenus() {
        ensureMenu("MENU_HOME", "Tong quan", "/home", "homeOutline", MenuType.TAB, null, 10);
        ensureMenu("MENU_PRODUCTS", "Hang hoa", "/products", "gridOutline", MenuType.TAB, null, 20,
                permission("/api/v1/products", "GET"));
        ensureMenu("MENU_SALES", "Ban hang", "/sales", "storefrontOutline", MenuType.TAB, null, 30,
                permission("/api/v1/orders", "POST"));
        ensureMenu("MENU_ORDERS", "Hoa don", "/orders", "receiptOutline", MenuType.TAB, null, 40,
                permission("/api/v1/orders", "GET"));
        Menu more = ensureMenu("MENU_MORE", "Khac", "/more", "reorderThreeOutline", MenuType.TAB, null, 50);

        ensureMenu("SHORTCUT_SALES", "Tao don", "/sales", "cartOutline", MenuType.SHORTCUT, null, 110,
                permission("/api/v1/orders", "POST"));
        ensureMenu("SHORTCUT_PRODUCTS", "San pham", "/products", "cubeOutline", MenuType.SHORTCUT, null, 120,
                permission("/api/v1/products", "GET"));
        ensureMenu("SHORTCUT_EMPLOYEES", "Nhan vien", "/employees", "peopleOutline", MenuType.SHORTCUT, null, 130,
                permission("/api/v1/employees", "GET"));
        ensureMenu("SHORTCUT_IMPORT_ORDERS", "Nhap hang", "/import-orders", "logInOutline", MenuType.SHORTCUT, null, 140,
                permission("/api/v1/import-orders", "GET"));

        Menu transactionGroup = ensureMenu("GROUP_TRANSACTION", "Giao dich", null, "appsOutline", MenuType.GROUP, more, 210);
        ensureMenu("ITEM_SALES", "Ban hang", "/sales", "bagHandleOutline", MenuType.ITEM, transactionGroup, 211,
                permission("/api/v1/orders", "POST"));
        ensureMenu("ITEM_ORDERS", "Hoa don", "/orders", "receiptOutline", MenuType.ITEM, transactionGroup, 212,
                permission("/api/v1/orders", "GET"));
        ensureMenu("ITEM_ORDER_CREATE", "Dat hang", "/orders/new", "bagOutline", MenuType.ITEM, transactionGroup, 213,
                permission("/api/v1/orders", "POST"));
        ensureMenu("ITEM_FUND_LEDGER", "So quy", "/fund-ledger", "walletOutline", MenuType.ITEM, transactionGroup, 214,
                permission("/api/v1/fund-accounts", "GET"));

        Menu productGroup = ensureMenu("GROUP_PRODUCTS", "Hang hoa", null, "archiveOutline", MenuType.GROUP, more, 220);
        ensureMenu("ITEM_PRODUCTS", "Hang hoa", "/products", "archiveOutline", MenuType.ITEM, productGroup, 221,
                permission("/api/v1/products", "GET"));
        ensureMenu("ITEM_INVENTORY_ADJUSTMENTS", "Kiem kho", "/inventory-adjustments", "clipboardOutline", MenuType.ITEM, productGroup, 222,
                permission("/api/v1/inventory-adjustments", "GET"));
        ensureMenu("ITEM_IMPORT_ORDERS", "Nhap hang", "/import-orders", "cartOutline", MenuType.ITEM, productGroup, 223,
                permission("/api/v1/import-orders", "GET"));
        ensureMenu("ITEM_IMPORT_RETURN_ORDERS", "Tra hang nhap", "/import-return-orders", "arrowUndoOutline", MenuType.ITEM, productGroup, 224,
                permission("/api/v1/import-return-orders", "GET"));

        Menu partnerGroup = ensureMenu("GROUP_PARTNERS", "Doi tac", null, "peopleOutline", MenuType.GROUP, more, 230);
        ensureMenu("ITEM_CUSTOMERS", "Khach hang", "/customers", "personOutline", MenuType.ITEM, partnerGroup, 231,
                permission("/api/v1/customers", "GET"));
        ensureMenu("ITEM_SUPPLIERS", "Nha cung cap", "/suppliers", "storefrontOutline", MenuType.ITEM, partnerGroup, 232,
                permission("/api/v1/suppliers", "GET"));

        Menu employeeGroup = ensureMenu("GROUP_EMPLOYEES", "Nhan vien", null, "peopleOutline", MenuType.GROUP, more, 240);
        ensureMenu("ITEM_EMPLOYEES", "Nhan vien", "/employees", "peopleOutline", MenuType.ITEM, employeeGroup, 241,
                permission("/api/v1/employees", "GET"));
        ensureMenu("ITEM_ROSTER", "Lich lam viec", null, "timeOutline", MenuType.ITEM, employeeGroup, 242,
                permission("/api/v1/roster/day", "GET"));
        ensureMenu("ITEM_ATTENDANCE", "Cham cong", null, "calendarOutline", MenuType.ITEM, employeeGroup, 243,
                permission("/api/v1/attendance", "GET"));
        ensureMenu("ITEM_PAYROLLS", "Bang luong", null, "documentTextOutline", MenuType.ITEM, employeeGroup, 244,
                permission("/api/v1/payrolls", "GET"),
                permission("/api/v1/payrolls/me", "GET"));

        Menu settingsGroup = ensureMenu("GROUP_SETTINGS", "Cai dat chung", null, "settingsOutline", MenuType.GROUP, more, 250);
        ensureMenu("ITEM_ROLE_MANAGEMENT", "Van phong", "/offices", "businessOutline", MenuType.ITEM, settingsGroup, 251,
                permission("/api/v1/offices", "GET"));
    }

    // ------------------------------------------------------------------ helpers

    private Permission perm(String name, String apiPath, String method, String module) {
        return Permission.builder()
                .name(name)
                .apiPath(apiPath)
                .method(method)
                .module(module)
                .createdAt(LocalDateTime.now())
                .build();
    }

    private Permission permission(String apiPath, String method) {
        return permissionRepository.findByApiPathAndMethod(apiPath, method).orElse(null);
    }

    private Menu ensureMenu(String code, String title, String route, String icon, MenuType menuType,
                            Menu parent, int sortOrder, Permission... permissions) {
        Menu menu = menuRepository.findByCode(code).orElse(null);
        LocalDateTime now = LocalDateTime.now();

        if (menu == null) {
            menu = Menu.builder()
                    .code(code)
                    .title(title)
                    .route(route)
                    .icon(icon)
                    .menuType(menuType)
                    .parent(parent)
                    .sortOrder(sortOrder)
                    .active(true)
                    .deleted(false)
                    .createdAt(now)
                    .updatedAt(now)
                    .permissions(new ArrayList<>())
                    .build();
        } else {
            menu.setTitle(title);
            menu.setRoute(route);
            menu.setIcon(icon);
            menu.setMenuType(menuType);
            menu.setParent(parent);
            menu.setSortOrder(sortOrder);
            menu.setActive(true);
            menu.setDeleted(false);
            menu.setUpdatedAt(now);
        }

        List<Permission> desiredPermissions = Arrays.stream(permissions)
                .filter(Objects::nonNull)
                .toList();
        menu.getPermissions().clear();
        menu.getPermissions().addAll(desiredPermissions);
        return menuRepository.save(menu);
    }

    private List<Permission> mergePermissions(List<Permission> existing, List<Permission> toAdd) {
        Map<String, Permission> merged = new LinkedHashMap<>();
        if (existing != null) existing.forEach(p -> merged.put(p.getApiPath() + "#" + p.getMethod(), p));
        if (toAdd != null) toAdd.forEach(p -> merged.put(p.getApiPath() + "#" + p.getMethod(), p));
        return new ArrayList<>(merged.values());
    }

    private void ensureRole(String name, String description, List<Permission> desired) {
        Role role = roleRepository.findByName(name).orElse(null);
        if (role == null) {
            role = Role.builder()
                    .name(name)
                    .description(description)
                    .active(true)
                    .permissions(desired)
                    .createdAt(LocalDateTime.now())
                    .version(0)
                    .build();
            roleRepository.save(role);
            log.info("Created role: {}", name);
        } else {
            Set<String> existingKeys = role.getPermissions().stream()
                    .map(p -> p.getApiPath() + "#" + p.getMethod())
                    .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
            Set<String> desiredKeys = desired.stream()
                    .map(p -> p.getApiPath() + "#" + p.getMethod())
                    .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));

            if (!existingKeys.equals(desiredKeys)) {
                role.getPermissions().clear();
                role.getPermissions().addAll(desired);
                roleRepository.save(role);
                log.info("Synced permissions for role: {}", name);
            }
        }
    }
}
