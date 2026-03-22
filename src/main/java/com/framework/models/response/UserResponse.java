package com.framework.models.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

/**
 * Response body for user endpoints.
 *
 * Typical shape:
 * {
 *   "userId":    "usr_abc123",
 *   "firstName": "John",
 *   "lastName":  "Doe",
 *   "email":     "john@test.in",
 *   "phone":     "+91-9999999999",
 *   "country":   "IN",
 *   "status":    "ACTIVE",
 *   "role":      "CUSTOMER",
 *   "createdAt": "2024-01-15T10:30:00Z"
 * }
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class UserResponse {
    private String userId;
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private String country;
    private String status;
    private String role;
    private String createdAt;
}
