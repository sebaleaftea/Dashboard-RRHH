package com.microservice_employee.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(TalanaUpstreamException.class)
    public ResponseEntity<Map<String, Object>> handleTalanaUpstream(TalanaUpstreamException ex) {
        int status = ex.getStatus();
        HttpStatus httpStatus;
        try {
            httpStatus = HttpStatus.valueOf(status);
        } catch (Exception ignored) {
            httpStatus = HttpStatus.BAD_GATEWAY;
            status = httpStatus.value();
        }

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("error", "talana_upstream_error");
        body.put("status", status);
        body.put("message", ex.getMessage());

        return ResponseEntity.status(httpStatus).body(body);
    }

    /**
     * Captura IOException lanzadas por el cliente Talana y las mapea a un HTTP adecuado,
     * extrayendo el código desde el mensaje (formato: "HTTP <code> calling ...").
     */
    @ExceptionHandler(java.io.IOException.class)
    public ResponseEntity<Map<String, Object>> handleIOException(java.io.IOException ex) {
        int status = parseStatusCode(ex.getMessage());
        HttpStatus httpStatus;
        try {
            httpStatus = HttpStatus.valueOf(status);
        } catch (Exception ignored) {
            httpStatus = HttpStatus.BAD_GATEWAY;
            status = httpStatus.value();
        }

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("error", "talana_upstream_error");
        body.put("status", status);
        body.put("message", ex.getMessage());

        return ResponseEntity.status(httpStatus).body(body);
    }

    private static int parseStatusCode(String message) {
        if (message == null) return 502;
        // Busca patrón "HTTP <code> " al inicio del mensaje
        String prefix = "HTTP ";
        int idx = message.indexOf(prefix);
        if (idx >= 0) {
            int start = idx + prefix.length();
            // Lee hasta el siguiente espacio
            int end = message.indexOf(' ', start);
            String codeStr = end > start ? message.substring(start, end) : message.substring(start).trim();
            try {
                return Integer.parseInt(codeStr);
            } catch (NumberFormatException ignored) {
                // continúa
            }
        }
        return 502;
    }
}
