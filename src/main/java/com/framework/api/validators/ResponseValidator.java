package com.framework.api.validators;

import io.restassured.module.jsv.JsonSchemaValidator;
import io.restassured.response.Response;
import io.restassured.response.ValidatableResponse;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.assertj.core.api.SoftAssertions;

import java.io.File;

import static org.hamcrest.Matchers.*;

/**
 * Fluent response validation wrapper.
 *
 * Combines:
 *  - RestAssured Hamcrest matchers (statusCode, body path)
 *  - JSON Schema validation
 *  - AssertJ soft assertions for custom checks
 *
 * Usage:
 *   ResponseValidator.of(response)
 *       .statusCode(201)
 *       .hasField("data.userId")
 *       .fieldEquals("data.status", "ACTIVE")
 *       .matchesSchema("schemas/user-create-response.json")
 *       .validate();
 */
public class ResponseValidator {

    private static final Logger log = LogManager.getLogger(ResponseValidator.class);

    private final Response           response;
    private final ValidatableResponse validatable;
    private final SoftAssertions      soft;

    private ResponseValidator(Response response) {
        this.response    = response;
        this.validatable = response.then().log().ifError();
        this.soft        = new SoftAssertions();
        log.debug("ResponseValidator → status={}, time={}ms",
                response.statusCode(), response.time());
    }

    public static ResponseValidator of(Response response) {
        return new ResponseValidator(response);
    }

    // ── Status ────────────────────────────────────────────────────────────────
    public ResponseValidator statusCode(int expected) {
        validatable.statusCode(expected);
        return this;
    }

    public ResponseValidator statusCodeIn(int... expected) {
        soft.assertThat(response.statusCode())
            .as("Status code should be one of the expected values")
            .isIn(intArrayToBoxed(expected));
        return this;
    }

    // ── Content-Type ──────────────────────────────────────────────────────────
    public ResponseValidator contentTypeIsJson() {
        validatable.contentType("application/json");
        return this;
    }

    // ── Body path assertions ───────────────────────────────────────────────────
    /** Assert a JsonPath field exists and is not null/empty. */
    public ResponseValidator hasField(String jsonPath) {
        soft.assertThat(response.jsonPath().getString(jsonPath))
            .as("Field '%s' should exist and be non-null", jsonPath)
            .isNotNull()
            .isNotBlank();
        return this;
    }

    /** Assert a JsonPath field equals exact value. */
    public ResponseValidator fieldEquals(String jsonPath, Object expected) {
        Object actual = response.jsonPath().get(jsonPath);
        soft.assertThat(actual)
            .as("Field '%s' expected='%s' actual='%s'", jsonPath, expected, actual)
            .isEqualTo(expected);
        return this;
    }

    /** Assert a JsonPath string field contains substring. */
    public ResponseValidator fieldContains(String jsonPath, String substring) {
        String actual = response.jsonPath().getString(jsonPath);
        soft.assertThat(actual)
            .as("Field '%s' should contain '%s'", jsonPath, substring)
            .contains(substring);
        return this;
    }

    /** Assert a JsonPath array/list is not empty. */
    public ResponseValidator listNotEmpty(String jsonPath) {
        validatable.body(jsonPath, not(empty()));
        return this;
    }

    /** Assert response time is within threshold (ms). */
    public ResponseValidator responseTimeBelow(long maxMillis) {
        soft.assertThat(response.time())
            .as("Response time should be below %d ms", maxMillis)
            .isLessThan(maxMillis);
        return this;
    }

    // ── JSON Schema ───────────────────────────────────────────────────────────
    /**
     * Validates response body against a JSON schema file.
     * Schema files live in src/test/resources/schemas/
     *
     * @param schemaRelativePath e.g. "schemas/user-response.json"
     */
    public ResponseValidator matchesSchema(String schemaRelativePath) {
        File schemaFile = new File("src/test/resources/" + schemaRelativePath);
        if (!schemaFile.exists()) {
            log.warn("Schema file not found, skipping schema validation: {}", schemaFile.getAbsolutePath());
            return this;
        }
        validatable.body(JsonSchemaValidator.matchesJsonSchema(schemaFile));
        return this;
    }

    // ── Raw access (for chaining into other calls) ────────────────────────────
    public <T> T extractAs(Class<T> clazz) {
        return response.as(clazz);
    }

    public String extractString(String jsonPath) {
        return response.jsonPath().getString(jsonPath);
    }

    public int extractInt(String jsonPath) {
        return response.jsonPath().getInt(jsonPath);
    }

    public Response getResponse() {
        return response;
    }

    // ── Terminal ──────────────────────────────────────────────────────────────
    /** Runs all soft assertions. Call at the end of a validation chain. */
    public ResponseValidator validate() {
        soft.assertAll();
        return this;
    }

    // ── Private helpers ───────────────────────────────────────────────────────
    private Integer[] intArrayToBoxed(int[] arr) {
        Integer[] boxed = new Integer[arr.length];
        for (int i = 0; i < arr.length; i++) boxed[i] = arr[i];
        return boxed;
    }
}
