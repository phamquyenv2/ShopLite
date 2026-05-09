package com.quyen.shoplite.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.quyen.shoplite.domain.*;
import com.quyen.shoplite.domain.request.ReqPayrollSyncDTO;
import com.quyen.shoplite.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@WithMockUser(username = "payroll_test_user")
class PayrollControllerIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @Autowired private UserRepository userRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private PermissionRepository permissionRepository;
    @Autowired private EmployeeRepository employeeRepository;
    @Autowired private OfficeRepository officeRepository;
    @Autowired private PayrollRepository payrollRepository;
    @Autowired private TransactionRepository transactionRepository;
    @Autowired private StoreRepository storeRepository;
    @Autowired private StoreMemberRepository storeMemberRepository;

    private Office testOffice;
    private Employee testEmployee;

    @BeforeEach
    void setup() {
        // 1. Setup Permissions
        Permission pSync = createPermission("Sync Payroll", "/api/v1/payrolls/sync-monthly", "POST");
        Permission pGetList = createPermission("Get List", "/api/v1/payrolls", "GET");
        Permission pGetById = createPermission("Get By Id", "/api/v1/payrolls/{id}", "GET");
        Permission pGetByEmp = createPermission("Get By Emp", "/api/v1/payrolls/employee/{employeeId}", "GET");

        // 2. Setup Role
        Role role = new Role();
        role.setName("ROLE_PAYROLL_TEST_" + System.nanoTime());
        role.setDescription("Payroll Testing Role");
        role.setActive(true);
        role.setPermissions(List.of(pSync, pGetList, pGetById, pGetByEmp));
        role = roleRepository.save(role);

        // 3. Setup User matching the @WithMockUser
        User user = new User();
        user.setUsername("payroll_test_user");
        user.setPassword("secret");
        user.setActive(true);
        user = userRepository.save(user);
        Store store = Store.builder()
                .name("Payroll Store " + System.nanoTime())
                .owner(user)
                .build();
        store = storeRepository.save(store);
        StoreMember member = StoreMember.builder()
                .store(store)
                .user(user)
                .role(role)
                .build();
        member = storeMemberRepository.save(member);

        // 4. Setup Office
        testOffice = new Office();
        testOffice.setStore(store);
        testOffice.setName("HQ Test Office " + System.nanoTime());
        testOffice.setOfficeLat(new BigDecimal("10.77609800"));
        testOffice.setOfficeLng(new BigDecimal("106.70081500"));
        testOffice.setRadius(200);
        testOffice.setLateGraceMinutes(15);
        testOffice = officeRepository.save(testOffice);

        // 5. Setup Employee mapped to User & Office
        testEmployee = new Employee();
        testEmployee.setStore(store);
        testEmployee.setStoreMember(member);
        testEmployee.setOffice(testOffice);
        testEmployee.setSalaryRate(100.0);
        testEmployee.setQr("QR-PAYROLL_" + System.nanoTime());
        testEmployee = employeeRepository.save(testEmployee);
    }

    private Permission createPermission(String name, String apiPath, String method) {
        Permission p = new Permission();
        p.setName(name);
        p.setApiPath(apiPath);
        p.setMethod(method);
        p.setModule("PAYROLL");
        return permissionRepository.save(p);
    }

    // =====================================================================================
    // SUCCESS CASES
    // =====================================================================================

    @Test
    @DisplayName("1) POST /sync-monthly - Success")
    void syncMonthly_Success() throws Exception {
        ReqPayrollSyncDTO req = new ReqPayrollSyncDTO();
        req.setPeriod(LocalDate.of(2025, 4, 15));
        req.setEmployeeId(testEmployee.getId());

        mockMvc.perform(post("/api/v1/payrolls/sync-monthly")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.message").value("Sync payroll success"))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].employeeId").value(testEmployee.getId()))
                .andExpect(jsonPath("$.data[0].period").value("2025-04-01"))
                .andExpect(jsonPath("$.data[0].totalSalary").value(0.0)); // No attendance = 0 total
    }

    @Test
    @DisplayName("2) GET /payrolls - Success")
    void getAllPayrolls_Success() throws Exception {
        Payroll payroll = Payroll.builder()
                .employee(testEmployee)
                .period(LocalDate.of(2025, 4, 1))
                .salaryRate(100.0)
                .totalHours(10.0)
                .totalSalary(1000.0)
                .build();
        payrollRepository.save(payroll);

        mockMvc.perform(get("/api/v1/payrolls"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.message").value("Get payrolls success"))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(org.hamcrest.Matchers.greaterThanOrEqualTo(1)));
    }

    @Test
    @DisplayName("3) GET /payrolls/{id} - Success")
    void getPayrollById_Success() throws Exception {
        Payroll payroll = Payroll.builder()
                .employee(testEmployee)
                .period(LocalDate.of(2025, 4, 1))
                .salaryRate(100.0)
                .totalHours(10.0)
                .totalSalary(1000.0)
                .build();
        payroll = payrollRepository.save(payroll);

        mockMvc.perform(get("/api/v1/payrolls/" + payroll.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.message").value("Get payroll by ID success"))
                .andExpect(jsonPath("$.data.employeeId").value(testEmployee.getId()));
    }

    @Test
    @DisplayName("4) GET /payrolls/employee/{employeeId} - Success")
    void getPayrollByEmployeeId_Success() throws Exception {
        Payroll payroll = Payroll.builder()
                .employee(testEmployee)
                .period(LocalDate.of(2025, 4, 1))
                .salaryRate(100.0)
                .totalHours(10.0)
                .totalSalary(1000.0)
                .build();
        payroll = payrollRepository.save(payroll);

        mockMvc.perform(get("/api/v1/payrolls/employee/" + testEmployee.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.data[0].employeeId").value(testEmployee.getId()));
    }

    // =====================================================================================
    // FAILURE CASES
    // =====================================================================================

    @Test
    @DisplayName("F1) POST /sync-monthly - Validation error on negative bonus/penalty")
    void syncMonthly_ValidationError() throws Exception {
        ReqPayrollSyncDTO req = new ReqPayrollSyncDTO();
        req.setPeriod(LocalDate.of(2025, 4, 15));
        req.setBonus(-100.0);
        req.setPenalty(-50.0);

        mockMvc.perform(post("/api/v1/payrolls/sync-monthly")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.statusCode").value(400));
    }

    @Test
    @DisplayName("F2) GET /payrolls/{id} - Not found handling")
    void getPayrollById_NotFound_Failure() throws Exception {
        mockMvc.perform(get("/api/v1/payrolls/999999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.statusCode").value(404));
    }
}
