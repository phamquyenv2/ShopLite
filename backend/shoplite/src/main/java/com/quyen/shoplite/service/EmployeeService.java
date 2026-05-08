package com.quyen.shoplite.service;

import com.quyen.shoplite.domain.Employee;
import com.quyen.shoplite.domain.Office;
import com.quyen.shoplite.domain.Role;
import com.quyen.shoplite.domain.StoreMember;
import com.quyen.shoplite.domain.Store;
import com.quyen.shoplite.domain.User;
import com.quyen.shoplite.domain.request.ReqEmployeeDTO;
import com.quyen.shoplite.domain.response.ResEmployeeDTO;
import com.quyen.shoplite.repository.EmployeeRepository;
import com.quyen.shoplite.repository.OfficeRepository;
import com.quyen.shoplite.repository.RoleRepository;
import com.quyen.shoplite.repository.StoreMemberRepository;
import com.quyen.shoplite.repository.UserRepository;
import com.quyen.shoplite.util.DTOMapper;
import com.quyen.shoplite.util.constant.StoreMemberStatus;
import com.quyen.shoplite.util.error.BadRequestException;
import com.quyen.shoplite.util.error.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final UserRepository userRepository;
    private final OfficeRepository officeRepository;
    private final StoreMemberRepository storeMemberRepository;
    private final RoleRepository roleRepository;
    private final CurrentStoreService currentStoreService;

    // ------------------------------------------------------------------ create

    @Transactional
    public ResEmployeeDTO create(ReqEmployeeDTO req) {
        Store store = currentStoreService.getCurrentStore();
        Long storeId = store.getId();

        // 1. Validate user exists
        User user = findUser(req.getUserId());

        // 2. Prevent duplicate employee-user mapping (active employees only)
        if (employeeRepository.existsByStoreMember_Store_IdAndStoreMember_User_IdAndDeletedFalse(storeId, req.getUserId())) {
            throw new BadRequestException(
                    "User id=" + req.getUserId() + " is already linked to an employee");
        }

        // 3. Validate office
        Office office = findOffice(req.getOfficeId());

        // 4. Validate salary
        if (req.getSalaryRate() < 0) {
            throw new BadRequestException("salaryRate must be >= 0");
        }

        // 5. QR uniqueness (active only)
        if (req.getQr() != null && !req.getQr().isBlank()) {
            if (employeeRepository.existsByStoreMember_Store_IdAndQrAndDeletedFalse(storeId, req.getQr().trim())) {
                throw new BadRequestException("qr_code already exists: " + req.getQr().trim());
            }
        }

        // 6. Ensure StoreMember exists or create one
        StoreMember storeMember = storeMemberRepository
                .findByStoreIdAndUserId(storeId, user.getId())
                .orElseGet(() -> {
                    Role defaultRole = roleRepository.findByName("ORDER_STAFF").orElse(null);
                    return storeMemberRepository.save(StoreMember.builder()
                            .store(store)
                            .user(user)
                            .role(defaultRole)
                            .status(StoreMemberStatus.ACTIVE)
                            .joinedAt(LocalDateTime.now())
                            .build());
                });

        Employee employee = Employee.builder()
                .storeMember(storeMember)
                .store(store)
                .office(office)
                .salaryRate(req.getSalaryRate())
                .qr(req.getQr() != null ? req.getQr().trim() : null)
                .note(req.getNote())
                .build();

        return DTOMapper.toResEmployeeDTO(employeeRepository.save(employee));
    }

    // ------------------------------------------------------------------ read

    public ResEmployeeDTO findById(Integer id) {
        return DTOMapper.toResEmployeeDTO(findEntityById(id));
    }

    public List<ResEmployeeDTO> findAll() {
        Long storeId = currentStoreService.getCurrentStoreId();
        ensureEmployeesForActiveMembers(storeId);
        return employeeRepository.findAllByStoreMember_Store_IdOrderByIdAsc(storeId).stream()
                .map(DTOMapper::toResEmployeeDTO)
                .toList();
    }

    // ------------------------------------------------------------------ update

    @Transactional
    public ResEmployeeDTO update(Integer id, ReqEmployeeDTO req) {
        Employee employee = findEntityById(id);
        Long storeId = currentStoreService.getCurrentStoreId();

        // 1. Validate & switch user if changed
        Integer currentUserId = employee.getStoreMember().getUser().getId();
        if (!req.getUserId().equals(currentUserId)) {
            if (employeeRepository.existsByStoreMember_Store_IdAndStoreMember_User_IdAndIdNotAndDeletedFalse(
                    storeId, req.getUserId(), id)) {
                throw new BadRequestException(
                        "User id=" + req.getUserId() + " is already linked to another employee");
            }
            User newUser = findUser(req.getUserId());
            StoreMember newStoreMember = storeMemberRepository
                    .findByStoreIdAndUserId(storeId, newUser.getId())
                    .orElseGet(() -> {
                        Role defaultRole = roleRepository.findByName("ORDER_STAFF").orElse(null);
                        Store store = employee.getStoreMember().getStore();
                        return storeMemberRepository.save(StoreMember.builder()
                                .store(store)
                                .user(newUser)
                                .role(defaultRole)
                                .status(StoreMemberStatus.ACTIVE)
                                .joinedAt(LocalDateTime.now())
                                .build());
                    });
            employee.setStoreMember(newStoreMember);
            employee.setStore(newStoreMember.getStore());
        }

        // 2. Validate & switch office if changed
        if (!req.getOfficeId().equals(
                employee.getOffice() != null ? employee.getOffice().getId() : null)) {
            Office newOffice = findOffice(req.getOfficeId());
            employee.setOffice(newOffice);
        }

        // 3. Validate salary
        if (req.getSalaryRate() < 0) {
            throw new BadRequestException("salaryRate must be >= 0");
        }
        employee.setSalaryRate(req.getSalaryRate());

        // 4. QR uniqueness
        String newQr = req.getQr() != null ? req.getQr().trim() : null;
        if (newQr != null && !newQr.isBlank()) {
            if (employeeRepository.existsByStoreMember_Store_IdAndQrAndIdNotAndDeletedFalse(storeId, newQr, id)) {
                throw new BadRequestException("qr_code already exists: " + newQr);
            }
        }
        employee.setQr(newQr);

        // 5. Note
        employee.setNote(req.getNote());

        return DTOMapper.toResEmployeeDTO(employeeRepository.save(employee));
    }

    // ------------------------------------------------------------------ delete

    @Transactional
    public void delete(Integer id) {
        Employee employee = findEntityById(id);
        if (employee.isDeleted()) {
            throw new BadRequestException("Employee id=" + id + " is already deactivated");
        }
        employee.setDeleted(true);
        employeeRepository.save(employee);
    }

    // ------------------------------------------------------------------ helpers

    private Employee findEntityById(Integer id) {
        Long storeId = currentStoreService.getCurrentStoreId();
        return employeeRepository.findByIdAndStoreMember_Store_IdAndDeletedFalse(id, storeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with id=" + id));
    }

    private User findUser(Integer userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id=" + userId));
    }

    private Office findOffice(Integer officeId) {
        Long storeId = currentStoreService.getCurrentStoreId();
        return officeRepository.findByIdAndStoreId(officeId, storeId)
                .orElseThrow(() -> new ResourceNotFoundException("Office not found with id=" + officeId));
    }

    private void ensureEmployeesForActiveMembers(Long storeId) {
        Office defaultOffice = officeRepository.findAllByStoreIdOrderByIdAsc(storeId).stream()
                .findFirst()
                .orElse(null);

        storeMemberRepository.findAllByStore_IdAndStatus(storeId, StoreMemberStatus.ACTIVE)
                .forEach(member -> employeeRepository.findByStoreMember_Id(member.getId())
                        .ifPresentOrElse(employee -> {
                            if (employee.isDeleted()) {
                                employee.setDeleted(false);
                                employeeRepository.save(employee);
                            }
                        }, () -> employeeRepository.save(Employee.builder()
                                .storeMember(member)
                                .store(member.getStore())
                                .office(defaultOffice)
                                .salaryRate(0.0)
                                .build())));
    }
}
