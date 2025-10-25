package org.example.auth.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AuthResponse {
    private String token;
    private String tokenType = "Bearer";

    // Constructor solo con token
    public AuthResponse(String token) {
        this.token = token;
        this.tokenType = "Bearer";
    }
}
