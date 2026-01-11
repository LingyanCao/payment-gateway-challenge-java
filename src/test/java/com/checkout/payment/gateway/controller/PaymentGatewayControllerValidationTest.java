package com.checkout.payment.gateway.controller;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

/**
 * Validation tests for POST /payments endpoint.
 * Tests all field-level and class-level validations defined in PostPaymentRequest.
 */
@SpringBootTest
@AutoConfigureMockMvc
class PaymentGatewayControllerValidationTest {

  @Autowired
  private MockMvc mvc;

  // 1. Card Number Validation Tests

  @Test
  void whenCardNumberIsNullThenValidationFails() throws Exception {
    String requestBody = """
        {
          "expiry_month": 12,
          "expiry_year": 2026,
          "currency": "GBP",
          "amount": 100,
          "cvv": 123
        }
        """;

    mvc.perform(MockMvcRequestBuilders.post("/payments")
            .contentType(MediaType.APPLICATION_JSON)
            .content(requestBody))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").value("Validation failed"))
        .andExpect(jsonPath("$.errors.cardNumber").value("Card number is required"));
  }

  @Test
  void whenCardNumberIsTooShortThenValidationFails() throws Exception {
    String requestBody = """
        {
          "card_number": "1234567890123",
          "expiry_month": 12,
          "expiry_year": 2026,
          "currency": "GBP",
          "amount": 100,
          "cvv": 123
        }
        """;

    mvc.perform(MockMvcRequestBuilders.post("/payments")
            .contentType(MediaType.APPLICATION_JSON)
            .content(requestBody))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").value("Validation failed"))
        .andExpect(jsonPath("$.errors.cardNumber").value("Card number must be between 14-19 digits"));
  }

  @Test
  void whenCardNumberIsTooLongThenValidationFails() throws Exception {
    String requestBody = """
        {
          "card_number": "12345678901234567890",
          "expiry_month": 12,
          "expiry_year": 2026,
          "currency": "GBP",
          "amount": 100,
          "cvv": 123
        }
        """;

    mvc.perform(MockMvcRequestBuilders.post("/payments")
            .contentType(MediaType.APPLICATION_JSON)
            .content(requestBody))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").value("Validation failed"))
        .andExpect(jsonPath("$.errors.cardNumber").value("Card number must be between 14-19 digits"));
  }

  @Test
  void whenCardNumberContainsNonDigitsThenValidationFails() throws Exception {
    String requestBody = """
        {
          "card_number": "1234-5678-9012-3456",
          "expiry_month": 12,
          "expiry_year": 2026,
          "currency": "GBP",
          "amount": 100,
          "cvv": 123
        }
        """;

    mvc.perform(MockMvcRequestBuilders.post("/payments")
            .contentType(MediaType.APPLICATION_JSON)
            .content(requestBody))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").value("Validation failed"))
        .andExpect(jsonPath("$.errors.cardNumber").value("Card number must be between 14-19 digits"));
  }

  // 2. Expiry Month Validation Tests

  @Test
  void whenExpiryMonthIsLessThan1ThenValidationFails() throws Exception {
    String requestBody = """
        {
          "card_number": "1234567890123456",
          "expiry_month": 0,
          "expiry_year": 2026,
          "currency": "GBP",
          "amount": 100,
          "cvv": 123
        }
        """;

    mvc.perform(MockMvcRequestBuilders.post("/payments")
            .contentType(MediaType.APPLICATION_JSON)
            .content(requestBody))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").value("Validation failed"))
        .andExpect(jsonPath("$.errors.expiryMonth").value("Expiry month must be between 1 and 12"));
  }

  @Test
  void whenExpiryMonthIsGreaterThan12ThenValidationFails() throws Exception {
    String requestBody = """
        {
          "card_number": "1234567890123456",
          "expiry_month": 13,
          "expiry_year": 2026,
          "currency": "GBP",
          "amount": 100,
          "cvv": 123
        }
        """;

    mvc.perform(MockMvcRequestBuilders.post("/payments")
            .contentType(MediaType.APPLICATION_JSON)
            .content(requestBody))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").value("Validation failed"))
        .andExpect(jsonPath("$.errors.expiryMonth").value("Expiry month must be between 1 and 12"));
  }

  // 3. Expiry Year Validation Tests

  @Test
  void whenExpiryDateIsInThePastThenValidationFails() throws Exception {
    // Using December 2025 which is before January 2026 (current date)
    String requestBody = """
        {
          "card_number": "1234567890123456",
          "expiry_month": 12,
          "expiry_year": 2025,
          "currency": "GBP",
          "amount": 100,
          "cvv": 123
        }
        """;

    mvc.perform(MockMvcRequestBuilders.post("/payments")
            .contentType(MediaType.APPLICATION_JSON)
            .content(requestBody))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").value("Validation failed"))
        .andExpect(jsonPath("$.errors.expiryYear").value("Expiry year must be 2026 or later"));
  }

  // 4. Currency Validation Tests

  @Test
  void whenCurrencyIsNullThenValidationFails() throws Exception {
    String requestBody = """
        {
          "card_number": "1234567890123456",
          "expiry_month": 12,
          "expiry_year": 2026,
          "amount": 100,
          "cvv": 123
        }
        """;

    mvc.perform(MockMvcRequestBuilders.post("/payments")
            .contentType(MediaType.APPLICATION_JSON)
            .content(requestBody))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").value("Validation failed"))
        .andExpect(jsonPath("$.errors.currency").value("Currency is required"));
  }

  @Test
  void whenCurrencyIsNotInWhitelistThenValidationFails() throws Exception {
    String requestBody = """
        {
          "card_number": "1234567890123456",
          "expiry_month": 12,
          "expiry_year": 2026,
          "currency": "JPY",
          "amount": 100,
          "cvv": 123
        }
        """;

    mvc.perform(MockMvcRequestBuilders.post("/payments")
            .contentType(MediaType.APPLICATION_JSON)
            .content(requestBody))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").value("Validation failed"))
        .andExpect(jsonPath("$.errors.currency").value("Currency must be one of: GBP, USD, CNY"));
  }

  // 5. Amount Validation Tests

  @Test
  void whenAmountIsZeroThenValidationFails() throws Exception {
    String requestBody = """
        {
          "card_number": "1234567890123456",
          "expiry_month": 12,
          "expiry_year": 2026,
          "currency": "GBP",
          "amount": 0,
          "cvv": 123
        }
        """;

    mvc.perform(MockMvcRequestBuilders.post("/payments")
            .contentType(MediaType.APPLICATION_JSON)
            .content(requestBody))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").value("Validation failed"))
        .andExpect(jsonPath("$.errors.amount").value("Amount must be greater than 0"));
  }

  @Test
  void whenAmountIsNegativeThenValidationFails() throws Exception {
    String requestBody = """
        {
          "card_number": "1234567890123456",
          "expiry_month": 12,
          "expiry_year": 2026,
          "currency": "GBP",
          "amount": -100,
          "cvv": 123
        }
        """;

    mvc.perform(MockMvcRequestBuilders.post("/payments")
            .contentType(MediaType.APPLICATION_JSON)
            .content(requestBody))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").value("Validation failed"))
        .andExpect(jsonPath("$.errors.amount").value("Amount must be greater than 0"));
  }

  // 6. CVV Validation Tests

  @Test
  void whenCvvIsTooSmallThenValidationFails() throws Exception {
    String requestBody = """
        {
          "card_number": "1234567890123456",
          "expiry_month": 12,
          "expiry_year": 2026,
          "currency": "GBP",
          "amount": 100,
          "cvv": 99
        }
        """;

    mvc.perform(MockMvcRequestBuilders.post("/payments")
            .contentType(MediaType.APPLICATION_JSON)
            .content(requestBody))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").value("Validation failed"))
        .andExpect(jsonPath("$.errors.cvv").value("CVV must be between 3-4 digits"));
  }

  @Test
  void whenCvvIsTooLargeThenValidationFails() throws Exception {
    String requestBody = """
        {
          "card_number": "1234567890123456",
          "expiry_month": 12,
          "expiry_year": 2026,
          "currency": "GBP",
          "amount": 100,
          "cvv": 10000
        }
        """;

    mvc.perform(MockMvcRequestBuilders.post("/payments")
            .contentType(MediaType.APPLICATION_JSON)
            .content(requestBody))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").value("Validation failed"))
        .andExpect(jsonPath("$.errors.cvv").value("CVV must be between 3-4 digits"));
  }

  // 7. Successful Validation Test

  @Test
  void whenAllFieldsAreValidThenPaymentIsProcessed() throws Exception {
    String requestBody = """
        {
          "card_number": "2222405343248877",
          "expiry_month": 12,
          "expiry_year": 2026,
          "currency": "GBP",
          "amount": 100,
          "cvv": 123
        }
        """;

    mvc.perform(MockMvcRequestBuilders.post("/payments")
            .contentType(MediaType.APPLICATION_JSON)
            .content(requestBody))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").exists())
        .andExpect(jsonPath("$.status").exists())
        .andExpect(jsonPath("$.cardNumberLastFour").value(8877))
        .andExpect(jsonPath("$.expiryMonth").value(12))
        .andExpect(jsonPath("$.expiryYear").value(2026))
        .andExpect(jsonPath("$.currency").value("GBP"))
        .andExpect(jsonPath("$.amount").value(100));
  }
}
