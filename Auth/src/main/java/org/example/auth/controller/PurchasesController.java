package org.example.auth.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@RestController
@RequestMapping("/api/purchases")
@RequiredArgsConstructor
public class PurchasesController {

    private final RestTemplate restTemplate;
    private final String apiKey = "2ea526178b2d7a7d8706d4f59ae18cb2";

    private HttpHeaders jsonHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Accept", MediaType.APPLICATION_JSON_VALUE);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    // GET /accounts/{accountId}/purchases
    @GetMapping("/accounts/{accountId}")
    public ResponseEntity<Object> getPurchasesByAccount(@PathVariable String accountId) {
        String url = "http://api.nessieisreal.com/accounts/" + accountId + "/purchases?key=" + apiKey;
        ResponseEntity<Object> response = restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(jsonHeaders()), Object.class);
        return response;
    }

    // GET /merchants/{merchantId}/accounts/{accountId}/purchases
    @GetMapping("/merchants/{merchantId}/accounts/{accountId}")
    public ResponseEntity<Object> getPurchasesByMerchantAccount(
            @PathVariable String merchantId,
            @PathVariable String accountId
    ) {
        String url = "http://api.nessieisreal.com/merchants/" + merchantId + "/accounts/" + accountId + "/purchases?key=" + apiKey;
        ResponseEntity<Object> response = restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(jsonHeaders()), Object.class);
        return response;
    }

    // GET /merchants/{merchantId}/purchases
    @GetMapping("/merchants/{merchantId}")
    public ResponseEntity<Object> getPurchasesByMerchant(@PathVariable String merchantId) {
        String url = "http://api.nessieisreal.com/merchants/" + merchantId + "/purchases?key=" + apiKey;
        ResponseEntity<Object> response = restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(jsonHeaders()), Object.class);
        return response;
    }

    // GET /{purchaseId}
    @GetMapping("/{purchaseId}")
    public ResponseEntity<Object> getPurchaseById(@PathVariable String purchaseId) {
        String url = "http://api.nessieisreal.com/purchases/" + purchaseId + "?key=" + apiKey;
        ResponseEntity<Object> response = restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(jsonHeaders()), Object.class);
        return response;
    }

    // POST /accounts/{accountId}/purchases
    @PostMapping("/accounts/{accountId}")
    public ResponseEntity<Object> createPurchase(
            @PathVariable String accountId,
            @RequestBody Map<String, Object> purchaseRequest
    ) {
        String url = "http://api.nessieisreal.com/accounts/" + accountId + "/purchases?key=" + apiKey;
        ResponseEntity<Object> response = restTemplate.exchange(url, HttpMethod.POST, new HttpEntity<>(purchaseRequest, jsonHeaders()), Object.class);
        return response;
    }

    // PUT /{purchaseId}
    @PutMapping("/{purchaseId}")
    public ResponseEntity<Object> updatePurchase(
            @PathVariable String purchaseId,
            @RequestBody Map<String, Object> purchaseRequest
    ) {
        String url = "http://api.nessieisreal.com/purchases/" + purchaseId + "?key=" + apiKey;
        ResponseEntity<Object> response = restTemplate.exchange(url, HttpMethod.PUT, new HttpEntity<>(purchaseRequest, jsonHeaders()), Object.class);
        return response;
    }

    // DELETE /{purchaseId}
    @DeleteMapping("/{purchaseId}")
    public ResponseEntity<Object> deletePurchase(@PathVariable String purchaseId) {
        String url = "http://api.nessieisreal.com/purchases/" + purchaseId + "?key=" + apiKey;
        ResponseEntity<Object> response = restTemplate.exchange(url, HttpMethod.DELETE, new HttpEntity<>(jsonHeaders()), Object.class);
        return response;
    }
}
