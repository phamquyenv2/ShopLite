package com.quyen.shoplite.repository;

import com.quyen.shoplite.domain.Attendance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface AttendanceRepository extends JpaRepository<Attendance, Integer> {

    /**
     * Tìm ca đang mở (chưa check-out) của nhân viên.
     * Dùng để chặn check-in mới khi chưa check-out ca trước.
     */
    Optional<Attendance> findByEmployee_IdAndCheckOutIsNull(Integer employeeId);

    /** Tất cả attendance của nhân viên trong một ngày, mới nhất trước */
    List<Attendance> findByEmployee_IdAndWorkingDayOrderByCheckInDesc(Integer employeeId, LocalDate workingDay);

    /** Most-recent attendance for an employee regardless of day (for scheduler) */
    Optional<Attendance> findTopByEmployee_IdOrderByCheckInDesc(Integer employeeId);

    /** All attendance records ordered newest first */
    List<Attendance> findAllByOrderByWorkingDayDescCheckInDesc();

    /** Completed attendances (có check-out) trong khoảng ngày — dùng để tính payroll */
    @Query("SELECT a FROM Attendance a WHERE a.employee.id = :eid " +
           "AND a.workingDay BETWEEN :from AND :to " +
           "AND a.checkOut IS NOT NULL")
    List<Attendance> findCompletedByEmployeeAndPeriod(
            @Param("eid") Integer employeeId,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to);

    /** Tất cả attendance chưa check-out trong ngày hôm nay hoặc trước đó — cho auto-checkout job */
    List<Attendance> findByCheckOutIsNullAndWorkingDayLessThanEqual(LocalDate workingDay);
}
