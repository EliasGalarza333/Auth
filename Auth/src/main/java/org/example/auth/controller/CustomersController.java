package org.example.auth.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@RestController
@RequestMapping("/api/customers")
@RequiredArgsConstructor
public class CustomersController {

    private final RestTemplate restTemplate;
    private final String apiKey = "2ea526178b2d7a7d8706d4f59ae18cb2";

    private HttpHeaders jsonHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Accept", MediaType.APPLICATION_JSON_VALUE);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    // GET /accounts/{accountId}/customer
    @GetMapping("/accounts/{accountId}")
    public ResponseEntity<Object> getCustomerByAccount(@PathVariable String accountId) {
        String url = "http://api.nessieisreal.com/accounts/" + accountId + "/customer?key=" + apiKey;
        ResponseEntity<Object> response = restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(jsonHeaders()), Object.class);
        return response;
    }

    // GET /customers
    @GetMapping
    public ResponseEntity<Object> getAllCustomers() {
        String url = "http://api.nessieisreal.com/customers?key=" + apiKey;
        ResponseEntity<Object> response = restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(jsonHeaders()), Object.class);
        return response;
    }

    // GET /customers/{customerId}
    @GetMapping("/{customerId}")
    public ResponseEntity<Object> getCustomerById(@PathVariable String customerId) {
        String url = "http://api.nessieisreal.com/customers/" + customerId + "?key=" + apiKey;
        ResponseEntity<Object> response = restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(jsonHeaders()), Object.class);
        return response;
    }

    // POST /customers
    @PostMapping
    public ResponseEntity<Object> createCustomer(@RequestBody Map<String, Object> customerRequest) {
        String url = "http://api.nessieisreal.com/customers?key=" + apiKey;
        ResponseEntity<Object> response = restTemplate.exchange(url, HttpMethod.POST, new HttpEntity<>(customerRequest, jsonHeaders()), Object.class);
        return response;
    }

    // PUT /customers/{customerId}
    @PutMapping("/{customerId}")
    public ResponseEntity<Object> updateCustomer(
            @PathVariable String customerId,
            @RequestBody Map<String, Object> customerRequest
    ) {
        String url = "http://api.nessieisreal.com/customers/" + customerId + "?key=" + apiKey;
        ResponseEntity<Object> response = restTemplate.exchange(url, HttpMethod.PUT, new HttpEntity<>(customerRequest, jsonHeaders()), Object.class);
        return response;
    }
}
