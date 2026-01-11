package com.checkout.payment.gateway.validation;

import com.checkout.payment.gateway.model.PostPaymentRequest;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.time.YearMonth;

public class FutureExpiryDateValidator implements ConstraintValidator<FutureExpiryDate, PostPaymentRequest> {

  @Override
  public void initialize(FutureExpiryDate constraintAnnotation) {
  }

  @Override
  public boolean isValid(PostPaymentRequest request, ConstraintValidatorContext context) {
    if (request == null) {
      return true;
    }

    Integer expiryMonth = request.getExpiryMonth();
    Integer expiryYear = request.getExpiryYear();

    // If either field is null, let @NotNull handle it
    if (expiryMonth == null || expiryYear == null) {
      return true;
    }

    // Validate month range (1-12) before creating YearMonth
    if (expiryMonth < 1 || expiryMonth > 12) {
      return true; // Let @Min/@Max handle month validation
    }

    // Validate year range
    if (expiryYear < 2026) {
      return true;
    }

    try {
      YearMonth cardExpiry = YearMonth.of(expiryYear, expiryMonth);
      YearMonth currentMonth = YearMonth.now();
      return !cardExpiry.isBefore(currentMonth);
    } catch (Exception e) {
      // If YearMonth creation fails for any reason, validation fails
      return false;
    }
  }
}
