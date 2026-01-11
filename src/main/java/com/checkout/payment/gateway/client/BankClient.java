package com.checkout.payment.gateway.client;

import com.checkout.payment.gateway.client.model.BankRequest;
import com.checkout.payment.gateway.client.model.BankResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class BankClient {

  private static final Logger LOG = LoggerFactory.getLogger(BankClient.class);

  private final RestTemplate restTemplate;

  @Value("${bank.simulator.url:http://localhost:8080}")
  private String bankSimulatorUrl;

  public BankClient() {
    this.restTemplate = new RestTemplate();
  }

  public BankResponse processPayment(BankRequest request) {
    String bankUrl = bankSimulatorUrl + "/payments";

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);

    HttpEntity<BankRequest> entity = new HttpEntity<>(request, headers);

    LOG.debug("Calling bank simulator at {} with request: {}", bankUrl, request);
    
    ResponseEntity<BankResponse> response = restTemplate.postForEntity(
        bankUrl,
        entity,
        BankResponse.class
    );

    BankResponse bankResponse = response.getBody();
    LOG.debug("Bank simulator response: {}", bankResponse);
    
    return bankResponse;
  }
}
