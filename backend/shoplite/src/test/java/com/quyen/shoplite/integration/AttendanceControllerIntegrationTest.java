package com.quyen.shoplite.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.quyen.shoplite.domain.*;
import com.quyen.shoplite.domain.request.ReqAttendanceCheckInDTO;
import com.quyen.shoplite.domain.request.ReqAttendanceCheckOutDTO;
import com.quyen.shoplite.repository.*;
import com.quyen.shoplite.util.constant.AttendanceStatusEnum;
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
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@WithMockUser(username = "attendance_test_user")
class AttendanceControllerIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @Autowired private UserRepository userRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private PermissionRepository permissionRepository;
    @Autowired private EmployeeRepository employeeRepository;
    @Autowired private OfficeRepository officeRepository;
    @Autowired private AttendanceRepository attendanceRepository;

    private Office testOffice;
    private Employee testEmployee;

    @BeforeEach
    void setup() {
        // 1. Setup Permissions
        Permission pCheckIn = createPermission("Check In", "/api/v1/attendance/check-in", "POST");
        Permission pCheckOut = createPermission("Check Out", "/api/v1/attendance/check-out", "POST");
        Permission pGetList = createPermission("Get List", "/api/v1/attendance", "GET");
        Permission pGetById = createPermission("Get By Id", "/api/v1/attendance/{id}", "GET");

        // 2. Setup Role
        Role role = new Role();
        role.setName("ROLE_ATTENDANCE_TEST_" + System.nanoTime());
        role.setDescription("Attendance Testing Role");
        role.setActive(true);
        role.setPermissions(List.of(pCheckIn, pCheckOut, pGetList, pGetById));
        role = roleRepository.save(role);

        // 3. Setup User matching the @WithMockUser
        User user = new User();
        user.setUsername("attendance_test_user");
        user.setPassword("secret");
        user.setActive(true);
        user.setRole(role);
        user = userRepository.save(user);

        // 4. Setup Office
        testOffice = new Office();
        testOffice.setName("HQ Test Office " + System.nanoTime());
        testOffice.setOfficeLat(new BigDecimal("10.77609800")); // Tọa độ giả định
        testOffice.setOfficeLng(new BigDecimal("106.70081500"));
        testOffice.setRadius(200); // Bán kính văn phòng 200m
        testOffice.setLateGraceMinutes(15);
        testOffice = officeRepository.save(testOffice);

        // 5. Setup Employee mapped to User & Office
        testEmployee = new Employee();
        testEmployee.setUser(user);
        testEmployee.setOffice(testOffice);
        testEmployee.setSalaryRate(90.0);
        testEmployee.setQr("QR-ATT_" + System.nanoTime());
        testEmployee = employeeRepository.save(testEmployee);
    }

    private Permission createPermission(String name, String apiPath, String method) {
        Permission p = new Permission();
        p.setName(name);
        p.setApiPath(apiPath);
        p.setMethod(method);
        p.setModule("ATTENDANCE");
        return permissionRepository.save(p);
    }

    // =====================================================================================
    // SUCCESS CASES
    // =====================================================================================

    @Test
    @DisplayName("1 & 8) POST /check-in - Success within radius (Walk-in)")
    void checkIn_Success_WithinRadius() throws Exception {
        ReqAttendanceCheckInDTO req = new ReqAttendanceCheckInDTO();
        req.setLatitude(10.776100); // Gần văn phòng
        req.setLongitude(106.700820);
        req.setDeviceId("TEST-DEVICE-01");

        mockMvc.perform(post("/api/v1/attendance/check-in")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.statusCode").value(201))
                .andExpect(jsonPath("$.message").value("Check-in success"))
                .andExpect(jsonPath("$.data.status").value("VALID"))
                .andExpect(jsonPath("$.data.walkIn").value(true))
                .andExpect(jsonPath("$.data.distance").value(org.hamcrest.Matchers.lessThanOrEqualTo(200.0)))
                .andExpect(jsonPath("$.data.rosterId").isEmpty());
    }

    @Test
    @DisplayName("2) POST /check-in - Success out of zone")
    void checkIn_Success_OutOfZone() throws Exception {
        ReqAttendanceCheckInDTO req = new ReqAttendanceCheckInDTO();
        req.setLatitude(21.028511); // Tọa độ xa lạc (Hà Nội)
        req.setLongitude(105.804817);

        mockMvc.perform(post("/api/v1/attendance/check-in")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.statusCode").value(201))
                .andExpect(jsonPath("$.message").value("Check-in success"))
                .andExpect(jsonPath("$.data.status").value("OUT_OF_ZONE"))
                .andExpect(jsonPath("$.data.distance").value(org.hamcrest.Matchers.greaterThan(200.0)));
    }

    @Test
    @DisplayName("4) POST /check-in - Second shift check-in success after first shift checked out")
    void checkIn_SecondShiftSuccess() throws Exception {
        // Simulate first shift
        Attendance shift1 = Attendance.builder()
                .employee(testEmployee)
                .office(testOffice)
                .walkIn(true)
                .workingDay(LocalDate.now())
                .status(AttendanceStatusEnum.VALID)
                .checkIn(LocalDateTime.now().minusHours(4))
                .checkOut(LocalDateTime.now().minusHours(2)) // Already checked out
                .build();
        attendanceRepository.save(shift1);

        // Attempt new check-in shift
        ReqAttendanceCheckInDTO req = new ReqAttendanceCheckInDTO();
        req.setLatitude(10.776110);
        req.setLongitude(106.700810);

        mockMvc.perform(post("/api/v1/attendance/check-in")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.statusCode").value(201))
                .andExpect(jsonPath("$.message").value("Check-in success"));
    }

    @Test
    @DisplayName("5) POST /check-out - Success check-out")
    void checkOut_Success() throws Exception {
        // Tạo một buổi open từ 30 phút trước
        Attendance openAttendance = Attendance.builder()
                .employee(testEmployee)
                .office(testOffice)
                .walkIn(true)
                .workingDay(LocalDate.now())
                .status(AttendanceStatusEnum.VALID)
                .checkIn(LocalDateTime.now().minusMinutes(30))
                .build();
        attendanceRepository.save(openAttendance);

        ReqAttendanceCheckOutDTO req = new ReqAttendanceCheckOutDTO();
        req.setLatitude(10.776100);
        req.setLongitude(106.700810);

        mockMvc.perform(post("/api/v1/attendance/check-out")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.message").value("Check-out success"))
                .andExpect(jsonPath("$.data.checkOut").isNotEmpty())
                .andExpect(jsonPath("$.data.workedMinutes").value(greaterThan(0)))
                .andExpect(jsonPath("$.data.payableMinutes").value(greaterThan(0)))
                .andExpect(jsonPath("$.data.closedAutomatically").value(false));
    }

    @Test
    @DisplayName("6) GET /attendance - Retrieve list with newest records")
    void getAllAttendances_Success() throws Exception {
        Attendance attendance = Attendance.builder()
                .employee(testEmployee)
                .office(testOffice)
                .walkIn(true)
                .workingDay(LocalDate.now())
                .status(AttendanceStatusEnum.VALID)
                .checkIn(LocalDateTime.now().minusHours(1))
                .build();
        attendanceRepository.save(attendance);

        mockMvc.perform(get("/api/v1/attendance"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.message").value("Get attendance list success"))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(org.hamcrest.Matchers.greaterThanOrEqualTo(1)));
    }

    @Test
    @DisplayName("7) GET /attendance/{id} - Get single attendance by ID")
    void getAttendanceById_Success() throws Exception {
        Attendance attendance = Attendance.builder()
                .employee(testEmployee)
                .office(testOffice)
                .walkIn(true)
                .workingDay(LocalDate.now())
                .status(AttendanceStatusEnum.VALID)
                .checkIn(LocalDateTime.now())
                .build();
        attendance = attendanceRepository.save(attendance);

        mockMvc.perform(get("/api/v1/attendance/" + attendance.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.message").value("Get attendance success"))
                .andExpect(jsonPath("$.data.id").value(attendance.getId()));
    }

    // =====================================================================================
    // FAILURE CASES
    // =====================================================================================

    @Test
    @DisplayName("3) POST /check-in - Fails due to duplicate open shift")
    void checkIn_DuplicateOpenShift_Failure() throws Exception {
        // Seed an open shift
        Attendance openAttendance = Attendance.builder()
                .employee(testEmployee)
                .office(testOffice)
                .walkIn(true)
                .workingDay(LocalDate.now())
                .status(AttendanceStatusEnum.VALID)
                .checkIn(LocalDateTime.now())
                .build();
        attendanceRepository.save(openAttendance);

        ReqAttendanceCheckInDTO req = new ReqAttendanceCheckInDTO();
        req.setLatitude(10.776100);
        req.setLongitude(106.700810);

        mockMvc.perform(post("/api/v1/attendance/check-in")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.statusCode").value(400))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsStringIgnoringCase("open shift")))
                .andExpect(jsonPath("$.data").isEmpty());
    }

    @Test
    @DisplayName("F1) POST /check-out - Fails without an open attendance record")
    void checkOut_NoOpenShift_Failure() throws Exception {
        // Ensure NO open attendance (handled by @Transactional rollback + no seeds)
        ReqAttendanceCheckOutDTO req = new ReqAttendanceCheckOutDTO();
        req.setLatitude(10.776100);
        req.setLongitude(106.700810);

        mockMvc.perform(post("/api/v1/attendance/check-out")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.statusCode").value(400))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsStringIgnoringCase("no open shift")))
                .andExpect(jsonPath("$.data").isEmpty());
    }

    @Test
    @DisplayName("F2) GET /attendance/{id} - Not found handling")
    void getAttendanceById_NotFound_Failure() throws Exception {
        mockMvc.perform(get("/api/v1/attendance/99999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.statusCode").value(404))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsStringIgnoringCase("not found")));
    }
}
