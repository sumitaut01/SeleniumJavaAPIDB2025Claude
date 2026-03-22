package com.framework.tests;

import com.framework.pages.LoginPage;

/**
 * Parent for hybrid tests that move between API and UI in a single test.
 *
 * Both WebDriver AND TestContext are fully initialised.
 *
 * Key bridge pattern:
 *   1. Use API services to set up data → stored in TestContext
 *   2. Use Page Objects to perform UI actions → read from TestContext
 *   3. Optionally use API services again to validate backend state
 *
 * Convenience helpers below eliminate boilerplate in test classes.
 *
 * Usage:
 *   public class CreateUserHybridTest extends BaseHybridTest {
 *       @Test
 *       public void testUserCreatedViaApiIsLoginableViaUI() {
 *           // Step 1: API — create user
 *           authService.loginExpectSuccess("admin@qa.in", "Admin@123");
 *           UserResponse user = userService.createUserExpectSuccess(
 *               userService.buildUniqueUserRequest());
 *
 *           // Step 2: UI — login as that new user
 *           loginAsContextUser();  // reads email from TestContext
 *
 *           // Step 3: Assert UI state
 *           assertThat(new DashboardPage().getWelcomeMessage())
 *               .contains(user.getFirstName());
 *       }
 *   }
 */
public abstract class BaseHybridTest extends BaseTest {

    @Override
    protected TestMode getMode() {
        return TestMode.HYBRID;
    }

    // ── Hybrid-specific helpers ───────────────────────────────────────────────

    /**
     * Logs in via UI using credentials stored in TestContext.
     * Call after createUserAndStoreContext() to bridge API→UI.
     */
    protected LoginPage loginAsContextUser() {
        String email    = ctx().getCreatedEmail();
        String password = "Test@1234";  // adjust if your service stores password

        log.info("Hybrid: UI login as context user → {}", email);
        LoginPage loginPage = new LoginPage();
        loginPage.openLoginPage().login(email, password);
        return loginPage;
    }

    /**
     * Logs in via UI using explicit credentials.
     */
    protected LoginPage loginViaUI(String email, String password) {
        log.info("Hybrid: UI login → {}", email);
        LoginPage loginPage = new LoginPage();
        loginPage.openLoginPage().login(email, password);
        return loginPage;
    }
}
