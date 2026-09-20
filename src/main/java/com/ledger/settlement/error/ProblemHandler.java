package com.ledger.settlement.error;

import java.net.URI;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

/**
 * Every failure this service produces comes back as an RFC 9457 ProblemDetail, with the
 * same shape whether the cause is a validation error or an unknown merchant. Spring's
 * ProblemDetail support sets the response content type to application/problem+json
 * automatically - see research.md for the mechanism note - so there is nothing to set by
 * hand here.
 *
 * Every branch fills type, title, status, detail and instance explicitly rather than
 * relying on defaults, because "the same error shape on every failure" was the actual
 * requirement from Meera, not merely "return some ProblemDetail".
 */
@RestControllerAdvice
public class ProblemHandler {

    private static final URI VALIDATION_TYPE = URI.create("https://ledger.example.com/problems/validation-failed");
    private static final URI MERCHANT_NOT_FOUND_TYPE = URI.create("https://ledger.example.com/problems/merchant-not-found");
    private static final URI PAYMENT_NOT_FOUND_TYPE = URI.create("https://ledger.example.com/problems/payment-not-found");

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException exception, WebRequest request) {
        String detail = exception.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining("; "));

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, detail);
        problem.setType(VALIDATION_TYPE);
        problem.setTitle("Validation failed");
        problem.setInstance(instanceUri(request));
        return problem;
    }

    @ExceptionHandler(MerchantNotFoundException.class)
    public ProblemDetail handleMerchantNotFound(MerchantNotFoundException exception, WebRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, exception.getMessage());
        problem.setType(MERCHANT_NOT_FOUND_TYPE);
        problem.setTitle("Merchant not found");
        problem.setInstance(instanceUri(request));
        problem.setProperty("merchantId", exception.getMerchantId());
        return problem;
    }

    @ExceptionHandler(PaymentNotFoundException.class)
    public ProblemDetail handlePaymentNotFound(PaymentNotFoundException exception, WebRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, exception.getMessage());
        problem.setType(PAYMENT_NOT_FOUND_TYPE);
        problem.setTitle("Payment not found");
        problem.setInstance(instanceUri(request));
        problem.setProperty("paymentId", exception.getPaymentId());
        return problem;
    }

    private static URI instanceUri(WebRequest request) {
        String description = request.getDescription(false); // "uri=/payments/settlement"
        return URI.create(description.replaceFirst("^uri=", ""));
    }
}
