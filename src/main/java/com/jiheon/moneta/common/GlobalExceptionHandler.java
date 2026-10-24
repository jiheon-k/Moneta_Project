package com.jiheon.moneta.common;

import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @Override
    protected ResponseEntity<Object> handleExceptionInternal(Exception ex, Object body,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        HttpStatus httpStatus = HttpStatus.resolve(status.value());
        String code = httpStatus == null ? "HTTP_" + status.value() : httpStatus.name();
        String message = defaultMessage(status.value());

        if (ex instanceof ResponseStatusException statusException && statusException.getReason() != null) {
            message = statusException.getReason();
        } else if (ex instanceof MethodArgumentNotValidException validationException) {
            message = validationException.getBindingResult().getFieldErrors().stream()
                    .map(error -> error.getField() + ": " + error.getDefaultMessage())
                    .distinct()
                    .sorted()
                    .collect(Collectors.joining(", "));
            if (message.isEmpty()) {
                message = "입력값을 확인해 주세요.";
            }
        } else if (ex instanceof HttpMessageNotReadableException) {
            message = "요청 본문의 JSON 형식과 필드 값을 확인해 주세요.";
        }

        return super.handleExceptionInternal(ex, new ErrorResponse(code, message), headers, status, request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpectedException(Exception ex) {
        log.error("요청 처리 중 예상하지 못한 오류가 발생했습니다. 오류 유형: {}", ex.getClass().getName());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("INTERNAL_SERVER_ERROR", defaultMessage(500)));
    }

    private String defaultMessage(int status) {
        return switch (status) {
            case 400 -> "요청 값이나 형식을 확인해 주세요.";
            case 401 -> "로그인이 필요합니다.";
            case 403 -> "변경 권한이 없습니다.";
            case 404 -> "대상을 찾을 수 없습니다.";
            case 405 -> "지원하지 않는 HTTP 메서드입니다.";
            case 409 -> "현재 상태에서는 요청을 처리할 수 없습니다.";
            case 415 -> "지원하지 않는 요청 형식입니다.";
            default -> status >= 500 ? "서버 오류가 발생했습니다." : "요청을 처리할 수 없습니다.";
        };
    }
}
