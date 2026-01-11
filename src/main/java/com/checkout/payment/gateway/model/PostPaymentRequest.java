package com.checkout.payment.gateway.model;

import com.checkout.payment.gateway.validation.FutureExpiryDate;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import java.io.Serializable;

@FutureExpiryDate
public class PostPaymentRequest implements Serializable {

  @NotNull(message = "Card number is required")
  @Pattern(regexp = "^[0-9]{14,19}$", message = "Card number must be between 14-19 digits")
  @JsonProperty("card_number")
  private String cardNumber;

  @NotNull(message = "Expiry month is required")
  @Min(value = 1, message = "Expiry month must be between 1 and 12")
  @Max(value = 12, message = "Expiry month must be between 1 and 12")
  @JsonProperty("expiry_month")
  private Integer expiryMonth;

  @NotNull(message = "Expiry year is required")
  @Min(value = 2026, message = "Expiry year must be 2026 or later")
  @JsonProperty("expiry_year")
  private Integer expiryYear;

  @NotNull(message = "Currency is required")
  @Pattern(
      regexp = "^(GBP|USD|CNY)$", 
      message = "Currency must be one of: GBP, USD, CNY"
  )
  private String currency;

  @NotNull(message = "Amount is required")
  @Positive(message = "Amount must be greater than 0")
  private Integer amount;

  @NotNull(message = "CVV is required")
  @Min(value = 100, message = "CVV must be between 3-4 digits")
  @Max(value = 9999, message = "CVV must be between 3-4 digits")
  private Integer cvv;

  public String getCardNumber() {
    return cardNumber;
  }

  public void setCardNumber(String cardNumber) {
    this.cardNumber = cardNumber;
  }

  @JsonIgnore
  public String getCardNumberLastFour() {
    if (cardNumber != null && cardNumber.length() >= 4) {
      return cardNumber.substring(cardNumber.length() - 4);
    }
    
    return "";
  }

  public int getExpiryMonth() {
    return expiryMonth;
  }

  public void setExpiryMonth(int expiryMonth) {
    this.expiryMonth = expiryMonth;
  }

  public int getExpiryYear() {
    return expiryYear;
  }

  public void setExpiryYear(int expiryYear) {
    this.expiryYear = expiryYear;
  }

  public String getCurrency() {
    return currency;
  }

  public void setCurrency(String currency) {
    this.currency = currency;
  }

  public int getAmount() {
    return amount;
  }

  public void setAmount(int amount) {
    this.amount = amount;
  }

  public int getCvv() {
    return cvv;
  }

  public void setCvv(int cvv) {
    this.cvv = cvv;
  }

  @JsonIgnore
  public String getExpiryDate() {
    return String.format("%d/%d", expiryMonth, expiryYear);
  }

  @Override
  public String toString() {
    return "PostPaymentRequest{" +
        "cardNumber='" + (cardNumber != null ? "****" + getCardNumberLastFour() : "null") + '\'' +
        ", expiryMonth=" + expiryMonth +
        ", expiryYear=" + expiryYear +
        ", currency='" + currency + '\'' +
        ", amount=" + amount +
        ", cvv=" + cvv +
        '}';
  }
}
