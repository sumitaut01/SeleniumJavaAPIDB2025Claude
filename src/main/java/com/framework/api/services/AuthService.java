package com.framework.api.services;

import com.framework.api.builder.ApiClient;
import com.framework.api.builder.RequestBuilder;
import com.framework.api.endpoints.ApiEndpoints;
import com.framework.api.filters.ExtentReportApiFilter;
import com.framework.api.validators.ResponseValidator;
import com.framework.context.TestContext;
import com.framework.models.request.LoginRequest;
import com.framework.models.response.LoginResponse;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * API Service for Authentication.
 *
 * Responsibilities:
 *  - Performs login via API
 *  - Extracts + stores token in TestContext (so other services can use it)
 *  - Returns LoginResponse for test-level assertions
 *
 * Think of this as the "Page Object" equivalent for the Auth API.
 *
 * Usage in a test:
 *   authService.loginAndStoreToken("user@in.com", "pass123");
 *   // Now TestContext.get().getAuthToken() is populated
 *   // All subsequent RequestBuilder.create().withAuth() calls will use it
 */
public class AuthService {

    private static final Logger log = LogManager.getLogger(AuthService.class);

    /** Authenticates and stores the token in TestContext. Returns the full response. */
    public Response loginAndStoreToken(String username, String password) {
        log.info("AuthService.login → user={}", username);

        LoginRequest body = LoginRequest.builder()
                .username(username)
                .password(password)
                .build();

        RequestSpecification spec = RequestBuilder.create()
                .withCountryHeader()
                .relaxedHttps()
                .build();

        // Note: no .withAuth() here — this IS the auth call
        Response response = ApiClient.post(spec, ApiEndpoints.Auth.LOGIN, body);

        if (response.statusCode() == 200) {
            LoginResponse loginResp = response.as(LoginResponse.class);
            TestContext.get().setAuthToken(loginResp.getAccessToken());
            TestContext.get().setRefreshToken(loginResp.getRefreshToken());
            log.info("AuthService: token stored in TestContext for thread={}",
                    Thread.currentThread().getName());
        } else {
            log.error("AuthService: login failed with status={}", response.statusCode());
        }

        return response;
    }

    /** Same as loginAndStoreToken but also asserts 200. */
    public void loginExpectSuccess(String username, String password) {
        Response response = loginAndStoreToken(username, password);
        ResponseValidator.of(response)
                .statusCode(200)
                .hasField("accessToken")
                .validate();
    }

    /** Logs out using the token already in TestContext. */
    public Response logout() {
        log.info("AuthService.logout");
        RequestSpecification spec = RequestBuilder.create()
                .withAuth()
                .build();
        return ApiClient.post(spec, ApiEndpoints.Auth.LOGOUT);
    }
}
