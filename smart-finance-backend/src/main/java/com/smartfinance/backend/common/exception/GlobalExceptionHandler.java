package com.smartfinance.backend.common.exception;

import com.smartfinance.backend.ia.exception.AiMessageQuotaExceededException;
import com.smartfinance.backend.ia.exception.AiProviderAuthException;
import com.smartfinance.backend.ia.exception.AiProviderException;
import com.smartfinance.backend.ia.exception.AiProviderModelNotFoundException;
import com.smartfinance.backend.ia.exception.AiProviderNotConfiguredException;
import com.smartfinance.backend.ia.exception.AiProviderRateLimitException;
import com.smartfinance.backend.ia.exception.AiProviderTimeoutException;
import com.smartfinance.backend.ia.exception.AiProviderUnavailableException;
import com.smartfinance.backend.ia.exception.AiProvidersExhaustedException;
import com.smartfinance.backend.ia.service.ai.AiChatOrchestrator;
import com.smartfinance.backend.extractos.exception.EmptyStatementTextException;
import com.smartfinance.backend.extractos.exception.StatementExtractionException;
import com.smartfinance.backend.extractos.exception.StatementPasswordException;
import com.smartfinance.backend.extractos.exception.UnsupportedStatementFileException;
import com.smartfinance.backend.integraciones.exception.InvalidLinkCodeException;
import com.smartfinance.backend.integraciones.exception.TelegramChatNotLinkedException;
import com.smartfinance.backend.integraciones.exception.TelegramImplausibleMovementException;
import com.smartfinance.backend.integraciones.exception.TelegramMessageParseException;
import com.smartfinance.backend.integraciones.exception.TelegramRateLimitExceededException;
import com.smartfinance.backend.servicios.exception.RecurringPaymentAlreadyPaidException;
import com.smartfinance.backend.servicios.exception.RecurringPaymentNotDueYetException;
import com.smartfinance.backend.usuario.exception.EmailAlreadyExistsException;
import com.smartfinance.backend.usuario.exception.InvalidCredentialsException;
import com.smartfinance.backend.usuario.exception.InvalidRefreshTokenException;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.time.Instant;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(EmailAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleEmailAlreadyExists(
            EmailAlreadyExistsException ex,
            HttpServletRequest request
    ) {
        return buildErrorResponse(HttpStatus.CONFLICT, ex.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleInvalidCredentials(
            InvalidCredentialsException ex,
            HttpServletRequest request
    ) {
        return buildErrorResponse(HttpStatus.UNAUTHORIZED, ex.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(InvalidRefreshTokenException.class)
    public ResponseEntity<ErrorResponse> handleInvalidRefreshToken(
            InvalidRefreshTokenException ex,
            HttpServletRequest request
    ) {
        return buildErrorResponse(HttpStatus.UNAUTHORIZED, ex.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(
            MethodArgumentNotValidException ex,
            HttpServletRequest request
    ) {
        String message = ex.getBindingResult().getFieldErrors()
                .stream()
                .findFirst()
                .map(FieldError::getDefaultMessage)
                .orElse("Datos de entrada inválidos");

        return buildErrorResponse(HttpStatus.BAD_REQUEST, message, request.getRequestURI());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleMessageNotReadable(
            HttpMessageNotReadableException ex,
            HttpServletRequest request
    ) {
        return buildErrorResponse(HttpStatus.BAD_REQUEST, "Cuerpo de la petición inválido o mal formado", request.getRequestURI());
    }

    @ExceptionHandler({
            IllegalArgumentException.class,
            ConstraintViolationException.class
    })
    public ResponseEntity<ErrorResponse> handleBadRequest(
            Exception ex,
            HttpServletRequest request
    ) {
        return buildErrorResponse(HttpStatus.BAD_REQUEST, ex.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler({
            ResourceNotFoundException.class,
            EntityNotFoundException.class
    })
    public ResponseEntity<ErrorResponse> handleNotFound(
            RuntimeException ex,
            HttpServletRequest request
    ) {
        return buildErrorResponse(HttpStatus.NOT_FOUND, ex.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(RecurringPaymentAlreadyPaidException.class)
    public ResponseEntity<ErrorResponse> handleRecurringPaymentAlreadyPaid(
            RecurringPaymentAlreadyPaidException ex,
            HttpServletRequest request
    ) {
        return buildErrorResponse(HttpStatus.CONFLICT, ex.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(RecurringPaymentNotDueYetException.class)
    public ResponseEntity<ErrorResponse> handleRecurringPaymentNotDueYet(
            RecurringPaymentNotDueYetException ex,
            HttpServletRequest request
    ) {
        return buildErrorResponse(HttpStatus.CONFLICT, ex.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(
            AccessDeniedException ex,
            HttpServletRequest request
    ) {
        return buildErrorResponse(HttpStatus.FORBIDDEN, ex.getMessage(), request.getRequestURI());
    }

    /**
     * Both exceptions are terminal failures of {@link AiChatOrchestrator#complete}: either no
     * provider is configured at all, or every configured provider failed. Neither ever carries
     * provider-specific detail, so both are mapped to the same 503 response with the exact same
     * generic message, regardless of the exception's own message — this makes it impossible to
     * leak which provider failed or why through this path.
     */
    @ExceptionHandler({
            AiProviderNotConfiguredException.class,
            AiProvidersExhaustedException.class
    })
    public ResponseEntity<ErrorResponse> handleAiUnavailable(
            RuntimeException ex,
            HttpServletRequest request
    ) {
        return buildErrorResponse(HttpStatus.SERVICE_UNAVAILABLE, AiChatOrchestrator.GENERIC_MESSAGE, request.getRequestURI());
    }

    @ExceptionHandler({
            AiProviderAuthException.class,
            AiProviderModelNotFoundException.class
    })
    public ResponseEntity<ErrorResponse> handleAiProviderConfigurationError(
            AiProviderException ex,
            HttpServletRequest request
    ) {
        return buildErrorResponse(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(AiProviderRateLimitException.class)
    public ResponseEntity<ErrorResponse> handleAiProviderRateLimit(
            AiProviderRateLimitException ex,
            HttpServletRequest request
    ) {
        return buildErrorResponse(HttpStatus.TOO_MANY_REQUESTS, ex.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(AiMessageQuotaExceededException.class)
    public ResponseEntity<ErrorResponse> handleAiMessageQuotaExceeded(
            AiMessageQuotaExceededException ex,
            HttpServletRequest request
    ) {
        return buildErrorResponse(HttpStatus.TOO_MANY_REQUESTS, ex.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler({
            AiProviderTimeoutException.class,
            AiProviderUnavailableException.class
    })
    public ResponseEntity<ErrorResponse> handleAiProviderUnavailable(
            AiProviderException ex,
            HttpServletRequest request
    ) {
        return buildErrorResponse(HttpStatus.SERVICE_UNAVAILABLE, ex.getMessage(), request.getRequestURI());
    }

    /**
     * Fallback for any {@link AiProviderException} not covered by a more specific handler above
     * (defensive — every concrete subtype currently thrown by {@code AiChatClient} is already
     * handled explicitly).
     */
    @ExceptionHandler(AiProviderException.class)
    public ResponseEntity<ErrorResponse> handleAiProviderGenericError(
            AiProviderException ex,
            HttpServletRequest request
    ) {
        return buildErrorResponse(HttpStatus.BAD_GATEWAY, ex.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(UnsupportedStatementFileException.class)
    public ResponseEntity<ErrorResponse> handleUnsupportedStatementFile(
            UnsupportedStatementFileException ex,
            HttpServletRequest request
    ) {
        return buildErrorResponse(HttpStatus.BAD_REQUEST, ex.getMessage(), request.getRequestURI());
    }

    /**
     * Las tres son fallas al procesar un extracto bancario ya subido válidamente (Sprint 2,
     * Frente 1): el archivo no tiene texto legible, está protegido con una contraseña incorrecta,
     * o el proveedor de IA no devolvió movimientos interpretables. Se mapean a
     * {@code 422 Unprocessable Entity} porque el request en sí es válido, pero su contenido no se
     * pudo procesar.
     */
    @ExceptionHandler({
            EmptyStatementTextException.class,
            StatementPasswordException.class,
            StatementExtractionException.class
    })
    public ResponseEntity<ErrorResponse> handleStatementProcessingError(
            RuntimeException ex,
            HttpServletRequest request
    ) {
        return buildErrorResponse(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage(), request.getRequestURI());
    }

    /**
     * Las tres excepciones del bot de Telegram (Sprint 2, Frente 2). n8n reenvía el mensaje de
     * cada una tal cual al usuario en el chat, por lo que deben ser textos amigables en español.
     */
    @ExceptionHandler(InvalidLinkCodeException.class)
    public ResponseEntity<ErrorResponse> handleInvalidLinkCode(
            InvalidLinkCodeException ex,
            HttpServletRequest request
    ) {
        return buildErrorResponse(HttpStatus.BAD_REQUEST, ex.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(TelegramChatNotLinkedException.class)
    public ResponseEntity<ErrorResponse> handleTelegramChatNotLinked(
            TelegramChatNotLinkedException ex,
            HttpServletRequest request
    ) {
        return buildErrorResponse(HttpStatus.NOT_FOUND, ex.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(TelegramMessageParseException.class)
    public ResponseEntity<ErrorResponse> handleTelegramMessageParse(
            TelegramMessageParseException ex,
            HttpServletRequest request
    ) {
        return buildErrorResponse(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(TelegramImplausibleMovementException.class)
    public ResponseEntity<ErrorResponse> handleTelegramImplausibleMovement(
            TelegramImplausibleMovementException ex,
            HttpServletRequest request
    ) {
        return buildErrorResponse(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(TelegramRateLimitExceededException.class)
    public ResponseEntity<ErrorResponse> handleTelegramRateLimitExceeded(
            TelegramRateLimitExceededException ex,
            HttpServletRequest request
    ) {
        return buildErrorResponse(HttpStatus.TOO_MANY_REQUESTS, ex.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorResponse> handleMaxUploadSizeExceeded(
            MaxUploadSizeExceededException ex,
            HttpServletRequest request
    ) {
        return buildErrorResponse(
                HttpStatus.PAYLOAD_TOO_LARGE,
                "El archivo supera el tamaño máximo permitido (10 MB)",
                request.getRequestURI()
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(
            Exception ex,
            HttpServletRequest request
    ) {
        // Se loguea con stack trace: un 500 sin causa registrada es indepurable en producción.
        log.error("Error no controlado en {}", request.getRequestURI(), ex);
        return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Error interno del servidor", request.getRequestURI());
    }

    private ResponseEntity<ErrorResponse> buildErrorResponse(
            HttpStatus status,
            String message,
            String path
    ) {
        ErrorResponse response = new ErrorResponse(
                Instant.now(),
                status.value(),
                status.getReasonPhrase(),
                message,
                path
        );

        return ResponseEntity.status(status).body(response);
    }
}
