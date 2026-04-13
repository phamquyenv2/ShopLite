package com.quyen.shoplite.service;

import com.quyen.shoplite.domain.Employee;
import com.quyen.shoplite.domain.Roster;
import com.quyen.shoplite.domain.request.ReqRosterDTO;
import com.quyen.shoplite.domain.response.ResRosterDTO;
import com.quyen.shoplite.repository.EmployeeRepository;
import com.quyen.shoplite.repository.RosterRepository;
import com.quyen.shoplite.util.DTOMapper;
import com.quyen.shoplite.util.error.BadRequestException;
import com.quyen.shoplite.util.error.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RosterService {

    private final RosterRepository rosterRepository;
    private final EmployeeRepository employeeRepository;

    // ------------------------------------------------------------------ create

    @Transactional
    public ResRosterDTO create(ReqRosterDTO req) {
        Employee employee = findActiveEmployee(req.getEmployeeId());

        if (rosterRepository.existsByEmployee_IdAndWorkingDay(employee.getId(), req.getWorkingDay())) {
            throw new BadRequestException(
                    "Roster already exists for employee id=" + employee.getId()
                            + " on date=" + req.getWorkingDay());
        }

        Roster roster = buildRoster(req, employee);
        return DTOMapper.toResRosterDTO(rosterRepository.save(roster));
    }

    // ------------------------------------------------------------------ update

    @Transactional
    public ResRosterDTO update(Integer id, ReqRosterDTO req) {
        Roster roster = findEntity(id);

        // If employee changes check for conflict
        if (!req.getEmployeeId().equals(roster.getEmployee().getId())) {
            Employee newEmployee = findActiveEmployee(req.getEmployeeId());
            if (rosterRepository.existsByEmployee_IdAndWorkingDay(newEmployee.getId(), req.getWorkingDay())
                    && !req.getWorkingDay().equals(roster.getWorkingDay())) {
                throw new BadRequestException(
                        "Roster conflict for employee id=" + newEmployee.getId()
                                + " on date=" + req.getWorkingDay());
            }
            roster.setEmployee(newEmployee);
        } else if (!req.getWorkingDay().equals(roster.getWorkingDay())) {
            // Same employee, different day — check conflict
            if (rosterRepository.existsByEmployee_IdAndWorkingDay(roster.getEmployee().getId(), req.getWorkingDay())) {
                throw new BadRequestException(
                        "Roster already exists for employee id=" + roster.getEmployee().getId()
                                + " on date=" + req.getWorkingDay());
            }
        }

        applyFields(req, roster);
        return DTOMapper.toResRosterDTO(rosterRepository.save(roster));
    }

    // ------------------------------------------------------------------ read

    public ResRosterDTO findById(Integer id) {
        return DTOMapper.toResRosterDTO(findEntity(id));
    }

    /**
     * Lấy lịch của một nhân viên trong khoảng ngày [from, to].
     */
    public List<ResRosterDTO> findByEmployeeAndRange(Integer employeeId, LocalDate from, LocalDate to) {
        if (from.isAfter(to)) {
            throw new BadRequestException("from must be <= to");
        }
        return rosterRepository
                .findByEmployee_IdAndWorkingDayBetweenOrderByWorkingDayAsc(employeeId, from, to)
                .stream()
                .map(DTOMapper::toResRosterDTO)
                .toList();
    }

    /**
     * Lấy lịch tất cả nhân viên trong một ngày cụ thể (daily overview).
     */
    public List<ResRosterDTO> findByDay(LocalDate day) {
        return rosterRepository.findByWorkingDayOrderByEmployee_IdAsc(day)
                .stream()
                .map(DTOMapper::toResRosterDTO)
                .toList();
    }

    // ------------------------------------------------------------------ delete

    @Transactional
    public void delete(Integer id) {
        Roster roster = findEntity(id);
        rosterRepository.delete(roster);
    }

    // ------------------------------------------------------------------ helpers

    private Roster findEntity(Integer id) {
        return rosterRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Roster not found with id=" + id));
    }

    private Employee findActiveEmployee(Integer employeeId) {
        return employeeRepository.findById(employeeId)
                .filter(e -> !e.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Active employee not found with id=" + employeeId));
    }

    private Roster buildRoster(ReqRosterDTO req, Employee employee) {
        Roster roster = new Roster();
        roster.setEmployee(employee);
        applyFields(req, roster);
        return roster;
    }

    /** Apply mutable fields from request onto a roster instance. */
    private void applyFields(ReqRosterDTO req, Roster roster) {
        if (req.getType() == com.quyen.shoplite.util.constant.RosterTypeEnum.WORKING) {
            if (req.getStartTime() == null || req.getEndTime() == null) {
                throw new BadRequestException("startTime và endTime bắt buộc khi type là WORKING");
            }
        }
        
        roster.setWorkingDay(req.getWorkingDay());
        roster.setType(req.getType());
        roster.setStartTime(req.getStartTime());
        roster.setEndTime(req.getEndTime());
        roster.setNote(req.getNote());
        roster.setUnpaidBreakMinutes(req.getUnpaidBreakMinutes() != null ? req.getUnpaidBreakMinutes() : 0L);

        // Auto-compute expectedHours from start/end if both provided
        if (req.getStartTime() != null && req.getEndTime() != null) {
            long minutes = Duration.between(req.getStartTime(), req.getEndTime()).toMinutes();
            roster.setExpectedHours(minutes > 0 ? minutes / 60.0 : 0.0);
        } else {
            roster.setExpectedHours(0.0);
        }
    }
}
