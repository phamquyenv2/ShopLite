package com.quyen.shoplite.service;

import com.quyen.shoplite.domain.Employee;
import com.quyen.shoplite.domain.EmployeeSalaryHistory;
import com.quyen.shoplite.domain.request.ReqEmployeeSalaryHistoryDTO;
import com.quyen.shoplite.domain.response.ResEmployeeSalaryHistoryDTO;
import com.quyen.shoplite.repository.EmployeeRepository;
import com.quyen.shoplite.repository.EmployeeSalaryHistoryRepository;
import com.quyen.shoplite.repository.UserRepository;
import com.quyen.shoplite.util.SecurityUtil;
import com.quyen.shoplite.util.constant.SalaryTypeEnum;
import com.quyen.shoplite.util.error.BadRequestException;
import com.quyen.shoplite.util.error.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class EmployeeSalaryHistoryService {

    private final EmployeeSalaryHistoryRepository salaryHistoryRepository;
    private final EmployeeRepository employeeRepository;
    private final UserRepository userRepository;
    private final CurrentStoreService currentStoreService;

    @Transactional
    public ResEmployeeSalaryHistoryDTO createForEmployee(Integer employeeId, ReqEmployeeSalaryHistoryDTO req) {
        Employee employee = findEmployee(employeeId);
        LocalDate effectiveFrom = req.getEffectiveFrom() != null ? req.getEffectiveFrom() : LocalDate.now();

        EmployeeSalaryHistory current = salaryHistoryRepository
                .findFirstByStore_IdAndEmployee_IdAndEffectiveToIsNullOrderByEffectiveFromDescIdDesc(
                        currentStoreService.getCurrentStoreId(), employeeId)
                .orElse(null);

        if (current != null && !current.getEffectiveFrom().isBefore(effectiveFrom)) {
            apply(current, req, effectiveFrom);
            current.setCreatedBy(SecurityUtil.getCurrentUserLogin().orElse(null));
            syncEmployeeSalaryCache(employee, current.getBaseRate());
            return toDto(salaryHistoryRepository.save(current));
        }

        if (current != null) {
            current.setEffectiveTo(effectiveFrom.minusDays(1));
            salaryHistoryRepository.save(current);
        }

        EmployeeSalaryHistory history = EmployeeSalaryHistory.builder()
                .store(currentStoreService.getCurrentStore())
                .employee(employee)
                .createdBy(SecurityUtil.getCurrentUserLogin().orElse(null))
                .build();
        apply(history, req, effectiveFrom);
        syncEmployeeSalaryCache(employee, history.getBaseRate());
        return toDto(salaryHistoryRepository.save(history));
    }

    @Transactional(readOnly = true)
    public List<ResEmployeeSalaryHistoryDTO> findByEmployee(Integer employeeId) {
        findEmployee(employeeId);
        Long storeId = currentStoreService.getCurrentStoreId();
        return salaryHistoryRepository.findByStore_IdAndEmployee_IdOrderByEffectiveFromDescIdDesc(storeId, employeeId)
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ResEmployeeSalaryHistoryDTO> findMine() {
        Employee employee = findCurrentEmployee();
        return salaryHistoryRepository
                .findByStore_IdAndEmployee_IdOrderByEffectiveFromDescIdDesc(
                        currentStoreService.getCurrentStoreId(), employee.getId())
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public ResEmployeeSalaryHistoryDTO findCurrentByEmployee(Integer employeeId) {
        findEmployee(employeeId);
        return findCurrentEntity(employeeId)
                .map(this::toDto)
                .orElseThrow(() -> new ResourceNotFoundException("Salary history not found for employee id=" + employeeId));
    }

    @Transactional(readOnly = true)
    public ResEmployeeSalaryHistoryDTO findCurrentMine() {
        Employee employee = findCurrentEmployee();
        return findCurrentEntity(employee.getId())
                .map(this::toDto)
                .orElseThrow(() -> new ResourceNotFoundException("Salary history not found for current employee"));
    }

    @Transactional(readOnly = true)
    public EmployeeSalaryHistory findEffectiveEntity(Integer employeeId, LocalDate date) {
        Long storeId = currentStoreService.getCurrentStoreId();
        return salaryHistoryRepository.findEffectiveAt(storeId, employeeId, date).stream()
                .findFirst()
                .orElse(null);
    }

    private void apply(EmployeeSalaryHistory history, ReqEmployeeSalaryHistoryDTO req, LocalDate effectiveFrom) {
        history.setSalaryType(req.getSalaryType() != null ? req.getSalaryType() : SalaryTypeEnum.HOURLY);
        history.setBaseRate(nonNegative(req.getBaseRate(), "baseRate"));
        history.setAllowance(nonNegativeOrZero(req.getAllowance(), "allowance"));
        history.setCommission(nonNegativeOrZero(req.getCommission(), "commission"));
        history.setRecurringBonus(nonNegativeOrZero(req.getRecurringBonus(), "recurringBonus"));
        history.setRecurringDeduction(nonNegativeOrZero(req.getRecurringDeduction(), "recurringDeduction"));
        history.setEffectiveFrom(effectiveFrom);
        history.setEffectiveTo(null);
        history.setReason(req.getReason());
    }

    private Double nonNegative(Double value, String field) {
        if (value == null || value < 0) {
            throw new BadRequestException(field + " must be >= 0");
        }
        return value;
    }

    private Double nonNegativeOrZero(Double value, String field) {
        if (value == null) {
            return 0.0;
        }
        return nonNegative(value, field);
    }

    private java.util.Optional<EmployeeSalaryHistory> findCurrentEntity(Integer employeeId) {
        Long storeId = currentStoreService.getCurrentStoreId();
        return salaryHistoryRepository
                .findFirstByStore_IdAndEmployee_IdAndEffectiveToIsNullOrderByEffectiveFromDescIdDesc(storeId, employeeId);
    }

    private Employee findEmployee(Integer employeeId) {
        Long storeId = currentStoreService.getCurrentStoreId();
        return employeeRepository.findByIdAndStoreMember_Store_IdAndDeletedFalse(employeeId, storeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with id=" + employeeId));
    }

    private Employee findCurrentEmployee() {
        String username = SecurityUtil.requireCurrentUserLogin();
        var user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with username=" + username));
        Long storeId = currentStoreService.getCurrentStoreId();
        return employeeRepository.findByStoreMember_Store_IdAndStoreMember_User_IdAndDeletedFalse(storeId, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found for user id=" + user.getId()));
    }

    private void syncEmployeeSalaryCache(Employee employee, Double baseRate) {
        employee.setSalaryRate(baseRate);
        employeeRepository.save(employee);
    }

    private ResEmployeeSalaryHistoryDTO toDto(EmployeeSalaryHistory history) {
        ResEmployeeSalaryHistoryDTO dto = new ResEmployeeSalaryHistoryDTO();
        dto.setId(history.getId());
        dto.setSalaryType(history.getSalaryType());
        dto.setBaseRate(history.getBaseRate());
        dto.setAllowance(history.getAllowance());
        dto.setCommission(history.getCommission());
        dto.setRecurringBonus(history.getRecurringBonus());
        dto.setRecurringDeduction(history.getRecurringDeduction());
        dto.setEffectiveFrom(history.getEffectiveFrom());
        dto.setEffectiveTo(history.getEffectiveTo());
        dto.setReason(history.getReason());
        dto.setCreatedBy(history.getCreatedBy());
        dto.setCreatedAt(history.getCreatedAt());
        dto.setCurrent(history.getEffectiveTo() == null);
        if (history.getEmployee() != null) {
            dto.setEmployeeId(history.getEmployee().getId());
            if (history.getEmployee().getStoreMember() != null
                    && history.getEmployee().getStoreMember().getUser() != null) {
                dto.setEmployeeUsername(history.getEmployee().getStoreMember().getUser().getUsername());
            }
        }
        return dto;
    }
}
