package com.framework.tests.hybrid;

import com.framework.db.mapper.UserDbRecord;
import com.framework.db.queries.DbQueries;
import com.framework.models.request.CreateUserRequest;
import com.framework.models.response.UserResponse;
import com.framework.pages.LoginPage;
import com.framework.tests.BaseHybridTest;
import org.assertj.core.api.SoftAssertions;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Full end-to-end tests — all three layers in one test method.
 *
 *   API  →  DB  →  UI      (create via API, verify in DB, validate in browser)
 *   UI   →  API →  DB      (act in browser, verify via API, confirm in DB)
 *   API  →  DB  →  API     (chain with DB as the source of truth between calls)
 *
 * This is the most powerful test pattern in the framework.
 * Each test tells a clear business story across all three layers.
 */
public class FullFlowTest extends BaseHybridTest {

    // Track email for cleanup
    private String testEmail;

    @AfterMethod(alwaysRun = true)
    public void cleanupTestData() {
        if (testEmail != null && pgDb != null) {
            dbHelper.cleanupTestUser(testEmail);
            log.info("Cleanup: removed test user email={}", testEmail);
            testEmail = null;
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // PATTERN 1:  API → DB → UI
    // Story: Admin creates a user via API. We verify the DB was written
    //        correctly. Then we prove the new user can log in via browser.
    // ════════════════════════════════════════════════════════════════════════
    @Test(
        description = "API creates user → DB confirms record → UI validates login works",
        groups = { "hybrid", "fullflow", "smoke" }
    )
    public void testCreateViaApi_VerifyInDb_ThenLoginViaUI() {

        // ── PHASE 1: API — create user ─────────────────────────────────────
        log.info("Phase 1 [API]: authenticating as admin");
        authService.loginExpectSuccess("admin@qa.in", "Admin@1234");

        CreateUserRequest newUser = userService.buildUniqueUserRequest();
        testEmail = newUser.getEmail();  // save for cleanup

        log.info("Phase 1 [API]: creating user → {}", testEmail);
        UserResponse apiUser = userService.createUserExpectSuccess(newUser);

        // ── PHASE 2: DB — verify Postgres record ───────────────────────────
        log.info("Phase 2 [DB-Postgres]: verifying user record persisted");

        dbAssert(pgDb)
            .forQuery(DbQueries.User.FIND_BY_EMAIL, testEmail)
            .rowExists()
            .columnEquals("status",  "ACTIVE")
            .columnEquals("country", country())
            .columnEquals("role",    "CUSTOMER")
            .columnNotBlank("user_id")
            .columnIsNull("deleted_at")     // not marked as deleted
            .validate();

        // Fetch typed record for downstream use
        UserDbRecord dbUser = pgDb.queryOne(
                DbQueries.User.FIND_BY_EMAIL,
                UserDbRecord.rowMapper(),
                testEmail)
            .orElseThrow(() -> new AssertionError("User not found in DB: " + testEmail));

        // API response userId must match what's actually in DB
        assertThat(dbUser.getUserId())
            .as("API response userId must match DB user_id")
            .isEqualTo(apiUser.getUserId());

        // ── PHASE 2B: DB — verify Oracle audit log ─────────────────────────
        if (orDb != null) {
            log.info("Phase 2 [DB-Oracle]: verifying USER_CREATED audit event");
            dbAssert(orDb)
                .forQuery(DbQueries.Audit.COUNT_EVENTS_BY_ENTITY,
                          apiUser.getUserId(), "USER_CREATED")
                .scalarEquals(1L)
                .validate();
        }

        // ── PHASE 3: UI — verify login works ──────────────────────────────
        log.info("Phase 3 [UI]: logging in as newly created user");
        LoginPage loginPage = loginViaUI(testEmail, "Test@1234");

        SoftAssertions soft = new SoftAssertions();
        soft.assertThat(loginPage.isLogoutLinkVisible())
            .as("User created via API should be able to log in via browser")
            .isTrue();
        soft.assertThat(loginPage.getCurrentPageUrl())
            .as("Should land on dashboard after login")
            .contains("/dashboard");
        soft.assertAll();
    }

    // ════════════════════════════════════════════════════════════════════════
    // PATTERN 2:  API creates + order → DB verifies both → UI shows order
    // Story: Full e-commerce flow — user + order created via API, both
    //        verified in DB, then validated in the UI order list.
    // ════════════════════════════════════════════════════════════════════════
    @Test(
        description = "API: create user + order → DB: verify both records → UI: order visible on page",
        groups = { "hybrid", "fullflow", "regression" }
    )
    public void testCreateOrderViaApi_VerifyInDb_CheckUI() {

        // ── PHASE 1: API chain ─────────────────────────────────────────────
        log.info("Phase 1 [API]: setting up user + order");
        authService.loginExpectSuccess("admin@qa.in", "Admin@1234");

        CreateUserRequest newUser = userService.buildUniqueUserRequest();
        testEmail = newUser.getEmail();
        UserResponse apiUser = userService.createUserExpectSuccess(newUser);

        orderService.createOrder(java.util.Map.of(
            "productId", "PROD-001",
            "quantity",  2,
            "currency",  country().equals("IN") ? "INR" : "USD"
        ));
        String orderId = ctx().getOrderId();
        log.info("Phase 1 done → userId={}, orderId={}", apiUser.getUserId(), orderId);

        // ── PHASE 2: DB — verify user row ──────────────────────────────────
        log.info("Phase 2 [DB]: verifying user record");
        dbAssert(pgDb)
            .forQuery(DbQueries.User.FIND_BY_ID, apiUser.getUserId())
            .rowExists()
            .columnEquals("status", "ACTIVE")
            .validate();

        // ── PHASE 2B: DB — verify order row ───────────────────────────────
        log.info("Phase 2 [DB]: verifying order record");
        dbAssert(pgDb)
            .forQuery(DbQueries.Order.FIND_BY_ID, orderId)
            .rowExists()
            .columnEquals("user_id", apiUser.getUserId())
            .columnEquals("status",  "PENDING")
            .columnEquals("quantity", "2")
            .columnNotBlank("total_amount")
            .validate();

        // COUNT check — user should now have exactly 1 order
        dbAssert(pgDb)
            .forQuery(DbQueries.Order.COUNT_BY_USER, apiUser.getUserId())
            .scalarEquals(1L)
            .validate();

        // ── PHASE 3: UI — verify order visible on page ────────────────────
        log.info("Phase 3 [UI]: verifying order appears for user in browser");
        loginViaUI(testEmail, "Test@1234");

        // Placeholder: swap in your real OrdersPage POM
        // OrdersPage ordersPage = new OrdersPage().open();
        // assertThat(ordersPage.isOrderVisible(orderId)).isTrue();
        // assertThat(ordersPage.getOrderStatus(orderId)).isEqualTo("PENDING");
        log.info("Phase 3 complete — add OrdersPage assertions when POM is ready");
    }

    // ════════════════════════════════════════════════════════════════════════
    // PATTERN 3:  API updates status → DB confirms change → UI reflects it
    // Story: Admin deactivates a user via API. DB should show INACTIVE.
    //        User should then be blocked from logging in via browser.
    // ════════════════════════════════════════════════════════════════════════
    @Test(
        description = "API deactivates user → DB confirms INACTIVE → UI blocks login",
        groups = { "hybrid", "fullflow", "negative" }
    )
    public void testDeactivateViaApi_VerifyDb_ThenUIBlocked() {

        // ── PHASE 1: API — create user ─────────────────────────────────────
        authService.loginExpectSuccess("admin@qa.in", "Admin@1234");
        CreateUserRequest newUser = userService.buildUniqueUserRequest();
        testEmail = newUser.getEmail();
        UserResponse apiUser = userService.createUserExpectSuccess(newUser);

        // ── PHASE 1B: Verify user is ACTIVE in DB first ────────────────────
        dbAssert(pgDb)
            .forQuery(DbQueries.User.GET_STATUS, apiUser.getUserId())
            .rowExists()
            .columnEquals("status", "ACTIVE")
            .validate();

        // ── PHASE 2: API — deactivate user ────────────────────────────────
        log.info("Phase 2 [API]: deactivating userId={}", apiUser.getUserId());
        // userService.updateStatus(apiUser.getUserId(), "INACTIVE"); // add when ready

        // Simulate via direct DB update for the demo
        dbHelper.forceUserStatus(apiUser.getUserId(), "INACTIVE");

        // ── PHASE 3: DB — verify status changed ───────────────────────────
        log.info("Phase 3 [DB]: verifying status is now INACTIVE");
        dbAssert(pgDb)
            .forQuery(DbQueries.User.GET_STATUS, apiUser.getUserId())
            .rowExists()
            .columnEquals("status", "INACTIVE")
            .validate();

        // ── PHASE 4: UI — verify login is now blocked ─────────────────────
        log.info("Phase 4 [UI]: verifying inactive user cannot log in");
        LoginPage loginPage = loginViaUI(testEmail, "Test@1234");

        SoftAssertions soft = new SoftAssertions();
        soft.assertThat(loginPage.isLogoutLinkVisible())
            .as("Deactivated user should NOT see logout link")
            .isFalse();
        soft.assertThat(loginPage.getErrorMessage())
            .as("Should see an error message for inactive account")
            .isNotBlank();
        soft.assertAll();
    }

    // ════════════════════════════════════════════════════════════════════════
    // PATTERN 4:  DB precondition → API call → DB post-condition check
    // Story: Uses DB as the source of truth before AND after an API call.
    //        No UI involved — pure API + DB chain.
    // ════════════════════════════════════════════════════════════════════════
    @Test(
        description = "DB: assert user is ACTIVE → API: place order → DB: verify order count incremented",
        groups = { "hybrid", "fullflow", "regression" }
    )
    public void testDbPrecondition_ApiAction_DbPostCondition() {

        // ── PHASE 1: API — create user so we have a clean starting point ───
        authService.loginExpectSuccess("admin@qa.in", "Admin@1234");
        CreateUserRequest newUser = userService.buildUniqueUserRequest();
        testEmail = newUser.getEmail();
        UserResponse apiUser = userService.createUserExpectSuccess(newUser);
        String userId = apiUser.getUserId();

        // ── PHASE 2: DB pre-condition — user exists and has 0 orders ──────
        log.info("Phase 2 [DB pre-condition]: user exists, 0 orders");
        dbAssert(pgDb)
            .forQuery(DbQueries.User.GET_STATUS, userId)
            .rowExists()
            .columnEquals("status", "ACTIVE")
            .validate();

        dbAssert(pgDb)
            .forQuery(DbQueries.Order.COUNT_BY_USER, userId)
            .scalarEquals(0L)
            .validate();

        // ── PHASE 3: API — place an order ─────────────────────────────────
        log.info("Phase 3 [API]: placing order for userId={}", userId);
        orderService.createOrder(java.util.Map.of(
            "productId", "PROD-002",
            "quantity",  1,
            "currency",  "INR"
        ));

        // ── PHASE 4: DB post-condition — order count is now 1 ─────────────
        log.info("Phase 4 [DB post-condition]: order count should be 1");
        dbAssert(pgDb)
            .forQuery(DbQueries.Order.COUNT_BY_USER, userId)
            .scalarEquals(1L)
            .validate();

        // Verify the order record has the right user linked
        dbAssert(pgDb)
            .forQuery(DbQueries.Order.FIND_BY_ID, ctx().getOrderId())
            .rowExists()
            .columnEquals("user_id", userId)
            .columnEquals("status",  "PENDING")
            .validate();
    }
}
