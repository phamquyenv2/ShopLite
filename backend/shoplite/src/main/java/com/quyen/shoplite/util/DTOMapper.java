package com.quyen.shoplite.util;

import com.quyen.shoplite.domain.*;
import com.quyen.shoplite.domain.response.*;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Utility class để chuyển đổi (mapping) giữa Entity và DTO.
 */
public class DTOMapper {

    private DTOMapper() {
    }

    // ==================== User ====================
    public static ResUserDTO toResUserDTO(User user) {
        if (user == null) {
            return null;
        }
        ResUserDTO dto = new ResUserDTO();
        dto.setId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setActive(user.isActive());
        dto.setCreatedAt(user.getCreatedAt());
        if (user.getRole() != null) {
            dto.setRoleId(user.getRole().getId());
            dto.setRoleName(user.getRole().getName());
        }
        return dto;
    }

    // ==================== Permission ====================
    public static ResPermissionDTO toResPermissionDTO(Permission p) {
        if (p == null) {
            return null;
        }
        ResPermissionDTO dto = new ResPermissionDTO();
        dto.setId(p.getId());
        dto.setName(p.getName());
        dto.setApiPath(p.getApiPath());
        dto.setMethod(p.getMethod());
        dto.setModule(p.getModule());
        dto.setCreatedAt(p.getCreatedAt());
        dto.setUpdatedAt(p.getUpdatedAt());
        dto.setCreatedBy(p.getCreatedBy());
        dto.setUpdatedBy(p.getUpdatedBy());
        return dto;
    }

    // ==================== Role ====================
    public static ResRoleDTO toResRoleDTO(Role role) {
        if (role == null) {
            return null;
        }
        ResRoleDTO dto = new ResRoleDTO();
        dto.setId(role.getId());
        dto.setName(role.getName());
        dto.setDescription(role.getDescription());
        dto.setActive(role.isActive());
        dto.setCreatedAt(role.getCreatedAt());
        dto.setUpdatedAt(role.getUpdatedAt());
        dto.setCreatedBy(role.getCreatedBy());
        dto.setUpdatedBy(role.getUpdatedBy());
        if (role.getPermissions() != null) {
            dto.setPermissions(role.getPermissions().stream()
                    .map(DTOMapper::toResPermissionDTO).toList());
        }
        return dto;
    }

    // ==================== Category ====================
    public static ResCategoryDTO toResCategoryDTO(Category category) {
        if (category == null) {
            return null;
        }
        ResCategoryDTO dto = new ResCategoryDTO();
        dto.setId(category.getId());
        dto.setName(category.getName());
        return dto;
    }

    // ==================== Unit ====================
    public static ResUnitDTO toResUnitDTO(Unit unit) {
        if (unit == null) {
            return null;
        }
        ResUnitDTO dto = new ResUnitDTO();
        dto.setId(unit.getId());
        dto.setName(unit.getName());
        dto.setDescription(unit.getDescription());
        return dto;
    }

    // ==================== Product ====================
    public static ResProductDTO toResProductDTO(Product product) {
        if (product == null) {
            return null;
        }
        ResProductDTO dto = new ResProductDTO();
        dto.setId(product.getId());
        dto.setName(product.getName());
        dto.setSku(product.getSku());
        dto.setBarcode(product.getBarcode());
        dto.setStock(product.getStock());
        dto.setCostPrice(product.getCostPrice());
        dto.setSellingPrice(product.getSellingPrice());
        dto.setMinStock(product.getMinStock());
        dto.setMaxStock(product.getMaxStock());
        dto.setImage(product.getImage());
        dto.setStatus(product.getStatus());
        dto.setDeleted(product.isDeleted());
        dto.setCreatedAt(product.getCreatedAt());
        dto.setUpdatedAt(product.getUpdatedAt());
        dto.setVersion(product.getVersion());
        if (product.getCategory() != null) {
            dto.setCategoryId(product.getCategory().getId());
            dto.setCategoryName(product.getCategory().getName());
        }
        if (product.getUnit() != null) {
            dto.setUnitId(product.getUnit().getId());
            dto.setUnitName(product.getUnit().getName());
        }
        return dto;
    }

    // ==================== Customer ====================
    public static ResCustomerDTO toResCustomerDTO(Customer customer) {
        if (customer == null) {
            return null;
        }
        ResCustomerDTO dto = new ResCustomerDTO();
        dto.setId(customer.getId());
        dto.setName(customer.getName());
        dto.setPhone(customer.getPhone());
        dto.setPoints(customer.getPoints());
        return dto;
    }

    // ==================== Supplier ====================
    public static ResSupplierDTO toResSupplierDTO(Supplier supplier) {
        if (supplier == null) {
            return null;
        }
        ResSupplierDTO dto = new ResSupplierDTO();
        dto.setId(supplier.getId());
        dto.setName(supplier.getName());
        dto.setPhone(supplier.getPhone());
        dto.setAddress(supplier.getAddress());
        dto.setEmail(supplier.getEmail());
        dto.setCreatedAt(supplier.getCreatedAt());
        return dto;
    }

    // ==================== Office ====================
    public static ResOfficeDTO toResOfficeDTO(Office office) {
        if (office == null) {
            return null;
        }
        ResOfficeDTO dto = new ResOfficeDTO();
        dto.setId(office.getId());
        dto.setName(office.getName());
        dto.setOfficeLat(office.getOfficeLat());
        dto.setOfficeLng(office.getOfficeLng());
        dto.setRadius(office.getRadius());
        dto.setShiftStart(office.getShiftStart());
        dto.setShiftEnd(office.getShiftEnd());
        dto.setLateGraceMinutes(office.getLateGraceMinutes());
        dto.setAutoCheckoutTime(office.getAutoCheckoutTime());
        return dto;
    }

    // ==================== Employee ====================
    public static ResEmployeeDTO toResEmployeeDTO(Employee employee) {
        if (employee == null) {
            return null;
        }
        ResEmployeeDTO dto = new ResEmployeeDTO();
        dto.setId(employee.getId());
        dto.setSalaryRate(employee.getSalaryRate());
        dto.setQr(employee.getQr());
        dto.setNote(employee.getNote());
        dto.setDeleted(employee.isDeleted());
        if (employee.getUser() != null) {
            dto.setUserId(employee.getUser().getId());
            dto.setUsername(employee.getUser().getUsername());
        }
        if (employee.getOffice() != null) {
            dto.setOfficeId(employee.getOffice().getId());
            dto.setOfficeName(employee.getOffice().getName());
        }
        return dto;
    }

    // ==================== Attendance ====================
    public static ResAttendanceDTO toResAttendanceDTO(Attendance attendance) {
        if (attendance == null) {
            return null;
        }
        ResAttendanceDTO dto = new ResAttendanceDTO();
        dto.setId(attendance.getId());
        dto.setCheckIn(attendance.getCheckIn());
        dto.setCheckOut(attendance.getCheckOut());
        dto.setWorkedMinutes(attendance.getWorkedMinutes());
        dto.setPayableMinutes(attendance.getPayableMinutes());
        dto.setWorkingDay(attendance.getWorkingDay());
        dto.setWalkIn(attendance.isWalkIn());
        dto.setLatitude(attendance.getLatitude());
        dto.setLongitude(attendance.getLongitude());
        dto.setDistance(attendance.getDistance());
        dto.setCheckOutLatitude(attendance.getCheckOutLatitude());
        dto.setCheckOutLongitude(attendance.getCheckOutLongitude());
        dto.setCheckOutDistance(attendance.getCheckOutDistance());
        dto.setLateMinutes(attendance.getLateMinutes());
        dto.setEarlyLeaveMinutes(attendance.getEarlyLeaveMinutes());
        dto.setClosedAutomatically(attendance.isClosedAutomatically());
        dto.setStatus(attendance.getStatus());
        if (attendance.getRoster() != null) {
            dto.setRosterId(attendance.getRoster().getId());
        }
        if (attendance.getEmployee() != null) {
            dto.setEmployeeId(attendance.getEmployee().getId());
            if (attendance.getEmployee().getUser() != null) {
                dto.setEmployeeUsername(attendance.getEmployee().getUser().getUsername());
            }
        }
        if (attendance.getOffice() != null) {
            dto.setOfficeId(attendance.getOffice().getId());
            dto.setOfficeName(attendance.getOffice().getName());
        }
        return dto;
    }

    // ==================== Payroll ====================
    public static ResPayrollDTO toResPayrollDTO(Payroll payroll) {
        if (payroll == null) {
            return null;
        }
        ResPayrollDTO dto = new ResPayrollDTO();
        dto.setId(payroll.getId());
        dto.setPeriod(payroll.getPeriod());
        dto.setSalaryRate(payroll.getSalaryRate());
        dto.setTotalHours(payroll.getTotalHours());
        dto.setBonus(payroll.getBonus());
        dto.setPenalty(payroll.getPenalty());
        dto.setTotalSalary(payroll.getTotalSalary());
        if (payroll.getEmployee() != null) {
            dto.setEmployeeId(payroll.getEmployee().getId());
            if (payroll.getEmployee().getUser() != null) {
                dto.setEmployeeUsername(payroll.getEmployee().getUser().getUsername());
            }
        }
        return dto;
    }

    // ==================== Payment ====================
    public static ResPaymentDTO toResPaymentDTO(Payment payment) {
        if (payment == null) {
            return null;
        }
        ResPaymentDTO dto = new ResPaymentDTO();
        dto.setId(payment.getId());
        dto.setMethod(payment.getMethod());
        dto.setAmount(payment.getAmount());
        dto.setStatus(payment.getStatus());
        dto.setCreatedAt(payment.getCreatedAt());
        if (payment.getOrder() != null) {
            dto.setOrderId(payment.getOrder().getId());
            dto.setOrderCode(payment.getOrder().getCode());
        }
        return dto;
    }

    // ==================== ImportItem ====================
    public static ResImportItemDTO toResImportItemDTO(ImportItem item) {
        if (item == null) {
            return null;
        }
        ResImportItemDTO dto = new ResImportItemDTO();
        dto.setId(item.getId());
        dto.setQuantity(item.getQuantity());
        dto.setImportPrice(item.getImportPrice());
        dto.setSubTotal(item.getSubTotal());
        if (item.getProduct() != null) {
            dto.setProductId(item.getProduct().getId());
            dto.setProductName(item.getProduct().getName());
            dto.setProductSku(item.getProduct().getSku());
        }
        return dto;
    }

    // ==================== ImportOrder ====================
    public static ResImportOrderDTO toResImportOrderDTO(ImportOrder importOrder, List<ImportItem> items) {
        if (importOrder == null) {
            return null;
        }
        ResImportOrderDTO dto = new ResImportOrderDTO();
        dto.setId(importOrder.getId());
        dto.setTax(importOrder.getTax());
        dto.setDiscount(importOrder.getDiscount());
        dto.setTotalAmount(importOrder.getTotalAmount());
        dto.setAmountPaid(importOrder.getAmountPaid());
        dto.setStatus(importOrder.getStatus());
        dto.setNote(importOrder.getNote());
        dto.setCreatedAt(importOrder.getCreatedAt());
        if (importOrder.getSupplier() != null) {
            dto.setSupplierId(importOrder.getSupplier().getId());
            dto.setSupplierName(importOrder.getSupplier().getName());
        }
        if (items != null) {
            dto.setItems(items.stream().map(DTOMapper::toResImportItemDTO).collect(Collectors.toList()));
        } else {
            dto.setItems(Collections.emptyList());
        }
        return dto;
    }

    // ==================== InventoryAdjustment ====================
    public static ResInventoryAdjustmentDTO toResInventoryAdjustmentDTO(InventoryAdjustment adjustment, List<InventoryLogs> logs) {
        if (adjustment == null) {
            return null;
        }
        ResInventoryAdjustmentDTO dto = new ResInventoryAdjustmentDTO();
        dto.setId(adjustment.getId());
        dto.setReason(adjustment.getReason());
        dto.setNote(adjustment.getNote());
        dto.setCreatedBy(adjustment.getCreatedBy());
        dto.setCreatedAt(adjustment.getCreatedAt());
        if (logs != null) {
            dto.setLogs(logs.stream().map(DTOMapper::toResInventoryLogDTO).collect(Collectors.toList()));
        } else {
            dto.setLogs(Collections.emptyList());
        }
        return dto;
    }

    // ==================== OrderItems ====================
    public static ResOrderItemDTO toResOrderItemDTO(OrderItems item) {
        if (item == null) {
            return null;
        }
        ResOrderItemDTO dto = new ResOrderItemDTO();
        dto.setId(item.getId());
        dto.setProductName(item.getProductName());
        dto.setQuantity(item.getQuantity());
        dto.setPrice(item.getPrice());
        dto.setTotalPrice(item.getTotalPrice());
        if (item.getProduct() != null) {
            dto.setProductId(item.getProduct().getId());
        }
        return dto;
    }

    // ==================== Order ====================
    public static ResOrderDTO toResOrderDTO(Order order) {
        if (order == null) {
            return null;
        }
        ResOrderDTO dto = new ResOrderDTO();
        dto.setId(order.getId());
        dto.setCode(order.getCode());
        dto.setRequestId(order.getRequestId());
        dto.setTotalAmount(order.getTotalAmount());
        dto.setDiscount(order.getDiscount());
        dto.setStatus(order.getStatus());
        dto.setCreatedAt(order.getCreatedAt());
        dto.setPaidAt(order.getPaidAt());
        if (order.getCustomer() != null) {
            dto.setCustomerId(order.getCustomer().getId());
            dto.setCustomerName(order.getCustomer().getName());
        }
        if (order.getUser() != null) {
            dto.setUserId(order.getUser().getId());
            dto.setUsername(order.getUser().getUsername());
        }
        return dto;
    }

    // ==================== Transaction ====================
    public static ResTransactionDTO toResTransactionDTO(Transaction transaction) {
        if (transaction == null) {
            return null;
        }
        ResTransactionDTO dto = new ResTransactionDTO();
        dto.setId(transaction.getId());
        dto.setAmount(transaction.getAmount());
        dto.setType(transaction.getType());
        dto.setContent(transaction.getContent());
        dto.setTransactionTime(transaction.getTransactionTime());
        dto.setCreatedAt(transaction.getCreatedAt());
        if (transaction.getOrder() != null) {
            dto.setOrderId(transaction.getOrder().getId());
            dto.setOrderCode(transaction.getOrder().getCode());
        }
        if (transaction.getImportOrder() != null) {
            dto.setImportOrderId(transaction.getImportOrder().getId());
        }
        if (transaction.getPayment() != null) {
            dto.setPaymentId(transaction.getPayment().getId());
        }
        if (transaction.getPayroll() != null) {
            dto.setPayrollId(transaction.getPayroll().getId());
        }
        return dto;
    }

    // ==================== InventoryLogs ====================
    public static ResInventoryLogDTO toResInventoryLogDTO(InventoryLogs log) {
        if (log == null) {
            return null;
        }
        ResInventoryLogDTO dto = new ResInventoryLogDTO();
        dto.setId(log.getId());
        dto.setQuantityIn(log.getQuantityIn());
        dto.setQuantityOut(log.getQuantityOut());
        dto.setBalanceAfter(log.getBalanceAfter());
        dto.setCurrentStock(log.getCurrentStock());
        dto.setType(log.getType());
        dto.setCreatedAt(log.getCreatedAt());
        if (log.getProduct() != null) {
            dto.setProductId(log.getProduct().getId());
            dto.setProductName(log.getProduct().getName());
            dto.setProductSku(log.getProduct().getSku());
        }
        return dto;
    }

    // ==================== Roster ====================
    public static ResRosterDTO toResRosterDTO(Roster roster) {
        if (roster == null) {
            return null;
        }
        ResRosterDTO dto = new ResRosterDTO();
        dto.setId(roster.getId());
        dto.setWorkingDay(roster.getWorkingDay());
        dto.setStartTime(roster.getStartTime());
        dto.setEndTime(roster.getEndTime());
        dto.setExpectedHours(roster.getExpectedHours());
        dto.setType(roster.getType());
        dto.setNote(roster.getNote());
        dto.setUnpaidBreakMinutes(roster.getUnpaidBreakMinutes());
        if (roster.getEmployee() != null) {
            dto.setEmployeeId(roster.getEmployee().getId());
            if (roster.getEmployee().getUser() != null) {
                dto.setEmployeeUsername(roster.getEmployee().getUser().getUsername());
            }
        }
        return dto;
    }
}
