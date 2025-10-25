package org.example.auth.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.example.auth.models.*;
import org.example.auth.repository.UserRepository;
import org.example.auth.services.JwtService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final RestTemplate restTemplate;
    private final String apiKey = "2ea526178b2d7a7d8706d4f59ae18cb2";

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;
    private final UserRepository userRepository;


    private HttpHeaders jsonHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Accept", MediaType.APPLICATION_JSON_VALUE);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    // Login - Generar token
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody AuthRequest request) {
        // Autenticar usuario
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getUsername(),
                        request.getPassword()
                )
        );

        // Cargar usuario y generar token
        UserDetails userDetails = userDetailsService.loadUserByUsername(request.getUsername());
        String token = jwtService.generateToken(userDetails);

        return ResponseEntity.ok(new AuthResponse(token));
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@RequestBody RegisterRequest request) {
        // Verificar si el usuario ya existe
        if (userRepository.existsByUsername(request.getUsername())) {
            return ResponseEntity.badRequest().build();
        }

        PasswordEncoder encoder = new BCryptPasswordEncoder();

        // --- Crear el body para Nessie ---
        String apiKey = "e74c2feafa6f8b24c71ded25e2baeb2e";
        String url = "http://api.nessieisreal.com/customers?key=" + apiKey;

        ReqNessiCustomer reqNessiCustomer = new ReqNessiCustomer();
        reqNessiCustomer.setAddress(request.getAddress());
        reqNessiCustomer.setFirst_name(request.getFirstName());
        reqNessiCustomer.setLast_name(request.getLastName());

        // --- Enviar solicitud al API externo ---
        ResponseEntity<String> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                new HttpEntity<>(reqNessiCustomer, jsonHeaders()),
                String.class
        );

        // --- Parsear el JSON con ObjectMapper ---
        String nessieId = null;
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(response.getBody());
            JsonNode objectCreated = root.path("objectCreated");
            if (!objectCreated.isMissingNode()) {
                nessieId = objectCreated.path("_id").asText();
            }
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }

        // --- Validar si se obtuvo el ID ---
        if (nessieId == null || nessieId.isEmpty()) {
            return ResponseEntity.internalServerError().body(new AuthResponse("Error creando usuario en Nessie"));
        }

        // --- Crear usuario local ---
        User user = User.builder()
                .username(request.getUsername())
                .password(encoder.encode(request.getPassword()))
                ._id("68f970a89683f20dd51a3ed0") // Guardamos el id de Nessie
                .build();

        userRepository.save(user);

        // --- Generar token ---
        String token = jwtService.generateToken(user);

        return ResponseEntity.ok(new AuthResponse(token));
    }

    @GetMapping("/purchases-info")
    public ResponseEntity<?> getPurchasesInfo(@RequestHeader("Authorization") String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Token inválido o ausente");
        }

        String token = authHeader.substring(7);
        String apiKey = "e74c2feafa6f8b24c71ded25e2baeb2e";

        try {
            // 1. Extraer username y obtener _id del usuario
            String username = jwtService.extractUsername(token);
            Optional<User> optionalUser = userRepository.findByUsername(username);

            if (optionalUser.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Usuario no encontrado");
            }

            String customerId = optionalUser.get().get_id();

            // 2. Obtener las cuentas del cliente
            RestTemplate restTemplate = new RestTemplate();
            String accountsUrl = "http://api.nessieisreal.com/customers/" + customerId + "/accounts?key=" + apiKey;

            ResponseEntity<List> accountsResponse = restTemplate.exchange(
                    accountsUrl,
                    HttpMethod.GET,
                    new HttpEntity<>(createHeaders()),
                    List.class
            );

            List<Map<String, Object>> accounts = accountsResponse.getBody();
            if (accounts == null || accounts.isEmpty()) {
                return ResponseEntity.ok(Collections.emptyList());
            }

            // 3. Crear ExecutorService para manejar las peticiones asíncronas
            ExecutorService executor = Executors.newFixedThreadPool(10);

            try {
                // 4. Por cada cuenta, crear un CompletableFuture para obtener las compras
                List<CompletableFuture<List<Map<String, Object>>>> accountFutures = new ArrayList<>();

                for (Map<String, Object> account : accounts) {
                    CompletableFuture<List<Map<String, Object>>> future = CompletableFuture.supplyAsync(() -> {
                        String accountId = (String) account.get("_id");
                        String purchasesUrl = "http://api.nessieisreal.com/accounts/" + accountId + "/purchases?key=" + apiKey;

                        try {
                            ResponseEntity<List> purchasesResponse = restTemplate.exchange(
                                    purchasesUrl,
                                    HttpMethod.GET,
                                    new HttpEntity<>(createHeaders()),
                                    List.class
                            );

                            List<Map<String, Object>> purchases = purchasesResponse.getBody();
                            if (purchases == null || purchases.isEmpty()) {
                                return new ArrayList<Map<String, Object>>();
                            }

                            // 5. Por cada compra, obtener información del merchant (también en paralelo)
                            List<CompletableFuture<Map<String, Object>>> merchantFutures = new ArrayList<>();

                            for (Map<String, Object> purchase : purchases) {
                                CompletableFuture<Map<String, Object>> merchantFuture = CompletableFuture.supplyAsync(() -> {
                                    String merchantId = (String) purchase.get("merchant_id");
                                    Object amountObj = purchase.get("amount");
                                    String merchantUrl = "http://api.nessieisreal.com/merchants/" + merchantId + "?key=" + apiKey;

                                    try {
                                        ResponseEntity<Map> merchantResponse = restTemplate.exchange(
                                                merchantUrl,
                                                HttpMethod.GET,
                                                new HttpEntity<>(createHeaders()),
                                                Map.class
                                        );

                                        Map<String, Object> merchant = merchantResponse.getBody();
                                        if (merchant != null) {
                                            Map<String, Object> result = new HashMap<>();
                                            result.put("amount", amountObj);
                                            result.put("name", merchant.get("name"));

                                            Map<String, Object> geocode = (Map<String, Object>) merchant.get("geocode");
                                            if (geocode != null) {
                                                result.put("lat", geocode.get("lat"));
                                                result.put("lng", geocode.get("lng"));
                                            }
                                            return result;
                                        }
                                    } catch (Exception e) {
                                        System.err.println("Error obteniendo merchant: " + e.getMessage());
                                    }
                                    return null;
                                }, executor);

                                merchantFutures.add(merchantFuture);
                            }

                            // Esperar a que todas las peticiones de merchants terminen
                            List<Map<String, Object>> merchantResults = new ArrayList<>();
                            for (CompletableFuture<Map<String, Object>> mf : merchantFutures) {
                                Map<String, Object> result = mf.join();
                                if (result != null) {
                                    merchantResults.add(result);
                                }
                            }
                            return merchantResults;

                        } catch (Exception e) {
                            System.err.println("Error obteniendo compras de cuenta: " + e.getMessage());
                            return new ArrayList<Map<String, Object>>();
                        }
                    }, executor);

                    accountFutures.add(future);
                }

                // 6. Esperar a que todas las cuentas terminen y aplanar los resultados
                List<Map<String, Object>> finalResults = new ArrayList<>();
                for (CompletableFuture<List<Map<String, Object>>> af : accountFutures) {
                    List<Map<String, Object>> accountResults = af.join();
                    finalResults.addAll(accountResults);
                }

                return ResponseEntity.ok(finalResults);

            } finally {
                executor.shutdown();
                try {
                    if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
                        executor.shutdownNow();
                    }
                } catch (InterruptedException e) {
                    executor.shutdownNow();
                }
            }

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error al procesar la solicitud: " + e.getMessage());
        }
    }

    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Accept", "application/json");
        return headers;
    }

    @GetMapping("/me")
    public ResponseEntity<?> getUserInfo(@RequestHeader("Authorization") String authHeader) {
        // 1. Extraer el token sin "Bearer "
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Token inválido o ausente");
        }
        String token = authHeader.substring(7); // quitar "Bearer "
        try {
            // 2. Obtener el username del token
            String username = jwtService.extractUsername(token);
            // 3. Buscar el usuario en la base de datos
            Optional<User> optionalUser = userRepository.findByUsername(username);
            if (optionalUser.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Usuario no encontrado");
            }
            User user = optionalUser.get();
            // 4. Devolver respuesta con datos mínimos
            Map<String, Object> response = new HashMap<>();
            response.put("username", user.getUsername());
            response.put("_id", user.get_id());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Token inválido");
        }
    }




    // Endpoint para validar token (opcional pero útil)
    @GetMapping("/validate")
    public ResponseEntity<Boolean> validateToken(
            @RequestHeader("Authorization") String authHeader
    ) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            try {
                String username = jwtService.extractUsername(token);
                UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                boolean isValid = jwtService.validateToken(token, userDetails);
                return ResponseEntity.ok(isValid);
            } catch (Exception e) {
                return ResponseEntity.ok(false);
            }
        }
        return ResponseEntity.ok(false);
    }
}
