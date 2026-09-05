package com.apisentinel.common.exception;

import com.apisentinel.common.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

        private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

        @ExceptionHandler(MethodArgumentNotValidException.class)
        public ResponseEntity<ErrorResponse> handleValidationExceptions(
                        MethodArgumentNotValidException ex,
                        HttpServletRequest request) {
                Map<String, String> errors = new HashMap<>();
                ex.getBindingResult().getAllErrors().forEach(error -> {
                        String fieldName = ((FieldError) error).getField();
                        String errorMessage = error.getDefaultMessage();
                        errors.put(fieldName, errorMessage);
                });

                ErrorResponse response = new ErrorResponse(
                                HttpStatus.BAD_REQUEST.value(),
                                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                                "Validation failed for one or more fields",
                                request.getRequestURI(),
                                errors);
                log.warn("Validation error on {}: {}", request.getRequestURI(), errors);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }

        @ExceptionHandler(ConstraintViolationException.class)
        public ResponseEntity<ErrorResponse> handleConstraintViolation(
                        ConstraintViolationException ex,
                        HttpServletRequest request) {
                ErrorResponse response = new ErrorResponse(
                                HttpStatus.BAD_REQUEST.value(),
                                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                                ex.getMessage(),
                                request.getRequestURI());
                log.warn("Constraint violation on {}: {}", request.getRequestURI(), ex.getMessage());
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }

        @ExceptionHandler(org.springframework.http.converter.HttpMessageNotReadableException.class)
        public ResponseEntity<ErrorResponse> handleHttpMessageNotReadable(
                        org.springframework.http.converter.HttpMessageNotReadableException ex,
                        HttpServletRequest request) {
                ErrorResponse response = new ErrorResponse(
                                HttpStatus.BAD_REQUEST.value(),
                                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                                "Malformed request payload: " + ex.getMessage(),
                                request.getRequestURI());
                log.warn("Malformed payload on {}: {}", request.getRequestURI(), ex.getMessage());
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }

        @ExceptionHandler(ResponseStatusException.class)
        public ResponseEntity<ErrorResponse> handleResponseStatusException(
                        ResponseStatusException ex,
                        HttpServletRequest request) {
                HttpStatus status = HttpStatus.resolve(ex.getStatusCode().value());
                String reason = status != null ? status.getReasonPhrase() : "Error";

                ErrorResponse response = new ErrorResponse(
                                ex.getStatusCode().value(),
                                reason,
                                ex.getReason() != null ? ex.getReason() : ex.getMessage(),
                                request.getRequestURI());
                log.warn("Response status exception on {}: {}", request.getRequestURI(), ex.getMessage());
                return ResponseEntity.status(ex.getStatusCode()).body(response);
        }

        @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
        public ResponseEntity<ErrorResponse> handleMethodNotSupported(
                        HttpRequestMethodNotSupportedException ex,
                        HttpServletRequest request) {
                ErrorResponse response = new ErrorResponse(
                                HttpStatus.METHOD_NOT_ALLOWED.value(),
                                HttpStatus.METHOD_NOT_ALLOWED.getReasonPhrase(),
                                ex.getMessage(),
                                request.getRequestURI());
                return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(response);
        }

        @ExceptionHandler(NoResourceFoundException.class)
        public ResponseEntity<ErrorResponse> handleNoResourceFound(
                        NoResourceFoundException ex,
                        HttpServletRequest request) {
                ErrorResponse response = new ErrorResponse(
                                HttpStatus.NOT_FOUND.value(),
                                HttpStatus.NOT_FOUND.getReasonPhrase(),
                                ex.getMessage(),
                                request.getRequestURI());
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }

        @ExceptionHandler(ResourceNotFoundException.class)
        public ResponseEntity<ErrorResponse> handleResourceNotFound(
                        ResourceNotFoundException ex,
                        HttpServletRequest request) {
                ErrorResponse response = new ErrorResponse(
                                HttpStatus.NOT_FOUND.value(),
                                HttpStatus.NOT_FOUND.getReasonPhrase(),
                                ex.getMessage(),
                                request.getRequestURI());
                log.info("Resource not found on {}: {}", request.getRequestURI(), ex.getMessage());
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }

        @ExceptionHandler(Exception.class)
        public ResponseEntity<ErrorResponse> handleGenericException(
                        Exception ex,
                        HttpServletRequest request) {
                log.error("Unhandled exception on {}: {}", request.getRequestURI(), ex.getMessage(), ex);
                ErrorResponse response = new ErrorResponse(
                                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                                HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase(),
                                "An unexpected internal error occurred",
                                request.getRequestURI());
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
}
