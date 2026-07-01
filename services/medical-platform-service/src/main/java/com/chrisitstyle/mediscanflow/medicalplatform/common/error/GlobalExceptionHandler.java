package com.chrisitstyle.mediscanflow.medicalplatform.common.error;

import com.chrisitstyle.mediscanflow.medicalplatform.common.validation.InvalidFileUploadException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    ResponseEntity<ApiErrorResponseDTO> handleResourceNotFound(
            ResourceNotFoundException exception,
            HttpServletRequest request
    ) {
        HttpStatus status = HttpStatus.NOT_FOUND;

        return ResponseEntity.status(status)
                .body(ApiErrorResponseDTO.of(
                        status.value(),
                        status.getReasonPhrase(),
                        exception.getMessage(),
                        request.getRequestURI()
                ));
    }

    @ExceptionHandler(DuplicateResourceException.class)
    ResponseEntity<ApiErrorResponseDTO> handleDuplicateResource(
            DuplicateResourceException exception,
            HttpServletRequest request
    ) {
        HttpStatus status = HttpStatus.CONFLICT;

        return ResponseEntity.status(status)
                .body(ApiErrorResponseDTO.of(
                        status.value(),
                        status.getReasonPhrase(),
                        exception.getMessage(),
                        request.getRequestURI()
                ));
    }

    @ExceptionHandler(InvalidAnalysisStateException.class)
    ResponseEntity<ApiErrorResponseDTO> handleInvalidAnalysisState(
            InvalidAnalysisStateException exception,
            HttpServletRequest request
    ) {
        HttpStatus status = HttpStatus.CONFLICT;

        return ResponseEntity.status(status)
                .body(ApiErrorResponseDTO.of(
                        status.value(),
                        status.getReasonPhrase(),
                        exception.getMessage(),
                        request.getRequestURI()
                ));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiErrorResponseDTO> handleMethodArgumentNotValid(
            MethodArgumentNotValidException exception,
            HttpServletRequest request
    ) {
        HttpStatus status = HttpStatus.BAD_REQUEST;
        Map<String, String> validationErrors = new LinkedHashMap<>();

        exception.getBindingResult().getFieldErrors()
                .forEach(fieldError ->
                        validationErrors.put(
                                fieldError.getField(),
                                fieldError.getDefaultMessage()
                        )
                );

        return ResponseEntity.status(status)
                .body(ApiErrorResponseDTO.withValidationErrors(
                        status.value(),
                        status.getReasonPhrase(),
                        "Validation failed",
                        request.getRequestURI(),
                        validationErrors
                ));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    ResponseEntity<ApiErrorResponseDTO> handleConstraintViolation(
            ConstraintViolationException exception,
            HttpServletRequest request
    ) {
        HttpStatus status = HttpStatus.BAD_REQUEST;
        Map<String, String> validationErrors = new LinkedHashMap<>();

        exception.getConstraintViolations()
                .forEach(violation ->
                        validationErrors.put(
                                violation.getPropertyPath().toString(),
                                violation.getMessage()
                        )
                );

        return ResponseEntity.status(status)
                .body(ApiErrorResponseDTO.withValidationErrors(
                        status.value(),
                        status.getReasonPhrase(),
                        "Validation failed",
                        request.getRequestURI(),
                        validationErrors
                ));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ApiErrorResponseDTO> handleHttpMessageNotReadable(
            HttpMessageNotReadableException exception,
            HttpServletRequest request
    ) {
        HttpStatus status = HttpStatus.BAD_REQUEST;

        return ResponseEntity.status(status)
                .body(ApiErrorResponseDTO.of(
                        status.value(),
                        status.getReasonPhrase(),
                        "Malformed request body",
                        request.getRequestURI()
                ));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    ResponseEntity<ApiErrorResponseDTO> handleMaxUploadSizeExceeded(
            MaxUploadSizeExceededException exception,
            HttpServletRequest request
    ) {
        HttpStatus status = HttpStatus.PAYLOAD_TOO_LARGE;

        return ResponseEntity.status(status)
                .body(ApiErrorResponseDTO.of(
                        status.value(),
                        status.getReasonPhrase(),
                        "Uploaded file is too large",
                        request.getRequestURI()
                ));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<ApiErrorResponseDTO> handleIllegalArgument(
            IllegalArgumentException exception,
            HttpServletRequest request
    ) {
        HttpStatus status = HttpStatus.BAD_REQUEST;

        return ResponseEntity.status(status)
                .body(ApiErrorResponseDTO.of(
                        status.value(),
                        status.getReasonPhrase(),
                        exception.getMessage(),
                        request.getRequestURI()
                ));
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiErrorResponseDTO> handleGenericException(
            Exception exception,
            HttpServletRequest request
    ) {
        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;

        return ResponseEntity.status(status)
                .body(ApiErrorResponseDTO.of(
                        status.value(),
                        status.getReasonPhrase(),
                        "Unexpected internal server error",
                        request.getRequestURI()
                ));
    }

    @ExceptionHandler(InvalidFileUploadException.class)
    ResponseEntity<ApiErrorResponseDTO> handleInvalidFileUpload(
            InvalidFileUploadException exception,
            HttpServletRequest request
    ) {
        HttpStatus status = HttpStatus.BAD_REQUEST;

        return ResponseEntity.status(status)
                .body(ApiErrorResponseDTO.of(
                        status.value(),
                        status.getReasonPhrase(),
                        exception.getMessage(),
                        request.getRequestURI()
                ));
    }

    @ExceptionHandler(MissingServletRequestPartException.class)
    ResponseEntity<ApiErrorResponseDTO> handleMissingServletRequestPart(
            MissingServletRequestPartException exception,
            HttpServletRequest request
    ) {
        HttpStatus status = HttpStatus.BAD_REQUEST;

        return ResponseEntity.status(status)
                .body(ApiErrorResponseDTO.of(
                        status.value(),
                        status.getReasonPhrase(),
                        "Required request part is missing: " + exception.getRequestPartName(),
                        request.getRequestURI()
                ));
    }
}
