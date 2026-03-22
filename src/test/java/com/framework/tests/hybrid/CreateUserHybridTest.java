package com.framework.tests.hybrid;

import com.framework.models.request.CreateUserRequest;
import com.framework.models.response.UserResponse;
import com.framework.pages.LoginPage;
import com.framework.tests.BaseHybridTest;
import org.assertj.core.api.SoftAssertions;
import org.testng.annotations.Test;

/**
 * Hybrid Tests — API and UI in a single test method.
 *
 * Pattern A  (API → UI):
 *   Generate / set up test data via API, then validate via browser.
 *   Use case: "Can a user created via admin API actually log in through the browser?"
 *
 * Pattern B  (UI → API):
 *   Perform an action through the browser, then validate the backend state via API.
 *   Use case: "After the user registers via UI, does the API reflect them as ACTIVE?"
 *
 * TestContext is the bridge — API services write to it, UI helpers read from it.
 */
public class CreateUserHybridTest extends BaseHybridTest {

    // ── Pattern A: API creates user → UI validates login works ───────────────
    @Test(
        description = "API: create user | UI: verify that user can log in via browser",
        groups      = { "hybrid", "smoke" }
    )
    public void testApiCreatedUserCanLoginViaUI() {
        // ── PHASE 1: API — set up data ────────────────────────────────────────
        log.info("Phase 1 (API): authenticating as admin");
        authService.loginExpectSuccess("admin@qa.in", "Admin@1234");

        log.info("Phase 1 (API): creating new user");
        CreateUserRequest newUser = userService.buildUniqueUserRequest();
        UserResponse created = userService.createUserExpectSuccess(newUser);

        // At this point ctx() holds:
        //   authToken     = admin token
        //   userId        = created.getUserId()
        //   createdEmail  = newUser.getEmail()
        log.info("Phase 1 done → userId={}, email={}", ctx().getUserId(), ctx().getCreatedEmail());

        // ── PHASE 2: UI — validate using browser ──────────────────────────────
        log.info("Phase 2 (UI): logging in as newly created user");
        LoginPage loginPage = loginViaUI(ctx().getCreatedEmail(), "Test@1234");

        SoftAssertions soft = new SoftAssertions();

        soft.assertThat(loginPage.isLogoutLinkVisible())
            .as("Logout link should be visible — user created via API should be able to log in")
            .isTrue();

        soft.assertThat(loginPage.getCurrentPageUrl())
            .as("Should land on dashboard after login")
            .contains("/dashboard");

        soft.assertAll();
    }

    // ── Pattern A (variant): API sets up order → UI validates order page ──────
    @Test(
        description = "API: create user + order | UI: verify order appears on user's order list page",
        groups      = { "hybrid", "regression" }
    )
    public void testApiOrderAppearsInUI() {
        // ── PHASE 1: API chain ─────────────────────────────────────────────────
        log.info("Phase 1 (API): setting up user + order");
        authService.loginExpectSuccess("admin@qa.in", "Admin@1234");

        userService.createUserExpectSuccess(userService.buildUniqueUserRequest());

        orderService.createOrder(java.util.Map.of(
                "productId", "PROD-001",
                "quantity",  1,
                "currency",  country().equals("IN") ? "INR" : "USD"
        ));

        String orderId = ctx().getOrderId();
        log.info("Phase 1 done → orderId={}", orderId);

        // ── PHASE 2: UI ────────────────────────────────────────────────────────
        log.info("Phase 2 (UI): navigating to orders page");
        loginViaUI(ctx().getCreatedEmail(), "Test@1234");

        // Navigate to orders page (adjust to your actual page object)
        // OrdersPage ordersPage = new OrdersPage().open();
        // assertThat(ordersPage.isOrderVisible(orderId)).isTrue();
        //
        // NOTE: OrdersPage is a placeholder — add it just like LoginPage extends BasePage

        log.info("Phase 2 complete — add OrdersPage assertions here");
    }

    // ── Pattern B: UI registers user → API validates backend state ────────────
    @Test(
        description = "UI: register new user | API: verify user is ACTIVE in backend",
        groups      = { "hybrid", "regression" }
    )
    public void testUIRegistrationReflectedInAPI() {
        // ── PHASE 1: UI — register via browser ────────────────────────────────
        log.info("Phase 1 (UI): registering new user via browser");

        // Generate unique test data for the registration form
        String uid      = java.util.UUID.randomUUID().toString().substring(0, 8);
        String email    = "ui_reg_" + uid + "@auto." + country().toLowerCase() + ".com";
        String password = "Test@1234";

        // RegistrationPage regPage = new RegistrationPage().open();
        // regPage.register(firstName, lastName, email, password);
        // assertThat(regPage.getSuccessMessage()).contains("Registration successful");
        //
        // For now, store what UI would have created:
        ctx().store("uiRegisteredEmail", email);
        log.info("Phase 1 done → email={} (UI registration placeholder)", email);

        // ── PHASE 2: API — validate backend state ─────────────────────────────
        log.info("Phase 2 (API): verifying user exists and is ACTIVE in backend");
        authService.loginExpectSuccess("admin@qa.in", "Admin@1234");

        // Search by email via API
        // Response searchResp = userService.searchByEmail(email);
        // ResponseValidator.of(searchResp)
        //     .statusCode(200)
        //     .fieldEquals("data[0].email", email)
        //     .fieldEquals("data[0].status", "ACTIVE")
        //     .validate();

        log.info("Phase 2 complete — add search-by-email API assertion here");
    }

    // ── Pattern B (variant): UI updates profile → API confirms changes ────────
    @Test(
        description = "UI: update user phone | API: verify phone change persisted in backend",
        groups      = { "hybrid", "regression" }
    )
    public void testUIProfileUpdateReflectedInAPI() {
        // ── PHASE 1: API setup — create user so we have a known starting state ─
        authService.loginExpectSuccess("admin@qa.in", "Admin@1234");
        UserResponse user = userService.createUserExpectSuccess(
                userService.buildUniqueUserRequest());

        // ── PHASE 2: UI — login and update phone number ────────────────────────
        log.info("Phase 2 (UI): updating phone for userId={}", user.getUserId());
        loginViaUI(ctx().getCreatedEmail(), "Test@1234");

        String newPhone = config().getCountry().getDialCode() + "8888888888";
        ctx().store("expectedPhone", newPhone);

        // ProfilePage profilePage = new ProfilePage().open();
        // profilePage.updatePhone(newPhone);
        // assertThat(profilePage.getSuccessToast()).contains("Profile updated");

        log.info("Phase 2 done (UI update placeholder)");

        // ── PHASE 3: API — re-auth as admin and verify phone changed ──────────
        log.info("Phase 3 (API): verifying phone update persisted");
        authService.loginExpectSuccess("admin@qa.in", "Admin@1234");

        UserResponse updated = userService.getUserExpectSuccess(user.getUserId());

        // assertThat(updated.getPhone()).isEqualTo(newPhone);
        log.info("Phase 3 complete — fetched phone={} (add assertion when ProfilePage exists)",
                updated.getPhone());
    }
}
