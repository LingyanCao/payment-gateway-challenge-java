package com.checkout.payment.gateway.exception;

import org.springframework.http.HttpStatus;

public class EventProcessingException extends RuntimeException{
  
  private final HttpStatus httpStatus;
  
  public EventProcessingException(String message, HttpStatus httpStatus) {
    super(message);
    this.httpStatus = httpStatus;
  }
  
  public HttpStatus getHttpStatus() {
    return httpStatus;
  }
}
