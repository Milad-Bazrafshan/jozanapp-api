package ir.orix.school.shared.web;

import java.net.URI;
import java.util.List;

import jakarta.validation.ConstraintViolationException;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import ir.orix.school.shared.config.RequestTraceFilter;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ApiException.class)
    ProblemDetail handleApiException(ApiException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(exception.getStatus(), exception.getMessage());
        problem.setTitle(exception.getStatus().getReasonPhrase());
        problem.setType(URI.create("https://school.orix.ir/problems/" + exception.getCode().toLowerCase().replace('_', '-')));
        problem.setProperty("code", exception.getCode());
        problem.setProperty("traceId", traceId());
        return problem;
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ProblemDetail handleDataIntegrityViolation(DataIntegrityViolationException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.CONFLICT, "The requested change conflicts with existing data.");
        problem.setTitle("Data conflict");
        problem.setType(URI.create("https://school.orix.ir/problems/data-conflict"));
        problem.setProperty("code", "DATA_CONFLICT");
        problem.setProperty("traceId", traceId());
        return problem;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail handleMethodArgumentNotValid(MethodArgumentNotValidException exception) {
        List<ApiFieldError> errors = exception.getBindingResult().getFieldErrors().stream()
                .map(error -> new ApiFieldError(
                        error.getField(),
                        error.getCode() == null ? "INVALID" : error.getCode().toUpperCase(),
                        error.getDefaultMessage() == null ? "Invalid value" : error.getDefaultMessage()))
                .toList();
        return validationProblem("REQUEST_VALIDATION_FAILED", errors);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    ProblemDetail handleConstraintViolation(ConstraintViolationException exception) {
        List<ApiFieldError> errors = exception.getConstraintViolations().stream()
                .map(violation -> new ApiFieldError(
                        violation.getPropertyPath().toString(),
                        "CONSTRAINT_VIOLATION",
                        violation.getMessage()))
                .toList();
        return validationProblem("CONSTRAINT_VALIDATION_FAILED", errors);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ProblemDetail handleUnreadableMessage(HttpMessageNotReadableException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                "The request body is missing or malformed.");
        problem.setTitle("Invalid request body");
        problem.setType(URI.create("https://school.orix.ir/problems/invalid-request-body"));
        problem.setProperty("code", "INVALID_REQUEST_BODY");
        problem.setProperty("traceId", traceId());
        return problem;
    }

    private ProblemDetail validationProblem(String code, List<ApiFieldError> errors) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                "One or more request fields are invalid.");
        problem.setTitle("Request validation failed");
        problem.setType(URI.create("https://school.orix.ir/problems/request-validation"));
        problem.setProperty("code", code);
        problem.setProperty("traceId", traceId());
        problem.setProperty("errors", errors);
        return problem;
    }

    private String traceId() {
        return MDC.get(RequestTraceFilter.TRACE_KEY);
    }
}
