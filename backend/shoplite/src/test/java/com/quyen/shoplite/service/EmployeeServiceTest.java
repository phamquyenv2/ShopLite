package com.quyen.shoplite.service;

import com.quyen.shoplite.repository.EmployeeRepository;
import com.quyen.shoplite.repository.OfficeRepository;
import com.quyen.shoplite.repository.RoleRepository;
import com.quyen.shoplite.repository.StoreMemberRepository;
import com.quyen.shoplite.repository.UserRepository;
import com.quyen.shoplite.util.constant.StoreMemberStatus;
import com.quyen.shoplite.util.error.BadRequestException;
import com.quyen.shoplite.util.error.ResourceNotFoundException;

import com.quyen.shoplite.domain.Employee;
import com.quyen.shoplite.domain.Office;
import com.quyen.shoplite.domain.Role;
import com.quyen.shoplite.domain.Store;
import com.quyen.shoplite.domain.StoreMember;
import com.quyen.shoplite.domain.User;
import com.quyen.shoplite.domain.request.ReqEmployeeDTO;
import com.quyen.shoplite.domain.response.ResEmployeeDTO;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmployeeServiceTest {

    @Mock private EmployeeRepository employeeRepository;
    @Mock private UserRepository userRepository;
    @Mock private OfficeRepository officeRepository;
    @Mock private StoreMemberRepository storeMemberRepository;
    @Mock private RoleRepository roleRepository;
    @Mock private CurrentStoreService currentStoreService;

    @InjectMocks
    private EmployeeService service;

    private Store store;
    private User owner;

    @BeforeEach
    void setUp() {
        owner = makeUser(1, "owner");
        store = Store.builder().id(10L).name("Main Store").owner(owner).build();
        when(currentStoreService.getCurrentStoreId()).thenReturn(store.getId());
    }

    @Test
    void create_WhenStoreMemberMissing_CreatesMemberAndEmployee() {
        User user = makeUser(2, "cashier");
        Office office = makeOffice(3);
        Role role = Role.builder().id(4L).name("ORDER_STAFF").build();
        ReqEmployeeDTO req = validReq(user.getId(), office.getId());

        when(currentStoreService.getCurrentStore()).thenReturn(store);
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(employeeRepository.existsByStoreMember_Store_IdAndStoreMember_User_IdAndDeletedFalse(store.getId(), user.getId()))
                .thenReturn(false);
        when(officeRepository.findByIdAndStoreId(office.getId(), store.getId())).thenReturn(Optional.of(office));
        when(employeeRepository.existsByStoreMember_Store_IdAndQrAndDeletedFalse(store.getId(), "QR-NEW")).thenReturn(false);
        when(storeMemberRepository.findByStoreIdAndUserId(store.getId(), user.getId())).thenReturn(Optional.empty());
        when(roleRepository.findByName("ORDER_STAFF")).thenReturn(Optional.of(role));
        when(storeMemberRepository.save(any(StoreMember.class))).thenAnswer(inv -> {
            StoreMember member = inv.getArgument(0);
            member.setId(99L);
            return member;
        });
        when(employeeRepository.save(any(Employee.class))).thenAnswer(inv -> {
            Employee employee = inv.getArgument(0);
            employee.setId(100);
            return employee;
        });

        ResEmployeeDTO result = service.create(req);

        assertNotNull(result);
        assertEquals(100, result.getId());
        assertEquals(user.getId(), result.getUserId());
        assertEquals("cashier", result.getUsername());
        assertEquals(office.getId(), result.getOfficeId());
        assertEquals("QR-NEW", result.getQr());
        assertEquals("ORDER_STAFF", result.getRoleName());
    }

    @Test
    void create_DuplicateUserMapping_Throws() {
        User user = makeUser(2, "cashier");
        ReqEmployeeDTO req = validReq(user.getId(), 3);

        when(currentStoreService.getCurrentStore()).thenReturn(store);
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(employeeRepository.existsByStoreMember_Store_IdAndStoreMember_User_IdAndDeletedFalse(store.getId(), user.getId()))
                .thenReturn(true);

        BadRequestException ex = assertThrows(BadRequestException.class, () -> service.create(req));

        assertTrue(ex.getMessage().contains(user.getId().toString()));
        verify(employeeRepository, never()).save(any());
    }

    @Test
    void findAll_EnsuresEmployeesForActiveMembersAndReturnsStoreScopedList() {
        Office office = makeOffice(3);
        User user = makeUser(2, "cashier");
        StoreMember member = makeMember(7L, user);
        Employee employee = makeEmployee(8, member, office);

        when(officeRepository.findAllByStoreIdOrderByIdAsc(store.getId())).thenReturn(List.of(office));
        when(storeMemberRepository.findAllByStore_IdAndStatus(store.getId(), StoreMemberStatus.ACTIVE)).thenReturn(List.of(member));
        when(employeeRepository.findByStoreMember_Id(member.getId())).thenReturn(Optional.of(employee));
        when(employeeRepository.findAllByStoreMember_Store_IdOrderByIdAsc(store.getId())).thenReturn(List.of(employee));

        List<ResEmployeeDTO> result = service.findAll();

        assertEquals(1, result.size());
        assertEquals(employee.getId(), result.get(0).getId());
        assertEquals(user.getId(), result.get(0).getUserId());
    }

    @Test
    void update_WhenUserChanges_UsesStoreMemberPath() {
        Office office = makeOffice(3);
        User oldUser = makeUser(2, "old");
        User newUser = makeUser(5, "new");
        StoreMember oldMember = makeMember(7L, oldUser);
        StoreMember newMember = makeMember(9L, newUser);
        Employee employee = makeEmployee(8, oldMember, office);
        ReqEmployeeDTO req = validReq(newUser.getId(), office.getId());
        req.setQr(null);

        when(employeeRepository.findByIdAndStoreMember_Store_IdAndDeletedFalse(employee.getId(), store.getId()))
                .thenReturn(Optional.of(employee));
        when(employeeRepository.existsByStoreMember_Store_IdAndStoreMember_User_IdAndIdNotAndDeletedFalse(
                store.getId(), newUser.getId(), employee.getId())).thenReturn(false);
        when(userRepository.findById(newUser.getId())).thenReturn(Optional.of(newUser));
        when(storeMemberRepository.findByStoreIdAndUserId(store.getId(), newUser.getId())).thenReturn(Optional.of(newMember));
        when(employeeRepository.save(any(Employee.class))).thenAnswer(inv -> inv.getArgument(0));

        ResEmployeeDTO result = service.update(employee.getId(), req);

        assertEquals(newUser.getId(), result.getUserId());
        assertEquals("new", result.getUsername());
        assertEquals(store, employee.getStore());
    }

    @Test
    void update_DuplicateQr_Throws() {
        Office office = makeOffice(3);
        User user = makeUser(2, "cashier");
        Employee employee = makeEmployee(8, makeMember(7L, user), office);
        ReqEmployeeDTO req = validReq(user.getId(), office.getId());
        req.setQr("TAKEN");

        when(employeeRepository.findByIdAndStoreMember_Store_IdAndDeletedFalse(employee.getId(), store.getId()))
                .thenReturn(Optional.of(employee));
        when(employeeRepository.existsByStoreMember_Store_IdAndQrAndIdNotAndDeletedFalse(store.getId(), "TAKEN", employee.getId()))
                .thenReturn(true);

        BadRequestException ex = assertThrows(BadRequestException.class, () -> service.update(employee.getId(), req));

        assertTrue(ex.getMessage().contains("TAKEN"));
        verify(employeeRepository, never()).save(any());
    }

    @Test
    void delete_SoftDeletesEmployee() {
        Office office = makeOffice(3);
        Employee employee = makeEmployee(8, makeMember(7L, makeUser(2, "cashier")), office);

        when(employeeRepository.findByIdAndStoreMember_Store_IdAndDeletedFalse(employee.getId(), store.getId()))
                .thenReturn(Optional.of(employee));
        when(employeeRepository.save(any(Employee.class))).thenAnswer(inv -> inv.getArgument(0));

        service.delete(employee.getId());

        ArgumentCaptor<Employee> captor = ArgumentCaptor.forClass(Employee.class);
        verify(employeeRepository).save(captor.capture());
        assertTrue(captor.getValue().isDeleted());
    }

    @Test
    void findById_NotFound_Throws() {
        when(employeeRepository.findByIdAndStoreMember_Store_IdAndDeletedFalse(404, store.getId()))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.findById(404));
    }

    private ReqEmployeeDTO validReq(Integer userId, Integer officeId) {
        ReqEmployeeDTO req = new ReqEmployeeDTO();
        req.setUserId(userId);
        req.setOfficeId(officeId);
        req.setSalaryRate(50.0);
        req.setQr("QR-NEW");
        req.setNote("note");
        return req;
    }

    private User makeUser(Integer id, String username) {
        return User.builder()
                .id(id)
                .username(username)
                .password("secret")
                .isActive(true)
                .build();
    }

    private Office makeOffice(Integer id) {
        return Office.builder().id(id).store(store).name("Office " + id).build();
    }

    private StoreMember makeMember(Long id, User user) {
        return StoreMember.builder()
                .id(id)
                .store(store)
                .user(user)
                .status(StoreMemberStatus.ACTIVE)
                .build();
    }

    private Employee makeEmployee(Integer id, StoreMember member, Office office) {
        return Employee.builder()
                .id(id)
                .store(store)
                .storeMember(member)
                .office(office)
                .salaryRate(100.0)
                .qr("QR-" + id)
                .deleted(false)
                .build();
    }
}
