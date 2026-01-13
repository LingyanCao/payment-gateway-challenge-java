package com.checkout.payment.gateway.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.checkout.payment.gateway.client.BankClient;
import com.checkout.payment.gateway.client.model.BankRequest;
import com.checkout.payment.gateway.client.model.BankResponse;
import com.checkout.payment.gateway.enums.PaymentStatus;
import com.checkout.payment.gateway.exception.EventProcessingException;
import com.checkout.payment.gateway.model.PostPaymentRequest;
import com.checkout.payment.gateway.model.PostPaymentResponse;
import com.checkout.payment.gateway.repository.PaymentsRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;

/**
 * Unit tests for PaymentGatewayService.
 * Uses Mockito to mock dependencies (BankClient and PaymentsRepository).
 */
@ExtendWith(MockitoExtension.class)
class PaymentGatewayServiceTest {

  @Mock
  private PaymentsRepository paymentsRepository;

  @Mock
  private BankClient bankClient;

  @InjectMocks
  private PaymentGatewayService paymentGatewayService;

  private PostPaymentRequest validRequest;
  private BankResponse authorizedBankResponse;
  private BankResponse declinedBankResponse;

  @BeforeEach
  void setUp() {
    // Setup valid payment request
    validRequest = new PostPaymentRequest();
    validRequest.setCardNumber("2222405343248877");
    validRequest.setExpiryMonth(12);
    validRequest.setExpiryYear(2026);
    validRequest.setCurrency("GBP");
    validRequest.setAmount(100);
    validRequest.setCvv(123);

    // Setup authorized bank response
    authorizedBankResponse = new BankResponse();
    authorizedBankResponse.setAuthorized(true);
    authorizedBankResponse.setAuthorizationCode("AUTH123");

    // Setup declined bank response
    declinedBankResponse = new BankResponse();
    declinedBankResponse.setAuthorized(false);
    declinedBankResponse.setAuthorizationCode(null);
  }

  // 1. getPaymentById Tests
  @Test
  void whenPaymentExistsThenReturnPayment() {
    UUID paymentId = UUID.randomUUID();
    PostPaymentResponse expectedPayment = new PostPaymentResponse();
    expectedPayment.setId(paymentId);
    expectedPayment.setStatus(PaymentStatus.AUTHORIZED);
    when(paymentsRepository.get(paymentId)).thenReturn(Optional.of(expectedPayment));

    PostPaymentResponse actualPayment = paymentGatewayService.getPaymentById(paymentId);

    assertNotNull(actualPayment);
    assertEquals(paymentId, actualPayment.getId());
    assertEquals(PaymentStatus.AUTHORIZED, actualPayment.getStatus());
    verify(paymentsRepository, times(1)).get(paymentId);
  }

  @Test
  void whenPaymentDoesNotExistThenThrowException() {
    UUID paymentId = UUID.randomUUID();
    when(paymentsRepository.get(paymentId)).thenReturn(Optional.empty());

    EventProcessingException exception = assertThrows(
        EventProcessingException.class,
        () -> paymentGatewayService.getPaymentById(paymentId)
    );

    assertEquals("Payment not found. Please check the payment ID and try again.", exception.getMessage());
    verify(paymentsRepository, times(1)).get(paymentId);
  }

  // 2. processPayment - Authorized Tests

  @Test
  void whenBankAuthorizesPaymentThenReturnAuthorizedStatus() {
    when(bankClient.processPayment(any(BankRequest.class))).thenReturn(authorizedBankResponse);
    PostPaymentResponse response = paymentGatewayService.processPayment(validRequest);

    assertNotNull(response);
    assertNotNull(response.getId());
    assertEquals(PaymentStatus.AUTHORIZED, response.getStatus());
    assertEquals(8877, response.getCardNumberLastFour());
    assertEquals(12, response.getExpiryMonth());
    assertEquals(2026, response.getExpiryYear());
    assertEquals("GBP", response.getCurrency());
    assertEquals(100, response.getAmount());
  }

  // 3. processPayment - Declined Tests

  @Test
  void whenBankDeclinesPaymentThenReturnDeclinedStatus() {
    when(bankClient.processPayment(any(BankRequest.class))).thenReturn(declinedBankResponse);

    PostPaymentResponse response = paymentGatewayService.processPayment(validRequest);

    assertNotNull(response);
    assertNotNull(response.getId());
    assertEquals(PaymentStatus.DECLINED, response.getStatus());
    assertEquals(8877, response.getCardNumberLastFour());

    verify(bankClient, times(1)).processPayment(any(BankRequest.class));
    verify(paymentsRepository, times(1)).add(response);
  }

  // 4. processPayment - Error Handling Tests

  @Test
  void whenBankReturns5xxErrorThenThrowServiceUnavailableException() {
    HttpServerErrorException serverError = mock(HttpServerErrorException.class);
    when(serverError.getMessage()).thenReturn("503 Service Unavailable");
    when(bankClient.processPayment(any(BankRequest.class))).thenThrow(serverError);

    EventProcessingException exception = assertThrows(
        EventProcessingException.class,
        () -> paymentGatewayService.processPayment(validRequest)
    );

    assertEquals("Bank service temporarily unavailable. Please try again later or contact support team.", exception.getMessage());
    assertEquals(org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE, exception.getHttpStatus());
    verify(bankClient, times(1)).processPayment(any(BankRequest.class));
    verify(paymentsRepository, never()).add(any());
  }

  @Test
  void whenBankReturns4xxErrorThenThrowBadRequestException() {
    // Simulate bank returning 400 Bad Request
    HttpClientErrorException clientError = HttpClientErrorException.create(
        HttpStatus.BAD_REQUEST,
        "Bad Request",
        null,
        "Invalid request format".getBytes(),
        null
    );
    when(bankClient.processPayment(any(BankRequest.class))).thenThrow(clientError);

    EventProcessingException exception = assertThrows(
        EventProcessingException.class,
        () -> paymentGatewayService.processPayment(validRequest)
    );

    assertEquals("Payment request validation failed. Please contact support.", exception.getMessage());
    assertEquals(org.springframework.http.HttpStatus.BAD_REQUEST, exception.getHttpStatus());
    verify(bankClient, times(1)).processPayment(any(BankRequest.class));
    verify(paymentsRepository, never()).add(any());
  }

  @Test
  void whenBankConnectionFailsThenThrowServiceUnavailableException() {
    ResourceAccessException connectionError = new ResourceAccessException("Connection refused");
    when(bankClient.processPayment(any(BankRequest.class))).thenThrow(connectionError);

    EventProcessingException exception = assertThrows(
        EventProcessingException.class,
        () -> paymentGatewayService.processPayment(validRequest)
    );

    assertEquals("Internal server error. Please try again or contact support team.", exception.getMessage());
    assertEquals(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR, exception.getHttpStatus());
    verify(bankClient, times(1)).processPayment(any(BankRequest.class));
    verify(paymentsRepository, never()).add(any());
  }

  @Test
  void whenUnexpectedExceptionOccursThenThrowServiceUnavailableException() {
    // Given
    RuntimeException unexpectedException = new RuntimeException("Unexpected error");
    when(bankClient.processPayment(any(BankRequest.class))).thenThrow(unexpectedException);

    // When & Then
    EventProcessingException exception = assertThrows(
        EventProcessingException.class,
        () -> paymentGatewayService.processPayment(validRequest)
    );

    assertEquals("Internal server error. Please try again or contact support team.", exception.getMessage());
    assertEquals(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR, exception.getHttpStatus());
    verify(bankClient, times(1)).processPayment(any(BankRequest.class));
    verify(paymentsRepository, never()).add(any());
  }

  // 5. processPayment - Repository Storage Tests

  @Test
  void whenPaymentIsProcessedThenItIsStoredInRepository() {
    ArgumentCaptor<PostPaymentResponse> responseCaptor = ArgumentCaptor.forClass(PostPaymentResponse.class);
    when(bankClient.processPayment(any(BankRequest.class))).thenReturn(authorizedBankResponse);
    PostPaymentResponse response = paymentGatewayService.processPayment(validRequest);

    verify(paymentsRepository).add(responseCaptor.capture());
    PostPaymentResponse storedPayment = responseCaptor.getValue();

    assertEquals(response.getId(), storedPayment.getId());
    assertEquals(response.getStatus(), storedPayment.getStatus());
    assertEquals(response.getCardNumberLastFour(), storedPayment.getCardNumberLastFour());
    assertEquals(response.getExpiryMonth(), storedPayment.getExpiryMonth());
    assertEquals(response.getExpiryYear(), storedPayment.getExpiryYear());
    assertEquals(response.getCurrency(), storedPayment.getCurrency());
    assertEquals(response.getAmount(), storedPayment.getAmount());
  }

  @Test
  void whenPaymentIsDeclinedThenItIsStillStoredInRepository() {
    when(bankClient.processPayment(any(BankRequest.class))).thenReturn(declinedBankResponse);

    PostPaymentResponse response = paymentGatewayService.processPayment(validRequest);

    verify(paymentsRepository, times(1)).add(response);
    assertEquals(PaymentStatus.DECLINED, response.getStatus());
  }

  @Test
  void whenBankErrorOccursThenPaymentIsNotStored() {
    HttpServerErrorException serverError = mock(HttpServerErrorException.class);
    when(bankClient.processPayment(any(BankRequest.class))).thenThrow(serverError);

    assertThrows(
        EventProcessingException.class,
        () -> paymentGatewayService.processPayment(validRequest)
    );

    verify(paymentsRepository, never()).add(any());
  }
}
