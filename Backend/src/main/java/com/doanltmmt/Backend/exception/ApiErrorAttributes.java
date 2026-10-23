package com.doanltmmt.Backend.exception;

import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.boot.web.servlet.error.DefaultErrorAttributes;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.WebRequest;

import java.util.Map;

/**
 * Lỗi không đi qua controller (vd. lỗi ở filter) được Spring Boot trả qua {@code /error}: thêm {@code code} và thông báo
 * tiếng Việt để cùng định dạng với {@link GlobalExceptionHandler}.
 */
@Component
public class ApiErrorAttributes extends DefaultErrorAttributes {

    @Override
    public Map<String, Object> getErrorAttributes(WebRequest webRequest, ErrorAttributeOptions options) {
        Map<String, Object> attrs = super.getErrorAttributes(webRequest, options);
        Object raw = attrs.get("status");
        HttpStatus status = raw instanceof Integer i ? HttpStatus.resolve(i) : null;
        if (status == null) status = HttpStatus.INTERNAL_SERVER_ERROR;
        attrs.put("code", ErrorCodes.forStatus(status));
        Object message = attrs.get("message");
        if (message == null || message.toString().isBlank() || status.is5xxServerError() || "No message available".equals(message)) {
            attrs.put("message", ErrorCodes.defaultMessage(status));
        }
        attrs.remove("trace");
        attrs.remove("exception");
        return attrs;
    }
}
