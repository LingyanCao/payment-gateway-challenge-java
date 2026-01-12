package com.checkout.payment.gateway.model.validation;

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

    // avoid duplicate field validation
    if (expiryMonth == null || expiryYear == null || expiryMonth < 1 || expiryMonth > 12 || expiryYear < 2025) {
      return true;
    }


    try {
      YearMonth cardExpiry = YearMonth.of(expiryYear, expiryMonth);
      YearMonth currentMonth = YearMonth.now();
      return !cardExpiry.isBefore(currentMonth);
    } catch (Exception e) {
      return false;
    }
  }
}
