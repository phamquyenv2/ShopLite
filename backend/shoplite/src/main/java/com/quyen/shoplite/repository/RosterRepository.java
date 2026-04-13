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

    /** Lấy toàn bộ lịch của một nhân viên trong khoảng ngày */
    List<Roster> findByEmployee_IdAndWorkingDayBetweenOrderByWorkingDayAsc(
            Integer employeeId, LocalDate from, LocalDate to);

    /** Lấy lịch của một nhân viên trong một ngày cụ thể */
    Optional<Roster> findByEmployee_IdAndWorkingDay(Integer employeeId, LocalDate workingDay);

    /** Kiểm tra lịch đã tồn tại chưa (tránh trùng) */
    boolean existsByEmployee_IdAndWorkingDay(Integer employeeId, LocalDate workingDay);

    /** Lấy lịch theo type trong khoảng thời gian (dùng khi tổng hợp) */
    List<Roster> findByEmployee_IdAndWorkingDayBetweenAndType(
            Integer employeeId, LocalDate from, LocalDate to, RosterTypeEnum type);

    /** Tất cả lịch trong một ngày (cho admin xem tổng quan) */
    List<Roster> findByWorkingDayOrderByEmployee_IdAsc(LocalDate workingDay);

    /**
     * Tìm Roster phù hợp với giờ check-in của nhân viên.
     * Check-in window: [roster.startTime - 30 phút, roster.endTime]
     * Chỉ áp dụng cho ngày có type = WORKING.
     *
     * @param employeeId Employee ID
     * @param day        Ngày làm việc
     * @param checkInTime Thời điểm check-in thực tế
     * @param windowStart startTime - 30 phút (truyền vào ngoài)
     */
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
