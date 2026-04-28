package com.quyen.shoplite.repository;

import com.quyen.shoplite.domain.Roster;
import com.quyen.shoplite.util.constant.RosterTypeEnum;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface RosterRepository extends JpaRepository<Roster, Integer> {

    List<Roster> findByEmployee_IdAndWorkingDayBetweenOrderByWorkingDayAsc(
            Integer employeeId, LocalDate from, LocalDate to);
    List<Roster> findByEmployee_StoreMember_Store_IdAndEmployee_IdAndWorkingDayBetweenOrderByWorkingDayAsc(
            Long storeId, Integer employeeId, LocalDate from, LocalDate to);

    Optional<Roster> findByEmployee_IdAndWorkingDay(Integer employeeId, LocalDate workingDay);
    Optional<Roster> findByEmployee_StoreMember_Store_IdAndEmployee_IdAndWorkingDay(Long storeId, Integer employeeId, LocalDate workingDay);

    boolean existsByEmployee_IdAndWorkingDay(Integer employeeId, LocalDate workingDay);
    boolean existsByEmployee_StoreMember_Store_IdAndEmployee_IdAndWorkingDay(Long storeId, Integer employeeId, LocalDate workingDay);

    List<Roster> findByEmployee_IdAndWorkingDayBetweenAndType(
            Integer employeeId, LocalDate from, LocalDate to, RosterTypeEnum type);

    List<Roster> findByWorkingDayOrderByEmployee_IdAsc(LocalDate workingDay);
    List<Roster> findByEmployee_StoreMember_Store_IdAndWorkingDayOrderByEmployee_IdAsc(Long storeId, LocalDate workingDay);

    @Query("SELECT r FROM Roster r WHERE r.employee.id = :eid " +
           "AND r.workingDay = :day " +
           "AND r.type = 'WORKING' " +
           "AND r.startTime IS NOT NULL " +
           "AND r.endTime IS NOT NULL " +
           "AND :windowStart <= r.endTime " +
           "AND :checkInTime >= :windowStart " +
           "ORDER BY ABS(FUNCTION('TIMESTAMPDIFF', MINUTE, r.startTime, :checkInTime)) ASC")
    List<Roster> findMatchingRoster(
            @Param("eid") Integer employeeId,
            @Param("day") LocalDate day,
            @Param("checkInTime") LocalTime checkInTime,
            @Param("windowStart") LocalTime windowStart);
}
