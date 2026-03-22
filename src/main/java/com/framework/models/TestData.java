package com.framework.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

/**
 * Generic test data model.
 * Used by both Excel and JSON data providers.
 * Add / remove fields to match your application.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class TestData {

    // ── Common fields ────────────────────────────────────────────────────────
    private String testCaseId;
    private String country;         // IN / US / UK / AU
    private String description;
    private boolean runFlag;        // true = execute, false = skip

    // ── Login data ───────────────────────────────────────────────────────────
    private String username;
    private String password;
    private String expectedTitle;

    // ── Registration data ────────────────────────────────────────────────────
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private String address;

    // ── Expected results ─────────────────────────────────────────────────────
    private String expectedMessage;
    private String expectedUrl;
}
