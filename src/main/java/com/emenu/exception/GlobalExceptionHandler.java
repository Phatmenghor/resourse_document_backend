package com.emenu.exception;

import com.emenu.exception.custom.*;
import com.emenu.security.SecurityUtils;
import com.emenu.shared.constants.ErrorCodes;
import com.emenu.shared.dto.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;

import javax.security.auth.login.AccountLockedException;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RestControllerAdvice
@RequiredArgsConstructor
@Slf4j
public class GlobalExceptionHandler {

    private final SecurityUtils securityUtils;

    // ================================
    // AUTHENTICATION & AUTHORIZATION ERRORS
    // ================================

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiResponse<Object>> handleBadCredentialsException(
            BadCredentialsException ex, HttpServletRequest request) {
        log.warn("Authentication failed - Invalid credentials from IP: {}", getClientIP(request));

        Map<String, Object> errorDetails = createErrorDetails(ErrorCodes.INVALID_CREDENTIALS, request);
        errorDetails.put("field", "credentials");
        errorDetails.put("suggestion", "Please verify your email and password");

        ApiResponse<Object> response = new ApiResponse<>("error",
                "Invalid email or password", errorDetails);
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiResponse<Object>> handleAuthenticationException(
            AuthenticationException ex, HttpServletRequest request) {
        log.warn("Authentication failed: {} from IP: {}", ex.getMessage(), getClientIP(request));

        Map<String, Object> errorDetails = createErrorDetails(ErrorCodes.INVALID_CREDENTIALS, request);
        ApiResponse<Object> response = new ApiResponse<>("error", 
            "Authentication failed. Please check your credentials.", errorDetails);
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
    }

    // Account Status Exception Handlers
    @ExceptionHandler(AccountInactiveException.class)
    public ResponseEntity<ApiResponse<Object>> handleAccountInactiveException(
            AccountInactiveException ex, HttpServletRequest request) {
        log.warn("Login blocked - Account inactive: {}", ex.getMessage());

        Map<String, Object> errorDetails = createErrorDetails(ErrorCodes.ACCOUNT_DISABLED, request);
        errorDetails.put("accountStatus", "INACTIVE");
        errorDetails.put("supportContact", "support@emenu-platform.com");
        errorDetails.put("action", "Contact support to reactivate your account");

        ApiResponse<Object> response = new ApiResponse<>("error", ex.getMessage(), errorDetails);
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
    }

    @ExceptionHandler(AccountLockedException.class)
    public ResponseEntity<ApiResponse<Object>> handleAccountLockedException(
            AccountLockedException ex, HttpServletRequest request) {
        log.warn("Login blocked - Account locked: {}", ex.getMessage());

        Map<String, Object> errorDetails = createErrorDetails(ErrorCodes.ACCOUNT_LOCKED, request);
        errorDetails.put("accountStatus", "LOCKED");
        errorDetails.put("supportContact", "support@emenu-platform.com");
        errorDetails.put("securityNote", "Your account has been locked for security reasons");
        errorDetails.put("action", "Contact support to unlock your account");

        ApiResponse<Object> response = new ApiResponse<>("error", ex.getMessage(), errorDetails);
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
    }

    @ExceptionHandler(AccountSuspendedException.class)
    public ResponseEntity<ApiResponse<Object>> handleAccountSuspendedException(
            AccountSuspendedException ex, HttpServletRequest request) {
        log.warn("Login blocked - Account suspended: {}", ex.getMessage());

        Map<String, Object> errorDetails = createErrorDetails(ErrorCodes.ACCOUNT_DISABLED, request);
        errorDetails.put("accountStatus", "SUSPENDED");
        errorDetails.put("supportContact", "support@emenu-platform.com");
        errorDetails.put("appealProcess", "Contact support to appeal this suspension");
        errorDetails.put("action", "Your account access has been suspended");

        ApiResponse<Object> response = new ApiResponse<>("error", ex.getMessage(), errorDetails);
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
    }

    @ExceptionHandler(DisabledException.class)
    public ResponseEntity<ApiResponse<Object>> handleDisabledException(
            DisabledException ex, HttpServletRequest request) {
        log.warn("Login blocked - Account disabled: {}", ex.getMessage());

        Map<String, Object> errorDetails = createErrorDetails(ErrorCodes.ACCOUNT_DISABLED, request);
        errorDetails.put("accountStatus", "DISABLED");
        errorDetails.put("supportContact", "support@emenu-platform.com");

        ApiResponse<Object> response = new ApiResponse<>("error", 
            "Your account has been disabled. Please contact support.", errorDetails);
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
    }

    @ExceptionHandler(LockedException.class)
    public ResponseEntity<ApiResponse<Object>> handleLockedException(
            LockedException ex, HttpServletRequest request) {
        log.warn("Login blocked - Account locked by Spring Security: {}", ex.getMessage());

        Map<String, Object> errorDetails = createErrorDetails(ErrorCodes.ACCOUNT_LOCKED, request);
        errorDetails.put("supportContact", "support@emenu-platform.com");
        
        ApiResponse<Object> response = new ApiResponse<>("error", 
            "Your account has been locked. Please contact support.", errorDetails);
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ApiResponse<Object>> handleUnauthorizedException(
            UnauthorizedException ex, HttpServletRequest request) {
        log.warn("Unauthorized access to {}: {}", request.getRequestURI(), ex.getMessage());

        Map<String, Object> errorDetails = createErrorDetails(ErrorCodes.UNAUTHORIZED, request);
        ApiResponse<Object> response = new ApiResponse<>("error", ex.getMessage(), errorDetails);
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
    }

    @ExceptionHandler(AlreadyExistsException.class)
    public ResponseEntity<ApiResponse<Object>> handleAlreadyExistsException(
            AlreadyExistsException ex, HttpServletRequest request) {
        log.warn("Conflict at {}: {}", request.getRequestURI(), ex.getMessage());

        Map<String, Object> errorDetails = createErrorDetails(ErrorCodes.ALREADY_EXISTS, request);
        ApiResponse<Object> response = new ApiResponse<>("error", ex.getMessage(), errorDetails);
        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiResponse<Object>> handleIllegalStateException(
            IllegalStateException ex, HttpServletRequest request) {
        log.warn("Illegal state at {}: {}", request.getRequestURI(), ex.getMessage());

        Map<String, Object> errorDetails = createErrorDetails(ErrorCodes.CONFLICT, request);
        ApiResponse<Object> response = new ApiResponse<>("error", ex.getMessage(), errorDetails);
        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Object>> handleAccessDeniedException(
            AccessDeniedException ex, HttpServletRequest request) {
        log.warn("Access denied for request to {}: {}", request.getRequestURI(), ex.getMessage());

        Map<String, Object> errorDetails = createErrorDetails(ErrorCodes.INSUFFICIENT_PERMISSIONS, request);
        errorDetails.put("requiredAction", "Ensure you have the necessary permissions");

        ApiResponse<Object> response = new ApiResponse<>("error",
            "Access denied. You don't have permission to perform this action.", errorDetails);
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
    }

    // ================================
    // VALIDATION ERRORS
    // ================================

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Object>> handleValidationExceptions(
            MethodArgumentNotValidException ex, HttpServletRequest request) {

        // ✅ ENHANCED: Detailed field-by-field validation errors
        Map<String, Object> fieldErrors = new HashMap<>();
        List<String> missingFields = new ArrayList<>();
        List<String> invalidFields = new ArrayList<>();

        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            Object rejectedValue = ((FieldError) error).getRejectedValue();

            fieldErrors.put(fieldName, Map.of(
                    "message", errorMessage,
                    "rejectedValue", rejectedValue,
                    "field", fieldName
            ));

            // Categorize errors
            if (errorMessage.toLowerCase().contains("required") ||
                    errorMessage.toLowerCase().contains("must not be null") ||
                    errorMessage.toLowerCase().contains("must not be blank")) {
                missingFields.add(fieldName);
            } else {
                invalidFields.add(fieldName);
            }
        });

        log.warn("Validation failed for request to {}: {} field errors", request.getRequestURI(), fieldErrors.size());

        Map<String, Object> errorDetails = createErrorDetails(ErrorCodes.VALIDATION_ERROR, request);
        errorDetails.put("fieldErrors", fieldErrors);
        errorDetails.put("missingFields", missingFields);
        errorDetails.put("invalidFields", invalidFields);
        errorDetails.put("totalErrors", fieldErrors.size());

        // ✅ ENHANCED: Better error message
        String message = String.format("Validation failed for %d field(s). Required fields: %s",
                fieldErrors.size(),
                missingFields.isEmpty() ? "none" : String.join(", ", missingFields));

        ApiResponse<Object> response = new ApiResponse<>("error", message, errorDetails);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Object>> handleConstraintViolationException(
            ConstraintViolationException ex, HttpServletRequest request) {
        Map<String, String> violations = ex.getConstraintViolations().stream()
                .collect(Collectors.toMap(
                        violation -> violation.getPropertyPath().toString(),
                        ConstraintViolation::getMessage
                ));

        log.warn("Constraint violation for request to {}: {}", request.getRequestURI(), violations);

        Map<String, Object> errorDetails = createErrorDetails(ErrorCodes.VALIDATION_ERROR, request);
        errorDetails.put("violations", violations);

        ApiResponse<Object> response = new ApiResponse<>("error", 
            "Data validation failed. Please check the constraints.", errorDetails);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ApiResponse<Object>> handleRuntimeException(RuntimeException ex, HttpServletRequest request) {
        log.error("Runtime exception in request to {}: {}", request.getRequestURI(), ex.getMessage(), ex);

        String message = ex.getMessage() != null ? ex.getMessage() : "An unexpected error occurred.";
        Map<String, Object> errorDetails = createErrorDetails(ErrorCodes.INTERNAL_SERVER_ERROR, request);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponse<>("error", message, errorDetails));
    }

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ApiResponse<Object>> handleValidationException(
            ValidationException ex, HttpServletRequest request) {
        log.warn("Business validation error: {}", ex.getMessage());

        Map<String, Object> errorDetails = createErrorDetails(ErrorCodes.VALIDATION_ERROR, request);

        // ✅ ENHANCED: Parse validation message for specific field information
        String message = ex.getMessage();

        // Extract specific field information from error message
        if (message.toLowerCase().contains("email")) {
            errorDetails.put("field", "email");
            if (message.contains("already registered") || message.contains("already taken")) {
                errorDetails.put("type", "duplicate");
            } else if (message.contains("format") || message.contains("invalid")) {
                errorDetails.put("type", "format");
            }
        } else if (message.toLowerCase().contains("phone")) {
            errorDetails.put("field", "phoneNumber");
            errorDetails.put("type", "format");
            errorDetails.put("example", "070 411260");
        } else if (message.toLowerCase().contains("subdomain")) {
            errorDetails.put("field", "subdomain");
            if (message.contains("already taken") || message.contains("not available")) {
                errorDetails.put("type", "duplicate");
                errorDetails.put("suggestion", "Please choose a different subdomain name");
            } else if (message.contains("reserved")) {
                errorDetails.put("type", "reserved");
                errorDetails.put("suggestion", "This subdomain is reserved. Please choose a different name");
            } else if (message.contains("format") || message.contains("invalid")) {
                errorDetails.put("type", "format");
                errorDetails.put("suggestion", "Use only lowercase letters, numbers, and hyphens (3-63 characters)");
            }
        } else if (message.toLowerCase().contains("business name")) {
            errorDetails.put("field", "businessName");
            errorDetails.put("type", "duplicate");
            errorDetails.put("suggestion", "Please choose a different business name");
        } else if (message.toLowerCase().contains("user identifier")) {
            errorDetails.put("field", "userIdentifier");
            errorDetails.put("type", "duplicate");
            errorDetails.put("suggestion", "Please choose a different user identifier");
        }

        ApiResponse<Object> response = new ApiResponse<>("error", message, errorDetails);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    // ================================
    // NOT FOUND ERRORS
    // ================================

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ApiResponse<Object>> handleUserNotFoundException(
            UserNotFoundException ex, HttpServletRequest request) {
        log.warn("User not found: {}", ex.getMessage());

        Map<String, Object> errorDetails = createErrorDetails(ErrorCodes.USER_NOT_FOUND, request);
        ApiResponse<Object> response = new ApiResponse<>("error", ex.getMessage(), errorDetails);
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ApiResponse<Object>> handleNotFoundException(
            NotFoundException ex, HttpServletRequest request) {
        log.warn("Resource not found: {}", ex.getMessage());

        Map<String, Object> errorDetails = createErrorDetails(ErrorCodes.USER_NOT_FOUND, request);
        ApiResponse<Object> response = new ApiResponse<>("error", ex.getMessage(), errorDetails);
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<ApiResponse<Object>> handleNoHandlerFoundException(
            NoHandlerFoundException ex, HttpServletRequest request) {
        log.warn("No handler found for {} {}", ex.getHttpMethod(), ex.getRequestURL());

        Map<String, Object> errorDetails = createErrorDetails("ENDPOINT_NOT_FOUND", request);
        errorDetails.put("method", ex.getHttpMethod());
        errorDetails.put("availableEndpoints", "Check API documentation for available endpoints");

        ApiResponse<Object> response = new ApiResponse<>("error", 
            "The requested endpoint was not found.", errorDetails);
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    // ================================
    // HTTP METHOD & REQUEST ERRORS
    // ================================

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiResponse<Object>> handleMethodNotSupportedException(
            HttpRequestMethodNotSupportedException ex, HttpServletRequest request) {
        log.warn("Method not supported: {} for {}", ex.getMethod(), request.getRequestURI());

        Map<String, Object> errorDetails = createErrorDetails("METHOD_NOT_SUPPORTED", request);
        errorDetails.put("supportedMethods", ex.getSupportedHttpMethods());
        errorDetails.put("requestedMethod", ex.getMethod());

        ApiResponse<Object> response = new ApiResponse<>("error",
                String.format("HTTP method '%s' is not supported for this endpoint", ex.getMethod()),
                errorDetails);
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(response);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Object>> handleHttpMessageNotReadableException(
            HttpMessageNotReadableException ex, HttpServletRequest request) {
        log.warn("Invalid JSON request body: {}", ex.getMessage());

        Map<String, Object> errorDetails = createErrorDetails("INVALID_REQUEST_BODY", request);
        errorDetails.put("hint", "Please check your JSON format and data types");

        ApiResponse<Object> response = new ApiResponse<>("error", 
            "Invalid request body. Please check your JSON format.", errorDetails);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiResponse<Object>> handleMissingParameterException(
            MissingServletRequestParameterException ex, HttpServletRequest request) {
        log.warn("Missing required parameter: {}", ex.getParameterName());

        Map<String, Object> errorDetails = createErrorDetails("MISSING_PARAMETER", request);
        errorDetails.put("field", ex.getParameterName());
        errorDetails.put("parameterType", ex.getParameterType());
        errorDetails.put("missingFields", List.of(ex.getParameterName()));

        ApiResponse<Object> response = new ApiResponse<>("error",
                String.format("Required parameter '%s' is missing", ex.getParameterName()),
                errorDetails);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Object>> handleTypeMismatchException(
            MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
        log.warn("Type mismatch for parameter: {}", ex.getName());

        Map<String, Object> errorDetails = createErrorDetails("TYPE_MISMATCH", request);
        errorDetails.put("parameterName", ex.getName());
        errorDetails.put("expectedType", ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "Unknown");
        errorDetails.put("providedValue", ex.getValue());

        ApiResponse<Object> response = new ApiResponse<>("error", 
            String.format("Invalid value for parameter '%s'. Expected %s.", 
                ex.getName(), 
                ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "different type"), 
            errorDetails);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    // ================================
    // DATABASE ERRORS
    // ================================

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Object>> handleDataIntegrityViolation(
            DataIntegrityViolationException ex, HttpServletRequest request) {

        String message = "Data validation failed";
        String errorCode = ErrorCodes.VALIDATION_ERROR;
        Map<String, Object> errorDetails = createErrorDetails(errorCode, request);

        // ✅ ENHANCED: Better constraint violation parsing
        String exceptionMessage = ex.getMessage() != null ? ex.getMessage().toLowerCase() : "";
        String rootCauseMessage = ex.getRootCause() != null ? ex.getRootCause().getMessage().toLowerCase() : "";
        String fullMessage = (exceptionMessage + " " + rootCauseMessage).toLowerCase();

        // Enhanced duplicate detection patterns
        if (containsPattern(fullMessage, new String[]{"email", "unique.*email", "users_email"})) {
            message = "Email address is already registered. Please use a different email or sign in if you already have an account.";
            errorCode = ErrorCodes.EMAIL_ALREADY_EXISTS;
            errorDetails.put("field", "email");
            errorDetails.put("type", "duplicate");
            errorDetails.put("constraint", "UNIQUE_EMAIL");
        } else if (containsPattern(fullMessage, new String[]{"phone", "unique.*phone", "users_phone_number"})) {
            message = "Phone number is already registered. Please use a different phone number.";
            errorCode = ErrorCodes.PHONE_ALREADY_EXISTS;
            errorDetails.put("field", "phoneNumber");
            errorDetails.put("type", "duplicate");
            errorDetails.put("constraint", "UNIQUE_PHONE");
        } else if (containsPattern(fullMessage, new String[]{"business.*email", "businesses_email"})) {
            message = "Business email is already registered. Please use a different email for your business.";
            errorCode = ErrorCodes.EMAIL_ALREADY_EXISTS;
            errorDetails.put("field", "businessEmail");
            errorDetails.put("type", "duplicate");
            errorDetails.put("constraint", "UNIQUE_BUSINESS_EMAIL");
        } else if (containsPattern(fullMessage, new String[]{"subdomain", "unique.*subdomain", "subdomains_subdomain"})) {
            message = "Subdomain is already taken. Please choose a different subdomain name.";
            errorDetails.put("field", "subdomain");
            errorDetails.put("type", "duplicate");
            errorDetails.put("constraint", "UNIQUE_SUBDOMAIN");
            errorDetails.put("suggestion", "Try adding numbers or modify the name (e.g., myrestaurant2, myrestaurant-kh)");
        } else if (containsPattern(fullMessage, new String[]{"business.*name", "businesses_name"})) {
            message = "Business name is already registered. Please choose a different business name.";
            errorDetails.put("field", "businessName");
            errorDetails.put("type", "duplicate");
            errorDetails.put("constraint", "UNIQUE_BUSINESS_NAME");
        } else if (containsPattern(fullMessage, new String[]{"user_identifier", "users_user_identifier"})) {
            message = "User identifier is already taken. Please choose a different identifier.";
            errorDetails.put("field", "userIdentifier");
            errorDetails.put("type", "duplicate");
            errorDetails.put("constraint", "UNIQUE_USER_IDENTIFIER");
        } else if (fullMessage.contains("foreign key")) {
            message = "Referenced data does not exist. Please check your input and try again.";
            errorDetails.put("constraint", "FOREIGN_KEY");
            errorDetails.put("type", "reference");
        } else if (fullMessage.contains("not null")) {
            message = "Required field is missing. Please provide all mandatory information.";
            errorDetails.put("constraint", "NOT_NULL");
            errorDetails.put("type", "required");
        } else {
            message = "Data constraint violation. Please check your input and try again.";
            errorDetails.put("constraint", "UNKNOWN");
            errorDetails.put("type", "validation");
        }

        log.warn("Data integrity violation: {} - Parsed message: {}", fullMessage, message);

        ApiResponse<Object> response = new ApiResponse<>("error", message, errorDetails);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<ApiResponse<Object>> handleDataAccessException(
            DataAccessException ex, HttpServletRequest request) {
        log.error("Database access error in request to {}: {}", request.getRequestURI(), ex.getMessage(), ex);

        Map<String, Object> errorDetails = createErrorDetails(ErrorCodes.DATABASE_ERROR, request);
        ApiResponse<Object> response = new ApiResponse<>("error", 
            "Database operation failed. Please try again.", errorDetails);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    @ExceptionHandler(SQLException.class)
    public ResponseEntity<ApiResponse<Object>> handleSQLException(
            SQLException ex, HttpServletRequest request) {
        log.error("SQL error in request to {}: {}", request.getRequestURI(), ex.getMessage(), ex);

        Map<String, Object> errorDetails = createErrorDetails(ErrorCodes.DATABASE_ERROR, request);
        errorDetails.put("sqlState", ex.getSQLState());
        
        ApiResponse<Object> response = new ApiResponse<>("error", 
            "Database query failed. Please try again.", errorDetails);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    // ================================
    // CUSTOM BUSINESS EXCEPTIONS
    // ================================

    @ExceptionHandler(CustomException.class)
    public ResponseEntity<ApiResponse<Object>> handleCustomException(
            CustomException ex, HttpServletRequest request) {
        log.error("Custom exception occurred: {} - Path: {}", ex.getMessage(), request.getRequestURI());

        Map<String, Object> errorDetails = createErrorDetails(ex.getErrorCode(), request);
        ApiResponse<Object> response = new ApiResponse<>("error", ex.getMessage(), errorDetails);
        return ResponseEntity.status(ex.getHttpStatus()).body(response);
    }

    // ================================
    // GENERIC EXCEPTION HANDLER
    // ================================

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Object>> handleGenericException(
            Exception ex, HttpServletRequest request) {
        String traceId = UUID.randomUUID().toString();
        log.error("Unexpected exception in request to {} [TraceId: {}]: {}", 
            request.getRequestURI(), traceId, ex.getMessage(), ex);

        Map<String, Object> errorDetails = createErrorDetails(ErrorCodes.INTERNAL_SERVER_ERROR, request);
        errorDetails.put("traceId", traceId);
        errorDetails.put("supportMessage", "Please contact support with the trace ID if the problem persists");

        ApiResponse<Object> response = new ApiResponse<>("error", 
            "An unexpected error occurred. Our team has been notified.", errorDetails);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    // ================================
    // HELPER METHODS
    // ================================

    private boolean containsPattern(String text, String[] patterns) {
        for (String pattern : patterns) {
            if (text.contains(pattern)) {
                return true;
            }
        }
        return false;
    }

    private String getSuggestionForConstraint(String errorCode) {
        return switch (errorCode) {
            case ErrorCodes.EMAIL_ALREADY_EXISTS -> "Please use a different email address or sign in if you already have an account";
            case ErrorCodes.PHONE_ALREADY_EXISTS -> "Please use a different phone number or update your existing account";
            default -> "Please check your input and try again";
        };
    }

    private Map<String, Object> createErrorDetails(String errorCode, HttpServletRequest request) {
        Map<String, Object> errorDetails = new HashMap<>();
        errorDetails.put("errorCode", errorCode);
        errorDetails.put("timestamp", LocalDateTime.now());
        errorDetails.put("path", request.getRequestURI());
        errorDetails.put("method", request.getMethod());
        errorDetails.put("traceId", UUID.randomUUID().toString());
        return errorDetails;
    }

    private String getClientIP(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader == null) {
            return request.getRemoteAddr();
        }
        return xfHeader.split(",")[0];
    }

}