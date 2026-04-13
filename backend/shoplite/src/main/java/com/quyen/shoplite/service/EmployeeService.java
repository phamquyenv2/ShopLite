package com.quyen.shoplite.service;

import com.quyen.shoplite.domain.Employee;
import com.quyen.shoplite.domain.Office;
import com.quyen.shoplite.domain.User;
import com.quyen.shoplite.domain.request.ReqEmployeeDTO;
import com.quyen.shoplite.domain.response.ResEmployeeDTO;
import com.quyen.shoplite.repository.EmployeeRepository;
import com.quyen.shoplite.repository.OfficeRepository;
import com.quyen.shoplite.repository.UserRepository;
import com.quyen.shoplite.util.DTOMapper;
import com.quyen.shoplite.util.error.BadRequestException;
import com.quyen.shoplite.util.error.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final UserRepository userRepository;
    private final OfficeRepository officeRepository;

    // ------------------------------------------------------------------ create

    @Transactional
    public ResEmployeeDTO create(ReqEmployeeDTO req) {
        // 1. Validate user exists
        User user = findUser(req.getUserId());

        // 2. Prevent duplicate employee-user mapping
        if (employeeRepository.existsByUser_Id(req.getUserId())) {
            throw new BadRequestException(
                    "User id=" + req.getUserId() + " is already linked to an employee");
        }

        // 3. Validate office
        Office office = findOffice(req.getOfficeId());

        // 4. Validate salary
        if (req.getSalaryRate() < 0) {
            throw new BadRequestException("salaryRate must be >= 0");
        }

        // 5. QR uniqueness
        if (req.getQr() != null && !req.getQr().isBlank()) {
            if (employeeRepository.existsByQr(req.getQr().trim())) {
                throw new BadRequestException("qr_code already exists: " + req.getQr().trim());
            }
        }

        Employee employee = Employee.builder()
                .user(user)
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
        return employeeRepository.findAll().stream()
                .map(DTOMapper::toResEmployeeDTO)
                .toList();
    }

    // ------------------------------------------------------------------ update

    @Transactional
    public ResEmployeeDTO update(Integer id, ReqEmployeeDTO req) {
        Employee employee = findEntityById(id);

        // 1. Validate & switch user if changed
        if (!req.getUserId().equals(employee.getUser().getId())) {
            // Ensure the new user exists
            User newUser = findUser(req.getUserId());
            // Ensure the new user is not already bound to another employee
            if (employeeRepository.existsByUser_IdAndIdNot(req.getUserId(), id)) {
                throw new BadRequestException(
                        "User id=" + req.getUserId() + " is already linked to another employee");
            }
            employee.setUser(newUser);
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

        // 4. QR uniqueness (only when qr value actually changes)
        String newQr = req.getQr() != null ? req.getQr().trim() : null;
        if (newQr != null && !newQr.isBlank()) {
            if (employeeRepository.existsByQrAndIdNot(newQr, id)) {
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
        employeeRepository.delete(employee);
    }

    // ------------------------------------------------------------------ helpers

    private Employee findEntityById(Integer id) {
        return employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with id=" + id));
    }

    private User findUser(Integer userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id=" + userId));
    }

    private Office findOffice(Integer officeId) {
        return officeRepository.findById(officeId)
                .orElseThrow(() -> new ResourceNotFoundException("Office not found with id=" + officeId));
    }
}
