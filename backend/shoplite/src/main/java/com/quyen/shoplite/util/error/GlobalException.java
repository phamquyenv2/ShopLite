package com.quyen.shoplite.util.error;

import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalException {

    private Map<String, Object> buildError(int statusCode, String message) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", Instant.now().toString());
        body.put("statusCode", statusCode);
        body.put("message", message);
        body.put("data", null);
        return body;
    }

    @ExceptionHandler(IdInvalidException.class)
    public ResponseEntity<Map<String, Object>> handleIdInvalidException(IdInvalidException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(buildError(400, e.getMessage()));
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<Map<String, Object>> handleBadRequestException(BadRequestException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(buildError(400, e.getMessage()));
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleResourceNotFoundException(ResourceNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(buildError(404, e.getMessage()));
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<Map<String, Object>> handleUnauthorizedException(UnauthorizedException e) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(buildError(401, e.getMessage()));
    }

    @ExceptionHandler(PermissionException.class)
    public ResponseEntity<Map<String, Object>> handlePermissionException(PermissionException e) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(buildError(403, e.getMessage()));
    }

    @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> handleAccessDeniedException(org.springframework.security.access.AccessDeniedException e) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(buildError(403, "Không có quyền truy cập."));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getAllErrors().stream()
                .map(error -> {
                    if (error instanceof FieldError fieldError) {
                        return fieldError.getField() + ": " + fieldError.getDefaultMessage();
                    }
                    return error.getDefaultMessage();
                })
                .sorted()
                .collect(Collectors.joining("; "));

        Map<String, Object> body = buildError(400, message);
        List<Map<String, String>> errors = e.getBindingResult().getAllErrors().stream()
                .map(error -> {
                    Map<String, String> item = new LinkedHashMap<>();
                    if (error instanceof FieldError fieldError) {
                        item.put("field", fieldError.getField());
                    } else {
                        item.put("field", error.getObjectName());
                    }
                    item.put("message", error.getDefaultMessage());
                    return item;
                })
                .sorted(Comparator.comparing(item -> item.getOrDefault("field", "")))
                .toList();
        body.put("errors", errors);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Map<String, Object>> handleConstraintViolationException(ConstraintViolationException e) {
        String message = "Dữ liệu đầu vào không hợp lệ.";
        Map<String, Object> body = buildError(400, message);
        List<Map<String, String>> errors = e.getConstraintViolations().stream()
                .map(violation -> {
                    Map<String, String> item = new LinkedHashMap<>();
                    String path = violation.getPropertyPath().toString();
                    String field = path.contains(".") ? path.substring(path.lastIndexOf('.') + 1) : path;
                    item.put("field", field);
                    item.put("message", violation.getMessage());
                    return item;
                })
                .sorted(Comparator.comparing(item -> item.getOrDefault("field", "")))
                .toList();
        body.put("errors", errors);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> handleHttpMessageNotReadableException(HttpMessageNotReadableException e) {
        String message = "Dữ liệu đầu vào không hợp lệ hoặc sai định dạng. Vui lòng kiểm tra lại.";
        if (e.getMessage() != null && e.getMessage().contains("PaymentMethodEnum")) {
            message = "Phương thức thanh toán không hợp lệ.";
        } else if (e.getMessage() != null && e.getMessage().contains("StatusEnum")) {
            message = "Trạng thái không hợp lệ.";
        } else if (e.getMessage() != null && e.getMessage().contains("RoleEnum")) {
            message = "Role không hợp lệ.";
        } else if (e.getMessage() != null && e.getMessage().contains("ImportOrderStatusEnum")) {
            message = "Trạng thái đơn nhập hàng không hợp lệ.";
        } else if (e.getMessage() != null && e.getMessage().contains("RosterTypeEnum")) {
            message = "Loại ca làm việc không hợp lệ.";
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(buildError(400, message));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(Exception e) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(buildError(500, "Đã xảy ra lỗi hệ thống, vui lòng thử lại sau."));
    }
}
