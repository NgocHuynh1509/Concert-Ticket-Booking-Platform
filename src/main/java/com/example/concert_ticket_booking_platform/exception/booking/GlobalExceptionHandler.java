package com.example.concert_ticket_booking_platform.exception.booking;

import com.example.concert_ticket_booking_platform.dto.booking.ErrorResponse;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
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

    // --- 404: ticketCategoryId / concertId / bookingId / voucherCode không tồn tại ---
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFound(ResourceNotFoundException ex) {
        return build(HttpStatus.NOT_FOUND, "NOT_FOUND", ex.getMessage(), null);
    }

    // --- 403: user đăng nhập cố xem/thao tác booking không phải của mình ---
    // Dùng Spring Security AccessDeniedException để tương thích luôn với @PreAuthorize
    // ở tầng Controller (vd @PreAuthorize("hasRole('OPERATOR')")) mà không cần thêm class riêng.
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex) {
        return build(HttpStatus.FORBIDDEN, "FORBIDDEN", "Bạn không có quyền truy cập tài nguyên này", null);
    }

    // --- 401: sai username/password lúc login ---
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleBadCredentials(BadCredentialsException ex) {
        return build(HttpStatus.UNAUTHORIZED, "BAD_CREDENTIALS", "Sai username hoặc password", null);
    }

    // --- 401: fallback cho các lỗi authentication khác (token hết hạn xử lý ở JwtAuthEntryPoint,
    // đây là lưới an toàn cho case AuthenticationException ném ra trong luồng bình thường) ---
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthentication(AuthenticationException ex) {
        return build(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "Yêu cầu đăng nhập để thực hiện thao tác này", null);
    }

    // --- 409: đã retry hết số lần cho phép ở tầng service mà vẫn conflict version
    // (2 request cùng trừ kho / cùng dùng voucher tại cùng 1 thời điểm) ---
    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<ErrorResponse> handleOptimisticLock(ObjectOptimisticLockingFailureException ex) {
        return build(HttpStatus.CONFLICT, "CONCURRENT_CONFLICT",
                "Hệ thống đang xử lý nhiều yêu cầu cùng lúc, vui lòng thử lại", null);
    }

    // --- 400: body JSON thiếu/sai định dạng (vd gửi quantity: "abc", hoặc body rỗng) ---
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleMalformedJson(HttpMessageNotReadableException ex) {
        return build(HttpStatus.BAD_REQUEST, "MALFORMED_REQUEST_BODY",
                "Request body thiếu hoặc không đúng định dạng JSON", null);
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

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<?> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        String message = String.format("Tham số '%s' không đúng định dạng (Giá trị nhận được: '%s')",
                ex.getPropertyName(), ex.getValue());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("message", message));
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