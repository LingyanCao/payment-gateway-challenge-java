package com.checkout.payment.gateway.client.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class BankRequest {
  
  @JsonProperty("card_number")
  private String cardNumber;
  
  @JsonProperty("expiry_date")
  private String expiryDate;
  
  private String currency;
  private int amount;
  private int cvv;

  public BankRequest() {
  }

  public BankRequest(String cardNumber, String expiryDate, String currency, int amount, int cvv) {
    this.cardNumber = cardNumber;
    this.expiryDate = expiryDate;
    this.currency = currency;
    this.amount = amount;
    this.cvv = cvv;
  }

  public String getCardNumber() {
    return cardNumber;
  }

  public void setCardNumber(String cardNumber) {
    this.cardNumber = cardNumber;
  }

  public String getExpiryDate() {
    return expiryDate;
  }

  public void setExpiryDate(String expiryDate) {
    this.expiryDate = expiryDate;
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

  @Override
  public String toString() {
    return "BankRequest{" +
        "cardNumber='****" + (cardNumber != null && cardNumber.length() >= 4 
            ? cardNumber.substring(cardNumber.length() - 4) : "****") + '\'' +
        ", expiryDate='" + expiryDate + '\'' +
        ", currency='" + currency + '\'' +
        ", amount=" + amount +
        ", cvv=***" +
        '}';
  }
}
