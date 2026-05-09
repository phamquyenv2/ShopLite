package com.quyen.shoplite.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.quyen.shoplite.domain.Employee;
import com.quyen.shoplite.domain.Office;
import com.quyen.shoplite.domain.Store;
import com.quyen.shoplite.domain.StoreMember;
import com.quyen.shoplite.domain.User;
import com.quyen.shoplite.domain.request.ReqEmployeeDTO;
import com.quyen.shoplite.repository.EmployeeRepository;
import com.quyen.shoplite.repository.OfficeRepository;
import com.quyen.shoplite.repository.StoreMemberRepository;
import com.quyen.shoplite.repository.StoreRepository;
import com.quyen.shoplite.repository.UserRepository;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@WithMockUser
class EmployeeControllerIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @Autowired private EmployeeRepository employeeRepository;
    @Autowired private UserRepository     userRepository;
    @Autowired private OfficeRepository   officeRepository;
    @Autowired private StoreRepository storeRepository;
    @Autowired private StoreMemberRepository storeMemberRepository;

    // IDs shared across tests within the same @BeforeEach scope
    private Integer itUserId;
    private Integer itOfficeId;
    private Long itStoreId;

    // ---------------------------------------------------------------- setup

    @BeforeEach
    void setup() {
        User user = new User();
        user.setUsername("emp_it_user_" + System.nanoTime());
        user.setPassword("secret");
        user.setActive(true);
        user = userRepository.save(user);
        itUserId = user.getId();
        Store store = Store.builder()
                .name("Employee IT Store " + System.nanoTime())
                .owner(user)
                .build();
        store = storeRepository.save(store);
        itStoreId = store.getId();

        Office office = new Office();
        office.setStore(store);
        office.setName("IT Office " + System.nanoTime());
        office.setOfficeLat(new BigDecimal("10.77609800"));
        office.setOfficeLng(new BigDecimal("106.70081500"));
        office.setRadius(200);
        office = officeRepository.save(office);
        itOfficeId = office.getId();
    }

    // ---------------------------------------------------------------- helpers

    private ReqEmployeeDTO validRequest() {
        ReqEmployeeDTO req = new ReqEmployeeDTO();
        req.setUserId(itUserId);
        req.setOfficeId(itOfficeId);
        req.setSalaryRate(80.0);
        req.setQr("QR-IT-" + System.nanoTime());
        req.setNote("integration test note");
        return req;
    }

    private Employee savedEmployee(Integer userId, Integer officeId, double salary, String qr) {
        User user     = userRepository.findById(userId).orElseThrow();
        Office office = officeRepository.findById(officeId).orElseThrow();
        Store store = storeRepository.findById(itStoreId).orElseThrow();
        StoreMember member = storeMemberRepository.findByStoreIdAndUserId(store.getId(), user.getId())
                .orElseGet(() -> storeMemberRepository.save(StoreMember.builder()
                        .store(store)
                        .user(user)
                        .build()));

        Employee emp = new Employee();
        emp.setStore(store);
        emp.setStoreMember(member);
        emp.setOffice(office);
        emp.setSalaryRate(salary);
        emp.setQr(qr);
        emp.setNote("pre-seeded note");
        return employeeRepository.save(emp);
    }

    // ==========================================================================
    // POST /api/v1/employees
    // ==========================================================================

    @Test
    @DisplayName("POST /employees – create employee success")
    void createEmployee_Success() throws Exception {
        ReqEmployeeDTO req = validRequest();

        mockMvc.perform(post("/api/v1/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.statusCode").value(201))
                .andExpect(jsonPath("$.data.salaryRate").value(80.0))
                .andExpect(jsonPath("$.data.userId").value(itUserId))
                .andExpect(jsonPath("$.data.officeId").value(itOfficeId))
                .andExpect(jsonPath("$.data.note").value("integration test note"))
                .andExpect(jsonPath("$.data.id").isNumber());

        // Verify persisted in DB
        assertThat(employeeRepository.existsByStoreMember_User_Id(itUserId)).isTrue();
    }

    @Test
    @DisplayName("POST /employees – missing user → 404 with error body")
    void createEmployee_UserNotFound_Failure() throws Exception {
        ReqEmployeeDTO req = validRequest();
        req.setUserId(99999); // non-existent

        mockMvc.perform(post("/api/v1/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.statusCode").value(404))
                .andExpect(jsonPath("$.message").value("User not found with id=99999"))
                .andExpect(jsonPath("$.data").isEmpty());
    }

    @Test
    @DisplayName("POST /employees – missing office → 404 with error body")
    void createEmployee_OfficeNotFound_Failure() throws Exception {
        ReqEmployeeDTO req = validRequest();
        req.setOfficeId(99999); // non-existent

        mockMvc.perform(post("/api/v1/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.statusCode").value(404))
                .andExpect(jsonPath("$.message").value("Office not found with id=99999"))
                .andExpect(jsonPath("$.data").isEmpty());
    }

    @Test
    @DisplayName("POST /employees – null salaryRate → 400 with validation errors array")
    void createEmployee_NullSalaryRate_Failure() throws Exception {
        ReqEmployeeDTO req = validRequest();
        req.setSalaryRate(null);

        mockMvc.perform(post("/api/v1/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.statusCode").value(400))
                .andExpect(jsonPath("$.errors").isArray());
    }

    @Test
    @DisplayName("POST /employees – negative salaryRate fails @Min(0) → 400 with errors array")
    void createEmployee_NegativeSalaryRate_Failure() throws Exception {
        ReqEmployeeDTO req = validRequest();
        req.setSalaryRate(-5.0);

        mockMvc.perform(post("/api/v1/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.statusCode").value(400))
                .andExpect(jsonPath("$.errors").isArray())
                .andExpect(jsonPath("$.errors[0].field").value("salaryRate"));
    }

    @Test
    @DisplayName("POST /employees – null userId → 400 with validation errors array")
    void createEmployee_NullUserId_Failure() throws Exception {
        ReqEmployeeDTO req = validRequest();
        req.setUserId(null);

        mockMvc.perform(post("/api/v1/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.statusCode").value(400))
                .andExpect(jsonPath("$.errors").isArray());
    }

    @Test
    @DisplayName("POST /employees – null officeId → 400 with validation errors array")
    void createEmployee_NullOfficeId_Failure() throws Exception {
        ReqEmployeeDTO req = validRequest();
        req.setOfficeId(null);

        mockMvc.perform(post("/api/v1/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.statusCode").value(400))
                .andExpect(jsonPath("$.errors").isArray());
    }

    @Test
    @DisplayName("POST /employees – duplicate user mapping → 400 with error message")
    void createEmployee_DuplicateUserMapping_Failure() throws Exception {
        // Seed: first employee bound to itUserId
        savedEmployee(itUserId, itOfficeId, 50.0, "QR-SEED-" + System.nanoTime());

        // Second create for the same user
        ReqEmployeeDTO req = validRequest();

        mockMvc.perform(post("/api/v1/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.statusCode").value(400))
                .andExpect(jsonPath("$.message").value(
                        "User id=" + itUserId + " is already linked to an employee"));
    }

    @Test
    @DisplayName("POST /employees – duplicate QR code → 400 with error message")
    void createEmployee_DuplicateQr_Failure() throws Exception {
        // Seed: another user bound to the same QR
        User anotherUser = new User();
        anotherUser.setUsername("other_" + System.nanoTime());
        anotherUser.setPassword("secret");
        anotherUser.setActive(true);
        anotherUser = userRepository.save(anotherUser);

        savedEmployee(anotherUser.getId(), itOfficeId, 40.0, "FIXED-QR");

        // Try to create with the same QR for a different user
        ReqEmployeeDTO req = validRequest();
        req.setQr("FIXED-QR");

        mockMvc.perform(post("/api/v1/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.statusCode").value(400))
                .andExpect(jsonPath("$.message").value("qr_code already exists: FIXED-QR"));
    }

    // ==========================================================================
    // GET /api/v1/employees/{id}
    // ==========================================================================

    @Test
    @DisplayName("GET /employees/{id} – success returns full DTO")
    void getEmployeeById_Success() throws Exception {
        Employee emp = savedEmployee(itUserId, itOfficeId, 120.0, "QR-GET-" + System.nanoTime());

        mockMvc.perform(get("/api/v1/employees/" + emp.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.data.id").value(emp.getId()))
                .andExpect(jsonPath("$.data.salaryRate").value(120.0))
                .andExpect(jsonPath("$.data.userId").value(itUserId))
                .andExpect(jsonPath("$.data.officeId").value(itOfficeId))
                .andExpect(jsonPath("$.data.note").value("pre-seeded note"));
    }

    @Test
    @DisplayName("GET /employees/{id} – not found → 404 with error body")
    void getEmployeeById_NotFound_Failure() throws Exception {
        mockMvc.perform(get("/api/v1/employees/99999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.statusCode").value(404))
                .andExpect(jsonPath("$.message").value("Employee not found with id=99999"))
                .andExpect(jsonPath("$.data").isEmpty());
    }

    // ==========================================================================
    // GET /api/v1/employees
    // ==========================================================================

    @Test
    @DisplayName("GET /employees – returns list with seeded employee")
    void getAllEmployees_Success() throws Exception {
        savedEmployee(itUserId, itOfficeId, 60.0, "QR-LIST-" + System.nanoTime());

        mockMvc.perform(get("/api/v1/employees"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(
                        org.hamcrest.Matchers.greaterThanOrEqualTo(1)));
    }

    // ==========================================================================
    // PUT /api/v1/employees/{id}
    // ==========================================================================

    @Test
    @DisplayName("PUT /employees/{id} – update salary, note, and QR – success")
    void updateEmployee_Success() throws Exception {
        Employee emp = savedEmployee(itUserId, itOfficeId, 100.0, "QR-OLD-" + System.nanoTime());

        ReqEmployeeDTO req = new ReqEmployeeDTO();
        req.setUserId(itUserId);          // same user
        req.setOfficeId(itOfficeId);      // same office
        req.setSalaryRate(200.0);
        req.setQr("QR-UPDATED");
        req.setNote("updated note");

        mockMvc.perform(put("/api/v1/employees/" + emp.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.data.salaryRate").value(200.0))
                .andExpect(jsonPath("$.data.qr").value("QR-UPDATED"))
                .andExpect(jsonPath("$.data.note").value("updated note"));

        // DB-level assertion
        Employee updated = employeeRepository.findById(emp.getId()).orElseThrow();
        assertThat(updated.getSalaryRate()).isEqualTo(200.0);
        assertThat(updated.getQr()).isEqualTo("QR-UPDATED");
        assertThat(updated.getNote()).isEqualTo("updated note");
    }

    @Test
    @DisplayName("PUT /employees/{id} – employee not found → 404")
    void updateEmployee_NotFound_Failure() throws Exception {
        ReqEmployeeDTO req = validRequest();

        mockMvc.perform(put("/api/v1/employees/99999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.statusCode").value(404))
                .andExpect(jsonPath("$.message").value("Employee not found with id=99999"));
    }

    @Test
    @DisplayName("PUT /employees/{id} – update fails with duplicate QR → 400")
    void updateEmployee_DuplicateQr_Failure() throws Exception {
        // Seed two employees; second update tries to steal first's QR
        User otherUser = new User();
        otherUser.setUsername("upd_other_" + System.nanoTime());
        otherUser.setPassword("secret");
        otherUser.setActive(true);
        otherUser = userRepository.save(otherUser);

        savedEmployee(otherUser.getId(), itOfficeId, 50.0, "TAKEN-QR");
        Employee target = savedEmployee(itUserId, itOfficeId, 80.0, "QR-TARGET-" + System.nanoTime());

        ReqEmployeeDTO req = new ReqEmployeeDTO();
        req.setUserId(itUserId);
        req.setOfficeId(itOfficeId);
        req.setSalaryRate(90.0);
        req.setQr("TAKEN-QR"); // conflicts with otherUser's employee

        mockMvc.perform(put("/api/v1/employees/" + target.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.statusCode").value(400))
                .andExpect(jsonPath("$.message").value("qr_code already exists: TAKEN-QR"));
    }

    // ==========================================================================
    // DELETE /api/v1/employees/{id}
    // ==========================================================================

    @Test
    @DisplayName("DELETE /employees/{id} – success returns 204 and soft-deletes employee")
    void deleteEmployee_Success() throws Exception {
        Employee emp = savedEmployee(itUserId, itOfficeId, 50.0, "QR-DEL-" + System.nanoTime());
        Integer empId = emp.getId();

        mockMvc.perform(delete("/api/v1/employees/" + empId))
                .andExpect(status().isNoContent());

        // Soft delete: record still in DB but with deleted=true
        assertThat(employeeRepository.findById(empId))
                .isPresent()
                .get()
                .extracting(Employee::isDeleted)
                .isEqualTo(true);
    }

    @Test
    @DisplayName("DELETE /employees/{id} – not found → 404 with error body")
    void deleteEmployee_NotFound_Failure() throws Exception {
        mockMvc.perform(delete("/api/v1/employees/99999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.statusCode").value(404))
                .andExpect(jsonPath("$.message").value("Employee not found with id=99999"));
    }
}
