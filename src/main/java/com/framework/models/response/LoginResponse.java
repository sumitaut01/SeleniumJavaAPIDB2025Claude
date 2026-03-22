package com.framework.models.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

/**
 * Response body for POST /api/v1/auth/login
 *
 * Typical shape:
 * {
 *   "accessToken":  "eyJhbG...",
 *   "refreshToken": "eyJhbG...",
 *   "expiresIn":    3600,
 *   "tokenType":    "Bearer"
 * }
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class LoginResponse {
    private String accessToken;
    private String refreshToken;
    private int    expiresIn;
    private String tokenType;
}
