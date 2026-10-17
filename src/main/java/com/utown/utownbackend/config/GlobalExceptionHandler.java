package com.utown.utownbackend.config;

import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import org.springframework.security.access.AccessDeniedException;

import java.util.stream.Collectors;
import org.slf4j.MDC;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private void enrichProblemDetail(ProblemDetail problemDetail) {
        String requestId = MDC.get(RequestCorrelationFilter.MDC_REQUEST_ID_KEY);
        if (requestId != null) {
            problemDetail.setProperty("requestId", requestId);
        }
    }

    @Override
    protected ProblemDetail createProblemDetail(Exception ex, HttpStatusCode status, String defaultDetail,
                                                String detailMessageCode, Object[] detailMessageArguments,
                                                WebRequest request) {
        ProblemDetail problemDetail = super.createProblemDetail(ex, status, defaultDetail, detailMessageCode, detailMessageArguments, request);
        enrichProblemDetail(problemDetail);
        return problemDetail;
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleEntityNotFound(EntityNotFoundException ex) {
        log.warn("Entity not found: {}", ex.getMessage());

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.NOT_FOUND, ex.getMessage());
        problemDetail.setTitle("Resource Not Found");
        enrichProblemDetail(problemDetail);

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(problemDetail);
    }

    @ExceptionHandler({
        com.utown.utownbackend.exception.ResourceConflictException.class,
        com.utown.utownbackend.exception.InvalidOrderStatusTransitionException.class
    })
    public ResponseEntity<ProblemDetail> handleResourceConflict(RuntimeException ex) {
        log.warn("Resource conflict: {}", ex.getMessage());

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.CONFLICT, ex.getMessage());
        problemDetail.setTitle("Resource Conflict");
        enrichProblemDetail(problemDetail);

        return ResponseEntity.status(HttpStatus.CONFLICT).body(problemDetail);
    }

    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
    public ResponseEntity<ProblemDetail> handleBadRequest(RuntimeException ex) {
        log.warn("Bad request: {}", ex.getMessage());

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST, ex.getMessage());
        problemDetail.setTitle("Bad Request");
        enrichProblemDetail(problemDetail);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problemDetail);
    }

    @ExceptionHandler(com.utown.utownbackend.exception.PaymentFailedException.class)
    public ResponseEntity<ProblemDetail> handlePaymentFailed(com.utown.utownbackend.exception.PaymentFailedException ex) {
        log.warn("Payment failed: {}", ex.getMessage());

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST, ex.getMessage());
        problemDetail.setTitle("Payment Failed");

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problemDetail);
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {

        String errors = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining(", "));

        log.warn("Validation failed: {}", errors);

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST, errors);
        problemDetail.setTitle("Validation Failed");
        enrichProblemDetail(problemDetail);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problemDetail);
    }


    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleUnexpected(Exception ex) {
        log.error("Unexpected error occurred", ex);

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected error occurred. Please try again later.");
        problemDetail.setTitle("Internal Server Error");
        enrichProblemDetail(problemDetail);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(problemDetail);
    }

    @ExceptionHandler(org.springframework.security.core.AuthenticationException.class)
    public ResponseEntity<ProblemDetail> handleAuthenticationException(org.springframework.security.core.AuthenticationException ex) {
        log.warn("Authentication failed: {}", ex.getMessage());

        String detail = (ex instanceof com.utown.utownbackend.exception.InvalidTokenException && ex.getMessage() != null)
                ? ex.getMessage()
                : "Invalid username or password, or unauthorized access.";

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.UNAUTHORIZED, detail);
        problemDetail.setTitle("Unauthorized");
        enrichProblemDetail(problemDetail);

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(problemDetail);
    }

    @ExceptionHandler(org.springframework.dao.DataIntegrityViolationException.class)
    public ResponseEntity<ProblemDetail> handleDataIntegrityViolation(org.springframework.dao.DataIntegrityViolationException ex) {
        log.warn("Data integrity violation: {}", ex.getMessage());

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.CONFLICT, "A resource conflict occurred. This might be a duplicate entry.");
        problemDetail.setTitle("Data Integrity Violation");
        enrichProblemDetail(problemDetail);

        return ResponseEntity.status(HttpStatus.CONFLICT).body(problemDetail);
    }

    @ExceptionHandler(org.springframework.security.authentication.AccountStatusException.class)
    public ResponseEntity<ProblemDetail> handleAccountStatusException(org.springframework.security.authentication.AccountStatusException ex) {
        log.warn("Account status invalid: {}", ex.getMessage());

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.FORBIDDEN, "Your account is suspended or inactive.");
        problemDetail.setTitle("Account Disabled");
        enrichProblemDetail(problemDetail);

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(problemDetail);
    }

    @ExceptionHandler(org.springframework.orm.ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<ProblemDetail> handleOptimisticLockingFailure(org.springframework.orm.ObjectOptimisticLockingFailureException ex) {
        log.warn("Optimistic locking failure: {}", ex.getMessage());

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.CONFLICT, "Concurrent modification detected. Please try again.");
        problemDetail.setTitle("Concurrent Modification");
        enrichProblemDetail(problemDetail);

        return ResponseEntity.status(HttpStatus.CONFLICT).body(problemDetail);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ProblemDetail> handleAccessDenied(AccessDeniedException ex) {
        log.warn("Access denied: {}", ex.getMessage());

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null
                || !auth.isAuthenticated()
                || auth instanceof AnonymousAuthenticationToken) {

            ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                    HttpStatus.UNAUTHORIZED,
                    "Authentication is required to access this resource."
            );
            problemDetail.setTitle("Unauthorized");
            enrichProblemDetail(problemDetail);

            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(problemDetail);
        }

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.FORBIDDEN,
                "You do not have permission to access this resource."
        );
        problemDetail.setTitle("Forbidden");
        enrichProblemDetail(problemDetail);

        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(problemDetail);
    }

    @ExceptionHandler(com.utown.utownbackend.exception.RateLimitExceededException.class)
    public ResponseEntity<ProblemDetail> handleRateLimitExceeded(com.utown.utownbackend.exception.RateLimitExceededException ex) {
        log.warn("Rate limit exceeded: {}", ex.getMessage());

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.TOO_MANY_REQUESTS, ex.getMessage());
        problemDetail.setTitle("Too Many Requests");

        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(problemDetail);
    }
}
