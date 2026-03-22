package com.framework.tests.api;

import com.framework.api.validators.ResponseValidator;
import com.framework.models.request.CreateUserRequest;
import com.framework.models.response.UserResponse;
import com.framework.tests.BaseApiTest;
import io.restassured.response.Response;
import org.assertj.core.api.SoftAssertions;
import org.testng.annotations.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pure API tests — no browser involved at all.
 *
 * Demonstrates:
 *  TC01 - Full CRUD chain: auth → create → get → delete
 *  TC02 - Negative: create with duplicate email
 *  TC03 - Negative: get non-existent user
 *  TC04 - Multi-call chain: create → update status → verify status changed
 */
public class UserApiTest extends BaseApiTest {

    // ── TC01: Full CRUD chain ─────────────────────────────────────────────────
    @Test(
        description = "Create user via API, fetch it, then delete it — full CRUD chain",
        groups      = { "api", "smoke", "regression" }
    )
    public void testUserFullCrudChain() {
        // Step 1: Authenticate — stores token in TestContext
        authService.loginExpectSuccess("admin@qa.in", "Admin@1234");

        // Step 2: Build unique user payload (country-aware)
        CreateUserRequest newUser = userService.buildUniqueUserRequest();

        // Step 3: Create user via POST /api/v1/users
        //         createUserExpectSuccess asserts 201 + schema + stores userId in ctx
        UserResponse created = userService.createUserExpectSuccess(newUser);

        SoftAssertions soft = new SoftAssertions();
        soft.assertThat(created.getStatus()).isEqualTo("ACTIVE");
        soft.assertThat(created.getEmail()).isEqualTo(newUser.getEmail());
        soft.assertThat(created.getCountry()).isEqualTo(country());
        soft.assertAll();

        // Step 4: GET /api/v1/users/{userId} — verify same data comes back
        UserResponse fetched = userService.getUserExpectSuccess(ctx().getUserId());

        assertThat(fetched.getUserId()).isEqualTo(created.getUserId());
        assertThat(fetched.getEmail()).isEqualTo(created.getEmail());

        // Step 5: DELETE — cleanup
        Response deleteResp = userService.deleteUser(ctx().getUserId());

        ResponseValidator.of(deleteResp)
                .statusCode(204)   // or 200 depending on your API
                .validate();
    }

    // ── TC02: Duplicate email should return 409 ───────────────────────────────
    @Test(
        description = "Create user with duplicate email should return 409 Conflict",
        groups      = { "api", "negative" }
    )
    public void testCreateUserDuplicateEmail() {
        authService.loginExpectSuccess("admin@qa.in", "Admin@1234");

        CreateUserRequest user = userService.buildUniqueUserRequest();

        // First create — should succeed
        userService.createUserExpectSuccess(user);

        // Second create with same email — should conflict
        Response duplicate = userService.createUserAndStoreContext(user);

        ResponseValidator.of(duplicate)
                .statusCode(409)
                .hasField("error")
                .fieldContains("error", "already exists")
                .validate();
    }

    // ── TC03: GET non-existent user should return 404 ─────────────────────────
    @Test(
        description = "GET non-existent userId should return 404",
        groups      = { "api", "negative" }
    )
    public void testGetNonExistentUser() {
        authService.loginExpectSuccess("admin@qa.in", "Admin@1234");

        Response response = userService.getUserById("NON_EXISTENT_ID_999");

        ResponseValidator.of(response)
                .statusCode(404)
                .hasField("error")
                .responseTimeBelow(3000)
                .validate();
    }

    // ── TC04: API→API chain — create user, then create order for that user ────
    @Test(
        description = "API chain: authenticate → create user → create order → verify order linked to user",
        groups      = { "api", "regression" }
    )
    public void testApiChain_CreateUserThenOrder() {
        // Step 1: Auth
        authService.loginExpectSuccess("admin@qa.in", "Admin@1234");

        // Step 2: Create user — userId stored in ctx automatically
        UserResponse user = userService.createUserExpectSuccess(
                userService.buildUniqueUserRequest());

        // Step 3: Create order for that user
        //         OrderService reads userId from TestContext automatically
        Response orderResp = orderService.createOrder(java.util.Map.of(
                "productId", "PROD-001",
                "quantity",  2,
                "currency",  "INR"
        ));

        ResponseValidator.of(orderResp)
                .statusCode(201)
                .hasField("data.orderId")
                .fieldEquals("data.userId", user.getUserId())
                .fieldEquals("data.status", "PENDING")
                .validate();

        String orderId = ctx().getOrderId();

        // Step 4: GET the order and verify it's linked to the right user
        Response fetchedOrder = orderService.getOrder(orderId);

        ResponseValidator.of(fetchedOrder)
                .statusCode(200)
                .fieldEquals("data.orderId", orderId)
                .fieldEquals("data.userId", user.getUserId())
                .responseTimeBelow(2000)
                .validate();
    }

    // ── TC05: Unauthenticated access should return 401 ────────────────────────
    @Test(
        description = "API call without token should return 401 Unauthorized",
        groups      = { "api", "negative", "security" }
    )
    public void testUnauthenticatedRequest() {
        // Do NOT call authService.loginExpectSuccess — intentionally no token

        Response response = userService.getUserById("any-user-id");

        ResponseValidator.of(response)
                .statusCode(401)
                .hasField("error")
                .validate();
    }
}
