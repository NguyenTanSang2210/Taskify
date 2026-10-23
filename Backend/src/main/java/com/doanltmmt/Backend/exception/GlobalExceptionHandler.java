package com.doanltmmt.Backend.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.TypeMismatchException;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingPathVariableException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.List;

/**
 * P3-03 — mọi lỗi API trả cùng một định dạng ({@link ErrorBodies}) với thông báo tiếng Việt và mã lỗi
 * ({@link ErrorCodes}); không bao giờ lộ stacktrace, câu lệnh SQL hay thông điệp nội bộ của Java (chi tiết chỉ ghi log).
 * Lỗi 401/403 ở tầng filter bảo mật do {@code SecurityErrorHandlers} trả, cùng định dạng.
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // ------------------------------------------------------------------ lỗi do ứng dụng ném ra

    /** ResponseStatusException / ApiException / TooManyRequestsException (giữ header, vd. Retry-After). */
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Object> handleStatus(ResponseStatusException ex, HttpServletRequest req) {
        HttpStatusCode status = ex.getStatusCode();
        String code = ex instanceof ApiException api ? api.getCode() : ErrorCodes.forStatus(status);
        String message = ex.getReason() == null || ex.getReason().isBlank() ? ErrorCodes.defaultMessage(status) : ex.getReason();
        if (status.is5xxServerError()) log.error("Lỗi {} tại {}", status.value(), req.getRequestURI(), ex);
        return ErrorBodies.of(status, code, message, req.getRequestURI(), null, ex.getHeaders());
    }

    /** Ràng buộc (@Valid trên tham số đơn, @Validated service). */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Object> handleConstraintViolation(ConstraintViolationException ex, HttpServletRequest req) {
        List<ErrorBodies.FieldError> errors = ex.getConstraintViolations().stream()
                .map(v -> new ErrorBodies.FieldError(lastNode(v.getPropertyPath().toString()), v.getMessage())).toList();
        return validationFailed(errors, req.getRequestURI());
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Object> handleAccessDenied(AccessDeniedException ex, HttpServletRequest req) {
        return simple(HttpStatus.FORBIDDEN, ErrorCodes.FORBIDDEN, ErrorCodes.defaultMessage(HttpStatus.FORBIDDEN), req);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<Object> handleAuthentication(AuthenticationException ex, HttpServletRequest req) {
        return simple(HttpStatus.UNAUTHORIZED, ErrorCodes.UNAUTHORIZED, ErrorCodes.defaultMessage(HttpStatus.UNAUTHORIZED), req);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Object> handleIntegrity(DataIntegrityViolationException ex, HttpServletRequest req) {
        log.warn("Vi phạm ràng buộc dữ liệu tại {}: {}", req.getRequestURI(), ex.getMostSpecificCause().getMessage());
        return simple(HttpStatus.CONFLICT, ErrorCodes.DATA_CONFLICT, "Dữ liệu không hợp lệ hoặc bị trùng. Vui lòng kiểm tra lại.", req);
    }

    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<Object> handleDataAccess(DataAccessException ex, HttpServletRequest req) {
        log.error("Lỗi truy cập CSDL tại {}", req.getRequestURI(), ex);
        return simple(HttpStatus.INTERNAL_SERVER_ERROR, ErrorCodes.INTERNAL_ERROR, ErrorCodes.defaultMessage(HttpStatus.INTERNAL_SERVER_ERROR), req);
    }

    /** Sai kiểu dữ liệu trong body / tham số chưa được kiểm tra riêng. */
    @ExceptionHandler({ClassCastException.class, IllegalArgumentException.class})
    public ResponseEntity<Object> handleBadInput(RuntimeException ex, HttpServletRequest req) {
        log.warn("Dữ liệu không hợp lệ tại {}: {}", req.getRequestURI(), ex.toString());
        return simple(HttpStatus.BAD_REQUEST, ErrorCodes.INVALID_INPUT, "Dữ liệu gửi lên không hợp lệ hoặc sai định dạng.", req);
    }

    /** Mọi lỗi còn lại: 500 với thông báo chung, chi tiết chỉ ghi log. */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handleUnexpected(Exception ex, HttpServletRequest req) {
        log.error("Lỗi không mong đợi tại {}", req.getRequestURI(), ex);
        return simple(HttpStatus.INTERNAL_SERVER_ERROR, ErrorCodes.INTERNAL_ERROR, ErrorCodes.defaultMessage(HttpStatus.INTERNAL_SERVER_ERROR), req);
    }

    // ------------------------------------------------------------------ lỗi chuẩn của Spring MVC

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(@NonNull MethodArgumentNotValidException ex,
                                                                  @NonNull HttpHeaders headers, @NonNull HttpStatusCode status,
                                                                  @NonNull WebRequest request) {
        List<ErrorBodies.FieldError> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(f -> new ErrorBodies.FieldError(f.getField(), f.getDefaultMessage())).toList();
        if (errors.isEmpty()) {
            errors = ex.getBindingResult().getGlobalErrors().stream()
                    .map(g -> new ErrorBodies.FieldError(g.getObjectName(), g.getDefaultMessage())).toList();
        }
        return validationFailed(errors, path(request));
    }

    @Override
    protected ResponseEntity<Object> handleHandlerMethodValidationException(@NonNull HandlerMethodValidationException ex,
                                                                            @NonNull HttpHeaders headers,
                                                                            @NonNull HttpStatusCode status,
                                                                            @NonNull WebRequest request) {
        List<ErrorBodies.FieldError> errors = ex.getParameterValidationResults().stream()
                .flatMap(r -> r.getResolvableErrors().stream().map(e -> new ErrorBodies.FieldError(
                        r.getMethodParameter().getParameterName(), e.getDefaultMessage()))).toList();
        return validationFailed(errors, path(request));
    }

    @Override
    protected ResponseEntity<Object> handleExceptionInternal(@NonNull Exception ex, @Nullable Object body,
                                                             @NonNull HttpHeaders headers, @NonNull HttpStatusCode status,
                                                             @NonNull WebRequest request) {
        String code = ErrorCodes.forStatus(status);
        String message = ErrorCodes.defaultMessage(status);
        if (ex instanceof HttpMessageNotReadableException) {
            code = ErrorCodes.MALFORMED_REQUEST;
            message = "Dữ liệu gửi lên không đúng định dạng JSON.";
        } else if (ex instanceof MissingServletRequestParameterException m) {
            code = ErrorCodes.MALFORMED_REQUEST;
            message = "Thiếu tham số bắt buộc: " + m.getParameterName();
        } else if (ex instanceof MissingPathVariableException m) {
            code = ErrorCodes.MALFORMED_REQUEST;
            message = "Thiếu tham số đường dẫn: " + m.getVariableName();
        } else if (ex instanceof TypeMismatchException t) {
            code = ErrorCodes.MALFORMED_REQUEST;
            message = "Giá trị không hợp lệ cho tham số " + (t.getPropertyName() == null ? "" : "'" + t.getPropertyName() + "'");
        } else if (ex instanceof ServletRequestBindingException) {
            code = ErrorCodes.MALFORMED_REQUEST;
        } else if (ex instanceof NoResourceFoundException || ex instanceof NoHandlerFoundException) {
            message = "Không tìm thấy đường dẫn yêu cầu.";
        } else if (ex instanceof HttpRequestMethodNotSupportedException m) {
            message = "Phương thức " + m.getMethod() + " không được hỗ trợ cho đường dẫn này.";
        } else if (ex instanceof HttpMediaTypeNotSupportedException) {
            message = "Định dạng dữ liệu gửi lên không được hỗ trợ.";
        } else if (ex instanceof MaxUploadSizeExceededException) {
            code = ErrorCodes.PAYLOAD_TOO_LARGE;
            message = "File quá lớn (tối đa 20 MB).";
        }
        if (status.is5xxServerError()) log.error("Lỗi {} tại {}", status.value(), path(request), ex);
        return ErrorBodies.of(status, code, message, path(request), null, headers);
    }

    // ------------------------------------------------------------------ tiện ích

    private static ResponseEntity<Object> validationFailed(List<ErrorBodies.FieldError> errors, String path) {
        String message = errors.isEmpty() ? "Dữ liệu không hợp lệ." : errors.get(0).message();
        return ErrorBodies.of(HttpStatus.BAD_REQUEST, ErrorCodes.VALIDATION_FAILED, message, path, errors, HttpHeaders.EMPTY);
    }

    private static ResponseEntity<Object> simple(HttpStatus status, String code, String message, HttpServletRequest req) {
        return ErrorBodies.of(status, code, message, req.getRequestURI(), null, HttpHeaders.EMPTY);
    }

    private static String path(WebRequest request) {
        return request instanceof ServletWebRequest s ? s.getRequest().getRequestURI() : request.getDescription(false);
    }

    private static String lastNode(String propertyPath) {
        int dot = propertyPath.lastIndexOf('.');
        return dot < 0 ? propertyPath : propertyPath.substring(dot + 1);
    }
}
