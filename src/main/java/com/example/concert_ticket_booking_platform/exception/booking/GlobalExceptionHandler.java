package com.example.concert_ticket_booking_platform.exception.booking;

import com.example.concert_ticket_booking_platform.dto.booking.ErrorResponse;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestControllerAdvice(name = "bookingGlobalExceptionHandler")
public class GlobalExceptionHandler {


    // --- 409: hết vé / xung đột trừ kho ---
    @ExceptionHandler(InsufficientTicketException.class)
    public ResponseEntity<ErrorResponse> handleInsufficientTicket(InsufficientTicketException ex) {
        return build(HttpStatus.CONFLICT, "OUT_OF_STOCK", ex.getMessage(), null);
    }

    // --- 409: Idempotency-Key trùng nhưng payload khác ---
    @ExceptionHandler(IdempotencyConflictException.class)
    public ResponseEntity<ErrorResponse> handleIdempotencyConflict(IdempotencyConflictException ex) {
        return build(HttpStatus.CONFLICT, "IDEMPOTENCY_CONFLICT", ex.getMessage(), null);
    }

    // --- 400: voucher hết lượt/hết hạn/vượt giới hạn user ---
    @ExceptionHandler(VoucherInvalidException.class)
    public ResponseEntity<ErrorResponse> handleVoucherInvalid(VoucherInvalidException ex) {
        return build(HttpStatus.BAD_REQUEST, "VOUCHER_INVALID", ex.getMessage(), null);
    }

    // --- 400: thiếu Idempotency-Key header ---
    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<ErrorResponse> handleMissingHeader(MissingRequestHeaderException ex) {
        return build(HttpStatus.BAD_REQUEST, "MISSING_HEADER", ex.getMessage(), null);
    }

    // --- 400: thiếu input / sai kiểu dữ liệu (@Valid trên @RequestBody) ---
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        List<String> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .toList();
        return build(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Dữ liệu không hợp lệ", errors);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(ConstraintViolationException ex) {
        return build(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", ex.getMessage(), null);
    }

    // --- fallback: lỗi không lường trước -> 500 (Hiện chi tiết lỗi để Debug) ---
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex) {
        // 1. In vết lỗi đỏ lè ra Console của IntelliJ/Eclipse để soi
        ex.printStackTrace();

        // 2. Lấy nguyên nhân chi tiết
        String detailMessage = ex.getMessage() != null ? ex.getMessage() : ex.toString();

        // 3. Trả về message chi tiết cho Client thay vì câu chung chung
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR",
                "Lỗi Server: " + ex.getClass().getSimpleName() + " - " + detailMessage, null);
    }
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<?> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        String message = String.format("Tham số '%s' không đúng định dạng (Giá trị nhận được: '%s')",
                ex.getPropertyName(), ex.getValue());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("message", message));
    }

    private ResponseEntity<ErrorResponse> build(HttpStatus status, String code, String message, List<String> errors) {
        ErrorResponse body = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(status.value())
                .code(code)
                .message(message)
                .errors(errors)
                .build();
        return ResponseEntity.status(status).body(body);
    }
}
