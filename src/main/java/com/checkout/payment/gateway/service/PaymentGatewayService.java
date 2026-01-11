package com.checkout.payment.gateway.service;

import com.checkout.payment.gateway.client.BankClient;
import com.checkout.payment.gateway.client.model.BankRequest;
import com.checkout.payment.gateway.client.model.BankResponse;
import com.checkout.payment.gateway.enums.PaymentStatus;
import com.checkout.payment.gateway.exception.EventProcessingException;
import com.checkout.payment.gateway.model.PostPaymentRequest;
import com.checkout.payment.gateway.model.PostPaymentResponse;
import com.checkout.payment.gateway.repository.PaymentsRepository;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpServerErrorException;

@Service
public class PaymentGatewayService {

  private static final Logger LOG = LoggerFactory.getLogger(PaymentGatewayService.class);

  private final PaymentsRepository paymentsRepository;
  private final BankClient bankClient;

  public PaymentGatewayService(PaymentsRepository paymentsRepository, BankClient bankClient) {
    this.paymentsRepository = paymentsRepository;
    this.bankClient = bankClient;
  }

  public PostPaymentResponse getPaymentById(UUID id) {
    LOG.debug("Requesting access to payment with ID {}", id);
    return paymentsRepository.get(id).orElseThrow(() -> 
        new EventProcessingException("Payment not found. Please check the payment ID and try again.", HttpStatus.NOT_FOUND));
  }

  public PostPaymentResponse processPayment(PostPaymentRequest paymentRequest) {
    LOG.info("Processing payment for card ending in {}", 
        paymentRequest.getCardNumber().substring(paymentRequest.getCardNumber().length() - 4));
    
    UUID paymentId = UUID.randomUUID();
    PostPaymentResponse response = new PostPaymentResponse();
    response.setId(paymentId);
    
    try {
      BankRequest bankRequest = new BankRequest(
          paymentRequest.getCardNumber(),
          paymentRequest.getExpiryDate(),
          paymentRequest.getCurrency(),
          paymentRequest.getAmount(),
          paymentRequest.getCvv()
      );
      
      BankResponse bankResponse = bankClient.processPayment(bankRequest);
      
      if (bankResponse.isAuthorized()) {
        response.setStatus(PaymentStatus.AUTHORIZED);
        LOG.info("Payment {} authorized with code: {}", paymentId, bankResponse.getAuthorizationCode());
      } else {
        response.setStatus(PaymentStatus.DECLINED);
        LOG.info("Payment {} declined", paymentId);
      }
      
    } catch (HttpServerErrorException ex) {
      // Bank service returned 5xx error - this is a technical failure, not a business decline.
      // Do not save payment record as we don't know the actual status.
      // Client should retry the request.
      LOG.error("Bank service unavailable for payment {}: HTTP {} - {}. Transaction not completed.", 
          paymentId, ex.getStatusCode(), ex.getStatusText(), ex);
      throw new EventProcessingException(
          "Payment service temporarily unavailable. Please try again later.",
          HttpStatus.SERVICE_UNAVAILABLE
      );
    } catch (Exception ex) {
      // Unexpected errors (network issues, timeouts, etc.) - also technical failures
      // Do not save payment record as we don't know the actual status.
      LOG.error("Unexpected error processing payment {}: {}. Transaction not completed.", 
          paymentId, ex.getMessage(), ex);
      throw new EventProcessingException(
          "Unable to process payment at this time. Please try again.",
          HttpStatus.SERVICE_UNAVAILABLE
      );
    }
    
    // Set card details in response (masked)
    response.setCardNumberLastFour(Integer.parseInt(paymentRequest.getCardNumberLastFour()));
    response.setExpiryMonth(paymentRequest.getExpiryMonth());
    response.setExpiryYear(paymentRequest.getExpiryYear());
    response.setCurrency(paymentRequest.getCurrency());
    response.setAmount(paymentRequest.getAmount());
    
    // Store payment
    paymentsRepository.add(response);
    
    return response;
  }
}
