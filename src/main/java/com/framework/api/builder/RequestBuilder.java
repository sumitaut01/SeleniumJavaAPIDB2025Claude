package com.framework.api.builder;

import com.framework.api.filters.ExtentReportApiFilter;
import com.framework.config.ConfigManager;
import com.framework.context.TestContext;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.filter.log.LogDetail;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Map;

/**
 * Fluent builder for RestAssured RequestSpecification.
 *
 * Design:
 *  - baseUri always comes from ConfigManager (env + country aware)
 *  - auth token is picked from TestContext (thread-local) when available
 *  - callers chain .withAuth() / .withHeaders() / .withContentType() etc.
 *
 * Usage:
 *   RequestSpecification spec = RequestBuilder.create()
 *       .withAuth()
 *       .withContentType(ContentType.JSON)
 *       .withQueryParam("country", "IN")
 *       .build();
 *
 *   given(spec).get(ApiEndpoints.User.GET_ALL).then()...
 */
public class RequestBuilder {

    private static final Logger log = LogManager.getLogger(RequestBuilder.class);

    private final RequestSpecBuilder specBuilder;

    private RequestBuilder() {
        String baseUri = ConfigManager.getInstance().getApiUrl();
        log.debug("RequestBuilder → baseUri={}", baseUri);

        this.specBuilder = new RequestSpecBuilder()
                .setBaseUri(baseUri)
                .setContentType(ContentType.JSON)
                .setAccept(ContentType.JSON)
                .addFilter(new ExtentReportApiFilter())  // auto-logs req+resp to ExtentReport
                .log(LogDetail.ALL);
    }

    // ── Entry point ──────────────────────────────────────────────────────────
    public static RequestBuilder create() {
        return new RequestBuilder();
    }

    // ── Auth ─────────────────────────────────────────────────────────────────

    /** Attaches Bearer token from the current thread's TestContext. */
    public RequestBuilder withAuth() {
        String token = TestContext.get().getAuthToken();
        if (token == null || token.isBlank()) {
            log.warn("withAuth() called but no auth token in TestContext for thread: {}",
                    Thread.currentThread().getName());
        } else {
            specBuilder.addHeader("Authorization", "Bearer " + token);
        }
        return this;
    }

    /** Attaches an explicit Bearer token. */
    public RequestBuilder withAuth(String bearerToken) {
        specBuilder.addHeader("Authorization", "Bearer " + bearerToken);
        return this;
    }

    /** Basic auth. */
    public RequestBuilder withBasicAuth(String username, String password) {
        specBuilder.setAuth(
                io.restassured.RestAssured.basic(username, password));
        return this;
    }

    // ── Content & Accept ─────────────────────────────────────────────────────
    public RequestBuilder withContentType(ContentType contentType) {
        specBuilder.setContentType(contentType);
        return this;
    }

    public RequestBuilder withAccept(ContentType accept) {
        specBuilder.setAccept(accept);
        return this;
    }

    // ── Headers ──────────────────────────────────────────────────────────────
    public RequestBuilder withHeader(String name, String value) {
        specBuilder.addHeader(name, value);
        return this;
    }

    public RequestBuilder withHeaders(Map<String, String> headers) {
        headers.forEach(specBuilder::addHeader);
        return this;
    }

    /** Adds country-specific header (useful for multi-country routing). */
    public RequestBuilder withCountryHeader() {
        String country = ConfigManager.getInstance().getCountry().name();
        specBuilder.addHeader("X-Country-Code", country);
        return this;
    }

    // ── Query params ─────────────────────────────────────────────────────────
    public RequestBuilder withQueryParam(String key, Object value) {
        specBuilder.addQueryParam(key, value);
        return this;
    }

    public RequestBuilder withQueryParams(Map<String, ?> params) {
        specBuilder.addQueryParams(params);
        return this;
    }

    // ── Body ─────────────────────────────────────────────────────────────────
    public RequestBuilder withBody(Object body) {
        specBuilder.setBody(body);
        return this;
    }

    // ── Relaxed HTTPS (self-signed certs in non-prod) ─────────────────────────
    public RequestBuilder relaxedHttps() {
        specBuilder.setRelaxedHTTPSValidation();
        return this;
    }

    // ── Build ─────────────────────────────────────────────────────────────────
    public RequestSpecification build() {
        return specBuilder.build();
    }
}
