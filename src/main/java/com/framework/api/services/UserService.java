package com.framework.api.services;

import com.framework.api.builder.ApiClient;
import com.framework.api.builder.RequestBuilder;
import com.framework.api.endpoints.ApiEndpoints;
import com.framework.api.validators.ResponseValidator;
import com.framework.config.ConfigManager;
import com.framework.context.TestContext;
import com.framework.models.request.CreateUserRequest;
import com.framework.models.response.UserResponse;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Map;
import java.util.UUID;

/**
 * API Service for User resource.
 *
 * Key capability: createUserAndStoreContext() creates a user via API and
 * stores the returned userId + email in TestContext so that a subsequent
 * UI step (LoginPage, ProfilePage, etc.) can use them without re-passing data.
 *
 * This is the primary mechanism for the "API → UI" hybrid test pattern.
 */
public class UserService {

    private static final Logger log = LogManager.getLogger(UserService.class);

    // ── Create ────────────────────────────────────────────────────────────────

    /**
     * Creates a user via API and stores userId + email in TestContext.
     * The auth token must already be in TestContext (call AuthService.loginExpectSuccess first).
     *
     * @return the full Response for optional further inspection in the test
     */
    public Response createUserAndStoreContext(CreateUserRequest request) {
        log.info("UserService.createUser → email={}", request.getEmail());

        RequestSpecification spec = RequestBuilder.create()
                .withAuth()
                .withCountryHeader()
                .build();

        Response response = ApiClient.post(spec, ApiEndpoints.User.CREATE, request);

        if (response.statusCode() == 201) {
            String userId = response.jsonPath().getString("data.userId");
            String email  = response.jsonPath().getString("data.email");

            TestContext.get().setUserId(userId);
            TestContext.get().setCreatedEmail(email);
            TestContext.get().setCreatedUsername(request.getFirstName() + " " + request.getLastName());

            log.info("UserService: user created → userId={}, email={}", userId, email);
        }

        return response;
    }

    /**
     * Creates a user and asserts 201. Stores context. Returns UserResponse POJO.
     */
    public UserResponse createUserExpectSuccess(CreateUserRequest request) {
        Response response = createUserAndStoreContext(request);

        ResponseValidator.of(response)
                .statusCode(201)
                .hasField("data.userId")
                .hasField("data.email")
                .fieldEquals("data.status", "ACTIVE")
                .matchesSchema("schemas/user-response.json")
                .validate();

        return response.jsonPath().getObject("data", UserResponse.class);
    }

    /**
     * Convenience: builds a unique CreateUserRequest from country-aware defaults.
     * Useful for generating test data on the fly without maintaining Excel/JSON rows.
     */
    public CreateUserRequest buildUniqueUserRequest() {
        String country = ConfigManager.getInstance().getCountry().name();
        String uid     = UUID.randomUUID().toString().substring(0, 8);

        return CreateUserRequest.builder()
                .firstName("Test")
                .lastName("User_" + uid)
                .email("test_" + uid + "@auto." + country.toLowerCase() + ".com")
                .phone(ConfigManager.getInstance().getCountry().getDialCode() + "9000000000")
                .password("Test@1234")
                .country(country)
                .role("CUSTOMER")
                .build();
    }

    // ── Read ──────────────────────────────────────────────────────────────────

    public Response getUserById(String userId) {
        log.info("UserService.getUser → userId={}", userId);

        RequestSpecification spec = RequestBuilder.create()
                .withAuth()
                .withCountryHeader()
                .build();

        return ApiClient.get(spec, ApiEndpoints.User.GET_BY_ID,
                Map.of("userId", userId));
    }

    public UserResponse getUserExpectSuccess(String userId) {
        Response response = getUserById(userId);

        ResponseValidator.of(response)
                .statusCode(200)
                .fieldEquals("data.userId", userId)
                .validate();

        return response.jsonPath().getObject("data", UserResponse.class);
    }

    // ── Delete ────────────────────────────────────────────────────────────────

    public Response deleteUser(String userId) {
        log.info("UserService.deleteUser → userId={}", userId);

        RequestSpecification spec = RequestBuilder.create()
                .withAuth()
                .withCountryHeader()
                .build();

        return ApiClient.delete(spec, ApiEndpoints.User.DELETE,
                Map.of("userId", userId));
    }

    /** Cleanup helper: deletes user stored in TestContext. Safe to call in @AfterMethod. */
    public void deleteUserFromContext() {
        String userId = TestContext.get().getUserId();
        if (userId != null && !userId.isBlank()) {
            Response response = deleteUser(userId);
            log.info("UserService: cleanup delete userId={} → status={}",
                    userId, response.statusCode());
        }
    }
}
