package com.framework.api.builder;

import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Map;

import static io.restassured.RestAssured.given;

/**
 * Low-level HTTP executor.
 * Wraps RestAssured given().when().method() calls.
 *
 * This is deliberately thin — all auth/header logic stays in RequestBuilder,
 * all validation logic stays in ResponseValidator.
 *
 * Usage:
 *   Response response = ApiClient.get(spec, ApiEndpoints.User.GET_BY_ID,
 *                                     Map.of("userId", "123"));
 */
public class ApiClient {

    private static final Logger log = LogManager.getLogger(ApiClient.class);

    private ApiClient() {}

    // ── GET ───────────────────────────────────────────────────────────────────
    public static Response get(RequestSpecification spec, String endpoint) {
        log.info("GET  {}", endpoint);
        return given(spec).when().get(endpoint);
    }

    public static Response get(RequestSpecification spec, String endpoint,
                               Map<String, Object> pathParams) {
        log.info("GET  {} | pathParams={}", endpoint, pathParams);
        return given(spec).pathParams(pathParams).when().get(endpoint);
    }

    // ── POST ──────────────────────────────────────────────────────────────────
    public static Response post(RequestSpecification spec, String endpoint) {
        log.info("POST {}", endpoint);
        return given(spec).when().post(endpoint);
    }

    public static Response post(RequestSpecification spec, String endpoint, Object body) {
        log.info("POST {} | body={}", endpoint, body);
        return given(spec).body(body).when().post(endpoint);
    }

    // ── PUT ───────────────────────────────────────────────────────────────────
    public static Response put(RequestSpecification spec, String endpoint, Object body) {
        log.info("PUT  {} | body={}", endpoint, body);
        return given(spec).body(body).when().put(endpoint);
    }

    public static Response put(RequestSpecification spec, String endpoint,
                               Object body, Map<String, Object> pathParams) {
        log.info("PUT  {} | pathParams={}", endpoint, pathParams);
        return given(spec).body(body).pathParams(pathParams).when().put(endpoint);
    }

    // ── PATCH ─────────────────────────────────────────────────────────────────
    public static Response patch(RequestSpecification spec, String endpoint, Object body) {
        log.info("PATCH {}", endpoint);
        return given(spec).body(body).when().patch(endpoint);
    }

    public static Response patch(RequestSpecification spec, String endpoint,
                                 Object body, Map<String, Object> pathParams) {
        log.info("PATCH {} | pathParams={}", endpoint, pathParams);
        return given(spec).body(body).pathParams(pathParams).when().patch(endpoint);
    }

    // ── DELETE ────────────────────────────────────────────────────────────────
    public static Response delete(RequestSpecification spec, String endpoint,
                                  Map<String, Object> pathParams) {
        log.info("DELETE {}", endpoint);
        return given(spec).pathParams(pathParams).when().delete(endpoint);
    }
}
