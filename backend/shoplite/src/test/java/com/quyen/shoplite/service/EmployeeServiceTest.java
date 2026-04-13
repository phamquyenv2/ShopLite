package com.quyen.shoplite.service;

import com.quyen.shoplite.domain.Employee;
import com.quyen.shoplite.domain.Office;
import com.quyen.shoplite.domain.Role;
import com.quyen.shoplite.domain.User;
import com.quyen.shoplite.domain.request.ReqEmployeeDTO;
import com.quyen.shoplite.domain.response.ResEmployeeDTO;
import com.quyen.shoplite.repository.EmployeeRepository;
import com.quyen.shoplite.repository.OfficeRepository;
import com.quyen.shoplite.repository.UserRepository;
import com.quyen.shoplite.util.error.BadRequestException;
import com.quyen.shoplite.util.error.ResourceNotFoundException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmployeeServiceTest {

    // ------------------------------------------------------------------ mocks
    @Mock private EmployeeRepository employeeRepository;
    @Mock private UserRepository     userRepository;
    @Mock private OfficeRepository   officeRepository;

    @InjectMocks
    private EmployeeService service;

    private Validator validator;

    // ---------------------------------------------------------------- helpers
    @BeforeEach
    void setUp() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    private User makeUser(int id) {
        User u = new User();
        u.setId(id);
        u.setUsername("user" + id);
        u.setPassword("secret");
        u.setActive(true);
        return u;
    }

    private Office makeOffice(int id) {
        Office o = new Office();
        o.setId(id);
        o.setName("Office " + id);
        return o;
    }

    private Employee makeEmployee(int id, User user, Office office) {
        Employee e = new Employee();
        e.setId(id);
        e.setUser(user);
        e.setOffice(office);
        e.setSalaryRate(100.0);
        e.setQr("QR-" + id);
        e.setNote("note " + id);
        return e;
    }

    /** Fully valid create request */
    private ReqEmployeeDTO validCreateReq(int userId, int officeId) {
        ReqEmployeeDTO req = new ReqEmployeeDTO();
        req.setUserId(userId);
        req.setOfficeId(officeId);
        req.setSalaryRate(50.0);
        req.setQr("QR-NEW");
        req.setNote("test note");
        return req;
    }

    // ==========================================================================
    // CREATE — success
    // ==========================================================================
    @Nested
    @DisplayName("create() – success cases")
    class CreateSuccessTests {

        @Test
        @DisplayName("Employee created successfully – all fields mapped to DTO")
        void create_Success_AllFieldsMapped() {
            // Arrange
            User user     = makeUser(1);
            Office office = makeOffice(10);

            when(userRepository.findById(1)).thenReturn(Optional.of(user));
            when(employeeRepository.existsByUser_IdAndDeletedFalse(1)).thenReturn(false);
            when(officeRepository.findById(10)).thenReturn(Optional.of(office));
            when(employeeRepository.existsByQrAndDeletedFalse("QR-NEW")).thenReturn(false);
            when(employeeRepository.save(any(Employee.class)))
                    .thenAnswer(inv -> {
                        Employee e = inv.getArgument(0);
                        e.setId(99);
                        return e;
                    });

            ReqEmployeeDTO req = validCreateReq(1, 10);

            // Act
            ResEmployeeDTO result = service.create(req);

            // Assert – DTO fields
            assertNotNull(result);
            assertEquals(99,        result.getId());
            assertEquals(50.0,      result.getSalaryRate());
            assertEquals("QR-NEW",  result.getQr());
            assertEquals("test note", result.getNote());
            assertEquals(1,         result.getUserId());
            assertEquals("user1",   result.getUsername());
            assertEquals(10,        result.getOfficeId());
            assertEquals("Office 10", result.getOfficeName());

            verify(employeeRepository).save(any(Employee.class));
        }

        @Test
        @DisplayName("Employee created with zero salary rate – boundary allowed")
        void create_ZeroSalaryRate_Allowed() {
            // Arrange
            User user     = makeUser(2);
            Office office = makeOffice(5);

            when(userRepository.findById(2)).thenReturn(Optional.of(user));
            when(employeeRepository.existsByUser_IdAndDeletedFalse(2)).thenReturn(false);
            when(officeRepository.findById(5)).thenReturn(Optional.of(office));
            when(employeeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            ReqEmployeeDTO req = validCreateReq(2, 5);
            req.setSalaryRate(0.0);
            req.setQr(null); // no QR

            // Act & Assert
            assertDoesNotThrow(() -> service.create(req));
            verify(employeeRepository).save(any(Employee.class));
        }

        @Test
        @DisplayName("Employee created with null QR – no QR uniqueness check performed")
        void create_NullQr_SkipsUniquenessCheck() {
            // Arrange
            User user     = makeUser(3);
            Office office = makeOffice(7);

            when(userRepository.findById(3)).thenReturn(Optional.of(user));
            when(employeeRepository.existsByUser_IdAndDeletedFalse(3)).thenReturn(false);
            when(officeRepository.findById(7)).thenReturn(Optional.of(office));
            when(employeeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            ReqEmployeeDTO req = validCreateReq(3, 7);
            req.setQr(null);

            // Act
            service.create(req);

            // Assert – existsByQr never called
            verify(employeeRepository, never()).existsByQr(any());
            verify(employeeRepository).save(any(Employee.class));
        }

        @Test
        @DisplayName("Employee entity persisted with correct field values (ArgumentCaptor)")
        void create_PersistedEntityHasCorrectFields() {
            // Arrange
            User user     = makeUser(4);
            Office office = makeOffice(20);

            when(userRepository.findById(4)).thenReturn(Optional.of(user));
            when(employeeRepository.existsByUser_IdAndDeletedFalse(4)).thenReturn(false);
            when(officeRepository.findById(20)).thenReturn(Optional.of(office));
            when(employeeRepository.existsByQrAndDeletedFalse("ABC-123")).thenReturn(false);
            when(employeeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            ReqEmployeeDTO req = new ReqEmployeeDTO();
            req.setUserId(4);
            req.setOfficeId(20);
            req.setSalaryRate(75.5);
            req.setQr(" ABC-123 "); // leading/trailing whitespace should be trimmed
            req.setNote("special note");

            // Act
            service.create(req);

            // Assert via captor
            ArgumentCaptor<Employee> captor = ArgumentCaptor.forClass(Employee.class);
            verify(employeeRepository).save(captor.capture());
            Employee saved = captor.getValue();

            assertEquals(user,         saved.getUser());
            assertEquals(office,       saved.getOffice());
            assertEquals(75.5,         saved.getSalaryRate());
            assertEquals("ABC-123",    saved.getQr());   // trimmed
            assertEquals("special note", saved.getNote());
        }
    }

    // ==========================================================================
    // CREATE — failure
    // ==========================================================================
    @Nested
    @DisplayName("create() – failure cases")
    class CreateFailureTests {

        @Test
        @DisplayName("User not found → ResourceNotFoundException with userId in message")
        void create_UserNotFound_Throws() {
            when(userRepository.findById(99)).thenReturn(Optional.empty());

            ReqEmployeeDTO req = validCreateReq(99, 1);
            ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                    () -> service.create(req));

            assertTrue(ex.getMessage().contains("99"));
            verify(employeeRepository, never()).save(any());
        }

        @Test
        @DisplayName("Duplicate user-employee mapping → BadRequestException")
        void create_DuplicateUserMapping_Throws() {
            User user = makeUser(5);
            when(userRepository.findById(5)).thenReturn(Optional.of(user));
            when(employeeRepository.existsByUser_IdAndDeletedFalse(5)).thenReturn(true); // already mapped

            ReqEmployeeDTO req = validCreateReq(5, 1);
            BadRequestException ex = assertThrows(BadRequestException.class,
                    () -> service.create(req));

            assertTrue(ex.getMessage().contains("5"));
            verify(employeeRepository, never()).save(any());
        }

        @Test
        @DisplayName("Office not found → ResourceNotFoundException with officeId in message")
        void create_OfficeNotFound_Throws() {
            User user = makeUser(6);
            when(userRepository.findById(6)).thenReturn(Optional.of(user));
            when(employeeRepository.existsByUser_IdAndDeletedFalse(6)).thenReturn(false);
            when(officeRepository.findById(999)).thenReturn(Optional.empty());

            ReqEmployeeDTO req = validCreateReq(6, 999);
            ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                    () -> service.create(req));

            assertTrue(ex.getMessage().contains("999"));
            verify(employeeRepository, never()).save(any());
        }

        @Test
        @DisplayName("Negative salary rate → BadRequestException")
        void create_NegativeSalaryRate_Throws() {
            User user     = makeUser(7);
            Office office = makeOffice(1);

            when(userRepository.findById(7)).thenReturn(Optional.of(user));
            when(employeeRepository.existsByUser_IdAndDeletedFalse(7)).thenReturn(false);
            when(officeRepository.findById(1)).thenReturn(Optional.of(office));

            ReqEmployeeDTO req = validCreateReq(7, 1);
            req.setSalaryRate(-10.0);

            BadRequestException ex = assertThrows(BadRequestException.class,
                    () -> service.create(req));

            assertTrue(ex.getMessage().contains(">= 0"));
            verify(employeeRepository, never()).save(any());
        }

        @Test
        @DisplayName("Duplicate QR code → BadRequestException with qr in message")
        void create_DuplicateQr_Throws() {
            User user     = makeUser(8);
            Office office = makeOffice(2);

            when(userRepository.findById(8)).thenReturn(Optional.of(user));
            when(employeeRepository.existsByUser_IdAndDeletedFalse(8)).thenReturn(false);
            when(officeRepository.findById(2)).thenReturn(Optional.of(office));
            when(employeeRepository.existsByQrAndDeletedFalse("DUPE-QR")).thenReturn(true);

            ReqEmployeeDTO req = validCreateReq(8, 2);
            req.setQr("DUPE-QR");

            BadRequestException ex = assertThrows(BadRequestException.class,
                    () -> service.create(req));

            assertTrue(ex.getMessage().contains("DUPE-QR"));
            verify(employeeRepository, never()).save(any());
        }
    }

    // ==========================================================================
    // GET BY ID
    // ==========================================================================
    @Nested
    @DisplayName("findById()")
    class FindByIdTests {

        @Test
        @DisplayName("Success – returns fully mapped DTO")
        void findById_Success() {
            User user     = makeUser(1);
            Office office = makeOffice(10);
            Employee emp  = makeEmployee(1, user, office);

            when(employeeRepository.findById(1)).thenReturn(Optional.of(emp));

            ResEmployeeDTO result = service.findById(1);

            assertNotNull(result);
            assertEquals(1,          result.getId());
            assertEquals(100.0,      result.getSalaryRate());
            assertEquals("QR-1",     result.getQr());
            assertEquals("note 1",   result.getNote());
            assertEquals(1,          result.getUserId());
            assertEquals("user1",    result.getUsername());
            assertEquals(10,         result.getOfficeId());
            assertEquals("Office 10", result.getOfficeName());

            verify(employeeRepository).findById(1);
        }

        @Test
        @DisplayName("Employee not found → ResourceNotFoundException with id in message")
        void findById_NotFound_Throws() {
            when(employeeRepository.findById(404)).thenReturn(Optional.empty());

            ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                    () -> service.findById(404));

            assertTrue(ex.getMessage().contains("404"));
        }
    }

    // ==========================================================================
    // FIND ALL
    // ==========================================================================
    @Nested
    @DisplayName("findAll()")
    class FindAllTests {

        @Test
        @DisplayName("Success – all employees mapped and returned")
        void findAll_Success() {
            User u1 = makeUser(1);
            User u2 = makeUser(2);
            Office o = makeOffice(5);

            Employee e1 = makeEmployee(1, u1, o);
            Employee e2 = makeEmployee(2, u2, o);

            when(employeeRepository.findAllByDeletedFalseOrderByIdAsc()).thenReturn(List.of(e1, e2));

            List<ResEmployeeDTO> result = service.findAll();

            assertNotNull(result);
            assertEquals(2, result.size());
            assertEquals(1, result.get(0).getId());
            assertEquals(2, result.get(1).getId());

            verify(employeeRepository).findAllByDeletedFalseOrderByIdAsc();
        }

        @Test
        @DisplayName("Success – empty repository returns empty list")
        void findAll_Empty() {
            when(employeeRepository.findAllByDeletedFalseOrderByIdAsc()).thenReturn(List.of());

            List<ResEmployeeDTO> result = service.findAll();

            assertNotNull(result);
            assertTrue(result.isEmpty());
        }
    }

    // ==========================================================================
    // UPDATE — success
    // ==========================================================================
    @Nested
    @DisplayName("update() – success cases")
    class UpdateSuccessTests {

        @Test
        @DisplayName("Update salary, note, and QR – same user & office – success")
        void update_SalaryNoteQr_SameUserOffice_Success() {
            // Arrange
            User user     = makeUser(1);
            Office office = makeOffice(10);
            Employee emp  = makeEmployee(1, user, office);

            when(employeeRepository.findById(1)).thenReturn(Optional.of(emp));
            when(employeeRepository.existsByQrAndIdNotAndDeletedFalse("NEW-QR", 1)).thenReturn(false);
            when(employeeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            ReqEmployeeDTO req = new ReqEmployeeDTO();
            req.setUserId(1);        // same user → no user block
            req.setOfficeId(10);     // same office → no office block
            req.setSalaryRate(200.0);
            req.setQr("NEW-QR");
            req.setNote("updated note");

            // Act
            ResEmployeeDTO result = service.update(1, req);

            // Assert
            assertEquals(200.0,         result.getSalaryRate());
            assertEquals("NEW-QR",      result.getQr());
            assertEquals("updated note", result.getNote());

            verify(employeeRepository).save(any(Employee.class));
            verify(userRepository, never()).findById(anyInt()); // same user — no user lookup needed
            verify(officeRepository, never()).findById(anyInt());
        }

        @Test
        @DisplayName("Update to a different user – new user exists and is unbound – success")
        void update_ChangedUser_Success() {
            User oldUser  = makeUser(1);
            User newUser  = makeUser(2);
            Office office = makeOffice(5);
            Employee emp  = makeEmployee(10, oldUser, office);

            when(employeeRepository.findById(10)).thenReturn(Optional.of(emp));
            when(userRepository.findById(2)).thenReturn(Optional.of(newUser));
            when(employeeRepository.existsByUser_IdAndIdNotAndDeletedFalse(2, 10)).thenReturn(false);
            when(employeeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            ReqEmployeeDTO req = new ReqEmployeeDTO();
            req.setUserId(2);        // switch user
            req.setOfficeId(5);
            req.setSalaryRate(100.0);
            req.setQr(null);

            // Act
            ResEmployeeDTO result = service.update(10, req);

            // Assert – user changed
            assertEquals(2,       result.getUserId());
            assertEquals("user2", result.getUsername());
            verify(userRepository).findById(2);
            verify(employeeRepository).save(any(Employee.class));
        }

        @Test
        @DisplayName("Update to a different office – new office exists – success")
        void update_ChangedOffice_Success() {
            User user      = makeUser(1);
            Office oldOff  = makeOffice(1);
            Office newOff  = makeOffice(2);
            Employee emp   = makeEmployee(5, user, oldOff);

            when(employeeRepository.findById(5)).thenReturn(Optional.of(emp));
            when(officeRepository.findById(2)).thenReturn(Optional.of(newOff));
            when(employeeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            ReqEmployeeDTO req = new ReqEmployeeDTO();
            req.setUserId(1);        // same user
            req.setOfficeId(2);      // switch office
            req.setSalaryRate(100.0);
            req.setQr(null);

            // Act
            ResEmployeeDTO result = service.update(5, req);

            // Assert – office changed
            assertEquals(2,          result.getOfficeId());
            assertEquals("Office 2", result.getOfficeName());
            verify(officeRepository).findById(2);
        }

        @Test
        @DisplayName("Update clears QR to null – allowed")
        void update_ClearQr_ToNull_Allowed() {
            User user     = makeUser(1);
            Office office = makeOffice(1);
            Employee emp  = makeEmployee(3, user, office);

            when(employeeRepository.findById(3)).thenReturn(Optional.of(emp));
            when(employeeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            ReqEmployeeDTO req = new ReqEmployeeDTO();
            req.setUserId(1);
            req.setOfficeId(1);
            req.setSalaryRate(100.0);
            req.setQr(null);    // clearing QR

            // Act
            ResEmployeeDTO result = service.update(3, req);

            // Assert
            assertNull(result.getQr());
            verify(employeeRepository, never()).existsByQrAndIdNotAndDeletedFalse(any(), anyInt());
        }
    }

    // ==========================================================================
    // UPDATE — failure
    // ==========================================================================
    @Nested
    @DisplayName("update() – failure cases")
    class UpdateFailureTests {

        @Test
        @DisplayName("Employee not found → ResourceNotFoundException with id in message")
        void update_EmployeeNotFound_Throws() {
            when(employeeRepository.findById(404)).thenReturn(Optional.empty());

            ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                    () -> service.update(404, validCreateReq(1, 1)));

            assertTrue(ex.getMessage().contains("404"));
            verify(employeeRepository, never()).save(any());
        }

        @Test
        @DisplayName("New user not found during update → ResourceNotFoundException")
        void update_NewUserNotFound_Throws() {
            User oldUser  = makeUser(1);
            Office office = makeOffice(1);
            Employee emp  = makeEmployee(1, oldUser, office);

            when(employeeRepository.findById(1)).thenReturn(Optional.of(emp));
            when(userRepository.findById(99)).thenReturn(Optional.empty());

            ReqEmployeeDTO req = validCreateReq(99, 1); // switch to non-existent user

            ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                    () -> service.update(1, req));

            assertTrue(ex.getMessage().contains("99"));
            verify(employeeRepository, never()).save(any());
        }

        @Test
        @DisplayName("New user already bound to another employee → BadRequestException")
        void update_NewUserAlreadyBound_Throws() {
            User oldUser  = makeUser(1);
            User newUser  = makeUser(2);
            Office office = makeOffice(1);
            Employee emp  = makeEmployee(1, oldUser, office);

            when(employeeRepository.findById(1)).thenReturn(Optional.of(emp));
            when(userRepository.findById(2)).thenReturn(Optional.of(newUser));
            when(employeeRepository.existsByUser_IdAndIdNotAndDeletedFalse(2, 1)).thenReturn(true); // already taken

            ReqEmployeeDTO req = validCreateReq(2, 1);

            BadRequestException ex = assertThrows(BadRequestException.class,
                    () -> service.update(1, req));

            assertTrue(ex.getMessage().contains("2"));
            verify(employeeRepository, never()).save(any());
        }

        @Test
        @DisplayName("New office not found during update → ResourceNotFoundException")
        void update_NewOfficeNotFound_Throws() {
            User user     = makeUser(1);
            Office office = makeOffice(1);
            Employee emp  = makeEmployee(1, user, office);

            when(employeeRepository.findById(1)).thenReturn(Optional.of(emp));
            when(officeRepository.findById(777)).thenReturn(Optional.empty());

            ReqEmployeeDTO req = validCreateReq(1, 777); // switch to non-existent office

            ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                    () -> service.update(1, req));

            assertTrue(ex.getMessage().contains("777"));
            verify(employeeRepository, never()).save(any());
        }

        @Test
        @DisplayName("Negative salary rate on update → BadRequestException")
        void update_NegativeSalaryRate_Throws() {
            User user     = makeUser(1);
            Office office = makeOffice(1);
            Employee emp  = makeEmployee(1, user, office);

            when(employeeRepository.findById(1)).thenReturn(Optional.of(emp));

            ReqEmployeeDTO req = validCreateReq(1, 1);
            req.setSalaryRate(-1.0);

            BadRequestException ex = assertThrows(BadRequestException.class,
                    () -> service.update(1, req));

            assertTrue(ex.getMessage().contains(">= 0"));
            verify(employeeRepository, never()).save(any());
        }

        @Test
        @DisplayName("Duplicate QR on update (belongs to another employee) → BadRequestException")
        void update_DuplicateQr_Throws() {
            User user     = makeUser(1);
            Office office = makeOffice(1);
            Employee emp  = makeEmployee(1, user, office);

            when(employeeRepository.findById(1)).thenReturn(Optional.of(emp));
            when(employeeRepository.existsByQrAndIdNotAndDeletedFalse("TAKEN-QR", 1)).thenReturn(true);

            ReqEmployeeDTO req = validCreateReq(1, 1);
            req.setQr("TAKEN-QR");

            BadRequestException ex = assertThrows(BadRequestException.class,
                    () -> service.update(1, req));

            assertTrue(ex.getMessage().contains("TAKEN-QR"));
            verify(employeeRepository, never()).save(any());
        }
    }

    // ==========================================================================
    // DELETE
    // ==========================================================================
    @Nested
    @DisplayName("delete()")
    class DeleteTests {

        @Test
        @DisplayName("Success – employee found and deleted")
        void delete_Success() {
            User user     = makeUser(1);
            Office office = makeOffice(1);
            Employee emp  = makeEmployee(1, user, office);

            when(employeeRepository.findById(1)).thenReturn(Optional.of(emp));
            when(employeeRepository.save(any(Employee.class))).thenAnswer(inv -> inv.getArgument(0));

            service.delete(1);

            verify(employeeRepository).findById(1);
            // Soft delete: save() called with deleted=true, not delete()
            ArgumentCaptor<Employee> captor = ArgumentCaptor.forClass(Employee.class);
            verify(employeeRepository).save(captor.capture());
            assertTrue(captor.getValue().isDeleted());
        }

        @Test
        @DisplayName("Employee not found → ResourceNotFoundException, delete never called")
        void delete_NotFound_Throws() {
            when(employeeRepository.findById(404)).thenReturn(Optional.empty());

            ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                    () -> service.delete(404));

            assertTrue(ex.getMessage().contains("404"));
            verify(employeeRepository, never()).delete(any(Employee.class));
        }
    }

    // ==========================================================================
    // DTO BEAN-VALIDATION
    // ==========================================================================
    @Nested
    @DisplayName("Bean-validation on ReqEmployeeDTO")
    class DtoValidationTests {

        @Test
        @DisplayName("Null userId fails @NotNull")
        void validate_NullUserId() {
            ReqEmployeeDTO req = validCreateReq(1, 1);
            req.setUserId(null);

            Set<ConstraintViolation<ReqEmployeeDTO>> v = validator.validate(req);
            assertTrue(v.stream().anyMatch(c -> c.getPropertyPath().toString().equals("userId")));
        }

        @Test
        @DisplayName("Null officeId fails @NotNull")
        void validate_NullOfficeId() {
            ReqEmployeeDTO req = validCreateReq(1, 1);
            req.setOfficeId(null);

            Set<ConstraintViolation<ReqEmployeeDTO>> v = validator.validate(req);
            assertTrue(v.stream().anyMatch(c -> c.getPropertyPath().toString().equals("officeId")));
        }

        @Test
        @DisplayName("Null salaryRate fails @NotNull")
        void validate_NullSalaryRate() {
            ReqEmployeeDTO req = validCreateReq(1, 1);
            req.setSalaryRate(null);

            Set<ConstraintViolation<ReqEmployeeDTO>> v = validator.validate(req);
            assertTrue(v.stream().anyMatch(c -> c.getPropertyPath().toString().equals("salaryRate")));
        }

        @Test
        @DisplayName("Negative salaryRate fails @Min(0)")
        void validate_NegativeSalaryRate() {
            ReqEmployeeDTO req = validCreateReq(1, 1);
            req.setSalaryRate(-5.0);

            Set<ConstraintViolation<ReqEmployeeDTO>> v = validator.validate(req);
            assertTrue(v.stream().anyMatch(c -> c.getPropertyPath().toString().equals("salaryRate")));
        }

        @Test
        @DisplayName("salaryRate = 0.0 passes @Min(0)")
        void validate_ZeroSalaryRate_Valid() {
            ReqEmployeeDTO req = validCreateReq(1, 1);
            req.setSalaryRate(0.0);

            Set<ConstraintViolation<ReqEmployeeDTO>> v = validator.validate(req);
            assertTrue(v.stream().noneMatch(c -> c.getPropertyPath().toString().equals("salaryRate")));
        }

        @Test
        @DisplayName("Fully valid request has no violations")
        void validate_FullyValid_NoViolations() {
            ReqEmployeeDTO req = validCreateReq(1, 10);

            Set<ConstraintViolation<ReqEmployeeDTO>> v = validator.validate(req);
            assertTrue(v.isEmpty());
        }
    }
}
