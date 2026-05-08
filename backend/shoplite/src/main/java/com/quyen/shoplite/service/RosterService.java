package com.quyen.shoplite.service;

import com.quyen.shoplite.domain.Employee;
import com.quyen.shoplite.domain.Roster;
import com.quyen.shoplite.domain.request.ReqRosterDTO;
import com.quyen.shoplite.domain.response.ResRosterDTO;
import com.quyen.shoplite.repository.EmployeeRepository;
import com.quyen.shoplite.repository.RosterRepository;
import com.quyen.shoplite.util.DTOMapper;
import com.quyen.shoplite.util.constant.RosterTypeEnum;
import com.quyen.shoplite.util.error.BadRequestException;
import com.quyen.shoplite.util.error.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RosterService {

    private final RosterRepository rosterRepository;
    private final EmployeeRepository employeeRepository;
    private final CurrentStoreService currentStoreService;

    @Transactional
    public ResRosterDTO create(ReqRosterDTO req) {
        Employee employee = findActiveEmployee(req.getEmployeeId());
        validateNoShiftOverlap(null, employee.getId(), req);

        Roster roster = buildRoster(req, employee);
        return DTOMapper.toResRosterDTO(rosterRepository.save(roster));
    }

    @Transactional
    public ResRosterDTO update(Integer id, ReqRosterDTO req) {
        Roster roster = findEntity(id);

        if (!req.getEmployeeId().equals(roster.getEmployee().getId())) {
            Employee newEmployee = findActiveEmployee(req.getEmployeeId());
            validateNoShiftOverlap(id, newEmployee.getId(), req);
            roster.setEmployee(newEmployee);
        } else {
            validateNoShiftOverlap(id, roster.getEmployee().getId(), req);
        }

        applyFields(req, roster);
        return DTOMapper.toResRosterDTO(rosterRepository.save(roster));
    }

    public ResRosterDTO findById(Integer id) {
        return DTOMapper.toResRosterDTO(findEntity(id));
    }

    public List<ResRosterDTO> findByEmployeeAndRange(Integer employeeId, LocalDate from, LocalDate to) {
        if (from.isAfter(to)) {
            throw new BadRequestException("from must be <= to");
        }
        return rosterRepository
                .findByEmployee_StoreMember_Store_IdAndEmployee_IdAndWorkingDayBetweenOrderByWorkingDayAsc(
                        currentStoreService.getCurrentStoreId(), employeeId, from, to)
                .stream()
                .map(DTOMapper::toResRosterDTO)
                .toList();
    }

    public List<ResRosterDTO> findByDay(LocalDate day) {
        return rosterRepository.findByEmployee_StoreMember_Store_IdAndWorkingDayOrderByEmployee_IdAsc(
                        currentStoreService.getCurrentStoreId(), day)
                .stream()
                .map(DTOMapper::toResRosterDTO)
                .toList();
    }

    public List<ResRosterDTO> findByRange(LocalDate from, LocalDate to) {
        if (from.isAfter(to)) {
            throw new BadRequestException("from must be <= to");
        }
        return rosterRepository.findByStoreAndWorkingDayBetween(currentStoreService.getCurrentStoreId(), from, to)
                .stream()
                .map(DTOMapper::toResRosterDTO)
                .toList();
    }

    @Transactional
    public void delete(Integer id) {
        Roster roster = findEntity(id);
        try {
            rosterRepository.delete(roster);
            rosterRepository.flush();
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            throw new BadRequestException("Không thể xóa ca làm đã có dữ liệu chấm công.");
        }
    }

    private Roster findEntity(Integer id) {
        Long storeId = currentStoreService.getCurrentStoreId();
        return rosterRepository.findById(id)
                .filter(roster -> roster.getEmployee().getStoreMember().getStore().getId().equals(storeId))
                .orElseThrow(() -> new ResourceNotFoundException("Roster not found with id=" + id));
    }

    private Employee findActiveEmployee(Integer employeeId) {
        Long storeId = currentStoreService.getCurrentStoreId();
        return employeeRepository.findByIdAndStoreMember_Store_IdAndDeletedFalse(employeeId, storeId)
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

    private void validateNoShiftOverlap(Integer currentRosterId, Integer employeeId, ReqRosterDTO req) {
        if (req.getType() != RosterTypeEnum.WORKING) {
            return;
        }
        validateWorkingTimes(req);

        Long storeId = currentStoreService.getCurrentStoreId();
        List<Roster> sameDayRosters = rosterRepository
                .findByEmployee_StoreMember_Store_IdAndEmployee_IdAndWorkingDay(storeId, employeeId, req.getWorkingDay());

        boolean overlaps = sameDayRosters.stream()
                .filter(existing -> currentRosterId == null || !existing.getId().equals(currentRosterId))
                .filter(existing -> existing.getType() == RosterTypeEnum.WORKING)
                .filter(existing -> existing.getStartTime() != null && existing.getEndTime() != null)
                .anyMatch(existing -> overlaps(req.getStartTime(), req.getEndTime(), existing.getStartTime(), existing.getEndTime()));

        if (overlaps) {
            throw new BadRequestException("Ca làm bị trùng giờ với ca khác của nhân viên trong ngày này");
        }
    }

    private boolean overlaps(LocalTime startA, LocalTime endA, LocalTime startB, LocalTime endB) {
        return startA.isBefore(endB) && startB.isBefore(endA);
    }

    private void applyFields(ReqRosterDTO req, Roster roster) {
        if (req.getType() == RosterTypeEnum.WORKING) {
            validateWorkingTimes(req);
        }

        roster.setWorkingDay(req.getWorkingDay());
        roster.setType(req.getType());
        roster.setStartTime(req.getType() == RosterTypeEnum.WORKING ? req.getStartTime() : null);
        roster.setEndTime(req.getType() == RosterTypeEnum.WORKING ? req.getEndTime() : null);
        roster.setCheckInAllowedFrom(req.getType() == RosterTypeEnum.WORKING ? defaultCheckInAllowedFrom(req) : null);
        roster.setCheckInAllowedTo(req.getType() == RosterTypeEnum.WORKING ? defaultCheckInAllowedTo(req) : null);
        roster.setCheckOutAllowedFrom(req.getType() == RosterTypeEnum.WORKING ? defaultCheckOutAllowedFrom(req) : null);
        roster.setCheckOutAllowedTo(req.getType() == RosterTypeEnum.WORKING ? defaultCheckOutAllowedTo(req) : null);
        roster.setNote(req.getNote());
        roster.setUnpaidBreakMinutes(req.getUnpaidBreakMinutes() != null ? req.getUnpaidBreakMinutes() : 0L);

        if (req.getType() == RosterTypeEnum.WORKING) {
            long minutes = Duration.between(req.getStartTime(), req.getEndTime()).toMinutes();
            roster.setExpectedHours(Math.max(minutes - roster.getUnpaidBreakMinutes(), 0L) / 60.0);
        } else {
            roster.setExpectedHours(0.0);
        }
    }

    private void validateWorkingTimes(ReqRosterDTO req) {
        if (req.getStartTime() == null || req.getEndTime() == null) {
            throw new BadRequestException("Ca làm cần giờ bắt đầu và kết thúc");
        }
        if (!req.getEndTime().isAfter(req.getStartTime())) {
            throw new BadRequestException("Giờ kết thúc phải sau giờ bắt đầu");
        }
    }

    private LocalTime defaultCheckInAllowedFrom(ReqRosterDTO req) {
        return req.getCheckInAllowedFrom() != null
                ? req.getCheckInAllowedFrom()
                : req.getStartTime().minusMinutes(30);
    }

    private LocalTime defaultCheckInAllowedTo(ReqRosterDTO req) {
        return req.getCheckInAllowedTo() != null
                ? req.getCheckInAllowedTo()
                : req.getEndTime();
    }

    private LocalTime defaultCheckOutAllowedFrom(ReqRosterDTO req) {
        return req.getCheckOutAllowedFrom() != null
                ? req.getCheckOutAllowedFrom()
                : req.getStartTime();
    }

    private LocalTime defaultCheckOutAllowedTo(ReqRosterDTO req) {
        return req.getCheckOutAllowedTo() != null
                ? req.getCheckOutAllowedTo()
                : req.getEndTime();
    }
}
