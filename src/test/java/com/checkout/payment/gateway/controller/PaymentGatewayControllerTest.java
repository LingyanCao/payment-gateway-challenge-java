package com.checkout.payment.gateway.controller;


import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.checkout.payment.gateway.enums.PaymentStatus;
import com.checkout.payment.gateway.model.PostPaymentResponse;
import com.checkout.payment.gateway.repository.PaymentsRepository;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

@SpringBootTest
@AutoConfigureMockMvc
class PaymentGatewayControllerTest {

  @Autowired
  private MockMvc mvc;
  @Autowired
  PaymentsRepository paymentsRepository;

  // 1. Test for GET /payment/{id} endpoint - successful retrieval
  @Test
  void whenPaymentWithIdExistThenCorrectPaymentIsReturned() throws Exception {
    PostPaymentResponse payment = new PostPaymentResponse();
    payment.setId(UUID.randomUUID());
    payment.setAmount(10);
    payment.setCurrency("USD");
    payment.setStatus(PaymentStatus.AUTHORIZED);
    payment.setExpiryMonth(12);
    payment.setExpiryYear(2024);
    payment.setCardNumberLastFour(4321);

    paymentsRepository.add(payment);

    mvc.perform(MockMvcRequestBuilders.get("/payments/" + payment.getId()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value(payment.getStatus().getName()))
        .andExpect(jsonPath("$.cardNumberLastFour").value(payment.getCardNumberLastFour()))
        .andExpect(jsonPath("$.expiryMonth").value(payment.getExpiryMonth()))
        .andExpect(jsonPath("$.expiryYear").value(payment.getExpiryYear()))
        .andExpect(jsonPath("$.currency").value(payment.getCurrency()))
        .andExpect(jsonPath("$.amount").value(payment.getAmount()));
  }

  // 2. Test for GET /payments/{id} endpoint - payment not found
  @Test
  void whenPaymentWithIdDoesNotExistThen404IsReturned() throws Exception {
    mvc.perform(MockMvcRequestBuilders.get("/payments/" + UUID.randomUUID()))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.message").value("Payment not found. Please check the payment ID and try again."));
  }

  // 3. Test for POST /payments endpoint - bank returns authorized
  @Test
  void whenBankAuthorizesPaymentThenReturn200WithAuthorizedStatus() throws Exception {
    // Card ending in 7 (odd number) will be authorized by bank simulator
    String validPaymentRequest = """
        {
          "card_number": "2222405343248877",
          "expiry_month": 4,
          "expiry_year": 2026,
          "currency": "GBP",
          "amount": 100,
          "cvv": 123
        }
        """;

    mvc.perform(MockMvcRequestBuilders.post("/payments")
            .contentType("application/json")
            .content(validPaymentRequest))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").exists())
        .andExpect(jsonPath("$.status").value("Authorized"))
        .andExpect(jsonPath("$.cardNumberLastFour").value(8877))
        .andExpect(jsonPath("$.expiryMonth").value(4))
        .andExpect(jsonPath("$.expiryYear").value(2026))
        .andExpect(jsonPath("$.currency").value("GBP"))
        .andExpect(jsonPath("$.amount").value(100));
  }

  // 4. Test for POST /payments endpoint - bank returns declined
  @Test
  void whenBankDeclinesPaymentThenReturn200WithDeclinedStatus() throws Exception {
    // Card ending in 2 (even number, not 0) will be declined by bank simulator
    String declinedPaymentRequest = """
        {
          "card_number": "2222405343248112",
          "expiry_month": 1,
          "expiry_year": 2026,
          "currency": "USD",
          "amount": 60000,
          "cvv": 456
        }
        """;

    mvc.perform(MockMvcRequestBuilders.post("/payments")
            .contentType("application/json")
            .content(declinedPaymentRequest))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").exists())
        .andExpect(jsonPath("$.status").value("Declined"))
        .andExpect(jsonPath("$.cardNumberLastFour").value(8112))
        .andExpect(jsonPath("$.expiryMonth").value(1))
        .andExpect(jsonPath("$.expiryYear").value(2026))
        .andExpect(jsonPath("$.currency").value("USD"))
        .andExpect(jsonPath("$.amount").value(60000));
  }

  // 5. Test for POST /payments endpoint - bank returns server error
  @Test
  void whenBankReturnsServerErrorThenReturn503() throws Exception {
    // Card ending in 0 will trigger 503 error from bank simulator
    String serverErrorRequest = """
        {
          "card_number": "2222405343248880",
          "expiry_month": 12,
          "expiry_year": 2026,
          "currency": "GBP",
          "amount": 100,
          "cvv": 789
        }
        """;

    mvc.perform(MockMvcRequestBuilders.post("/payments")
            .contentType("application/json")
            .content(serverErrorRequest))
        .andExpect(status().isServiceUnavailable())
        .andExpect(jsonPath("$.message").value("Payment service temporarily unavailable. Please try again later."));
  }

  
}