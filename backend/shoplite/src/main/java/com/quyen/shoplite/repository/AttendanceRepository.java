package com.quyen.shoplite.repository;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.quyen.shoplite.domain.Attendance;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface AttendanceRepository extends JpaRepository<Attendance, Integer> {

    Optional<Attendance> findByEmployee_IdAndCheckOutIsNull(Integer employeeId);
    Optional<Attendance> findByEmployee_StoreMember_Store_IdAndEmployee_IdAndCheckOutIsNull(Long storeId, Integer employeeId);
    boolean existsByRoster_Id(Integer rosterId);

    List<Attendance> findByEmployee_IdAndWorkingDayOrderByCheckInDesc(Integer employeeId, LocalDate workingDay);
    List<Attendance> findByEmployee_StoreMember_Store_IdAndEmployee_IdAndWorkingDayOrderByCheckInDesc(
            Long storeId, Integer employeeId, LocalDate workingDay);

    Optional<Attendance> findTopByEmployee_IdOrderByCheckInDesc(Integer employeeId);
    Optional<Attendance> findTopByEmployee_StoreMember_Store_IdAndEmployee_IdOrderByCheckInDesc(Long storeId, Integer employeeId);

    List<Attendance> findAllByOrderByWorkingDayDescCheckInDesc();

    @EntityGraph(attributePaths = {"employee", "employee.storeMember", "employee.storeMember.user", "office", "roster"})
    List<Attendance> findAllByEmployee_StoreMember_Store_IdOrderByWorkingDayDescCheckInDesc(Long storeId);

    @Query("SELECT a FROM Attendance a WHERE a.employee.id = :eid " +
           "AND a.workingDay BETWEEN :from AND :to " +
           "AND a.checkOut IS NOT NULL")
    List<Attendance> findCompletedByEmployeeAndPeriod(
            @Param("eid") Integer employeeId,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to);

    List<Attendance> findByCheckOutIsNullAndWorkingDayLessThanEqual(LocalDate workingDay);
}
