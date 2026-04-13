package com.quyen.shoplite.domain;

import com.quyen.shoplite.util.constant.AttendanceStatusEnum;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Một lần check-in/check-out. Nhân viên có thể có nhiều bản ghi trong cùng một ngày (multi-shift).
 * Bản ghi có checkOut == null được gọi là "open" — chỉ có tối đa 1 bản ghi open tại một thời điểm.
 */
@Entity
@Table(name = "attendances")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Attendance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "office_id", nullable = false)
    private Office office;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    /**
     * Ca làm việc (Roster) tương ứng. null = walk-in (không có trong lịch).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "roster_id")
    private Roster roster;

    @Column(name = "check_in")
    private LocalDateTime checkIn;

    @Column(name = "check_out")
    private LocalDateTime checkOut;

    /** Số phút làm việc thực tế (checkOut - checkIn) */
    @Column(name = "worked_minutes")
    private Long workedMinutes;

    /**
     * Số phút được tính lương:
     *  - Có Roster: capped theo roster window, trừ late, trừ early leave.
     *  - Walk-in: = workedMinutes (tính toàn bộ thời gian làm thực tế).
     *  - Nếu check-in trước giờ bắt đầu ca (không phải OT): bắt đầu tính từ roster.startTime.
     */
    @Column(name = "payable_minutes")
    private Long payableMinutes;

    /** Ngày làm việc (date part của checkIn, dùng cho queries theo ngày) */
    @Column(name = "working_day", nullable = false)
    private LocalDate workingDay;

    /** true = không có trong lịch làm (roster == null) */
    @Column(name = "walk_in", nullable = false)
    @Builder.Default
    private boolean walkIn = false;

    /** Check-in latitude */
    @Column(precision = 10, scale = 8)
    private BigDecimal latitude;

    /** Check-in longitude */
    @Column(precision = 11, scale = 8)
    private BigDecimal longitude;

    /** Distance from office in meters at check-in */
    @Column
    private Double distance;

    /** Check-out latitude */
    @Column(name = "checkout_latitude", precision = 10, scale = 8)
    private BigDecimal checkOutLatitude;

    /** Check-out longitude */
    @Column(name = "checkout_longitude", precision = 11, scale = 8)
    private BigDecimal checkOutLongitude;

    /** Distance from office in meters at check-out */
    @Column(name = "checkout_distance")
    private Double checkOutDistance;

    /**
     * Số phút đi trễ so với roster.startTime (sau khi trừ gracePeriod của văn phòng).
     * 0 nếu walk-in hoặc đến đúng giờ / trong grace period.
     */
    @Column(name = "late_minutes")
    @Builder.Default
    private Long lateMinutes = 0L;

    /**
     * Số phút về sớm so với roster.endTime.
     * 0 nếu walk-in hoặc check-out sau giờ kết thúc ca.
     */
    @Column(name = "early_leave_minutes")
    @Builder.Default
    private Long earlyLeaveMinutes = 0L;

    /** Whether the record was closed automatically by the scheduler */
    @Column(name = "closed_automatically")
    @Builder.Default
    private boolean closedAutomatically = false;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AttendanceStatusEnum status;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) {
            createdAt = now;
        }
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}

