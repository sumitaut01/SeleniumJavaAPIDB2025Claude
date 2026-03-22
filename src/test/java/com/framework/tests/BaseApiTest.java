package com.framework.tests;

/**
 * Parent for pure API tests — no WebDriver, no browser, no Selenium overhead.
 *
 * Extends BaseTest with mode = API so setUp() skips DriverManager.initDriver().
 *
 * All API services (authService, userService, orderService) and TestContext
 * are ready for use via BaseTest.
 *
 * Usage:
 *   public class UserApiTest extends BaseApiTest {
 *       @Test
 *       public void testCreateUser() {
 *           authService.loginExpectSuccess("admin@qa.in", "pass");
 *           UserResponse user = userService.createUserExpectSuccess(
 *               userService.buildUniqueUserRequest());
 *           assertThat(user.getStatus()).isEqualTo("ACTIVE");
 *       }
 *   }
 */
public abstract class BaseApiTest extends BaseTest {

    @Override
    protected TestMode getMode() {
        return TestMode.API;
    }
}
