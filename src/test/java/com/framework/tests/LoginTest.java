package com.framework.tests;

import com.framework.dataproviders.LoginDataProvider;
import com.framework.models.TestData;
import com.framework.pages.LoginPage;
import org.assertj.core.api.SoftAssertions;
import org.testng.annotations.Test;

/**
 * Sample Login tests demonstrating:
 *  - Data-driven execution (Excel + JSON)
 *  - Multi-country (data filtered by -Dcountry)
 *  - Soft assertions (AssertJ)
 *  - ExtentReport logging via TestListener (automatic — no code here)
 *  - Screenshot on failure (automatic via TestListener)
 */
public class LoginTest extends BaseTest {

    // ── TC01: Valid login — data driven from Excel ────────────────────────────
    @Test(
        dataProvider    = "loginDataExcel",
        dataProviderClass = LoginDataProvider.class,
        description     = "Verify successful login with valid credentials (Excel data)",
        groups          = { "smoke", "regression" }
    )
    public void testValidLogin_Excel(TestData data) {
        log.info("TC: {} | Country: {} | User: {}", data.getTestCaseId(), data.getCountry(), data.getUsername());

        LoginPage loginPage = new LoginPage();
        loginPage.openLoginPage()
                 .login(data.getUsername(), data.getPassword());

        SoftAssertions soft = new SoftAssertions();

        soft.assertThat(loginPage.isLogoutLinkVisible())
            .as("Logout link should be visible after successful login")
            .isTrue();

        if (data.getExpectedUrl() != null && !data.getExpectedUrl().isBlank()) {
            soft.assertThat(loginPage.getCurrentPageUrl())
                .as("Post-login URL should match expected")
                .contains(data.getExpectedUrl());
        }

        if (data.getExpectedTitle() != null && !data.getExpectedTitle().isBlank()) {
            soft.assertThat(loginPage.getTitle())
                .as("Page title should match")
                .containsIgnoringCase(data.getExpectedTitle());
        }

        soft.assertAll();
    }

    // ── TC02: Valid login — data driven from JSON ─────────────────────────────
    @Test(
        dataProvider      = "loginDataJson",
        dataProviderClass = LoginDataProvider.class,
        description       = "Verify successful login with valid credentials (JSON data)",
        groups            = { "smoke" }
    )
    public void testValidLogin_Json(TestData data) {
        log.info("TC: {} | Country: {} | User: {}", data.getTestCaseId(), data.getCountry(), data.getUsername());

        LoginPage loginPage = new LoginPage();
        loginPage.openLoginPage()
                 .login(data.getUsername(), data.getPassword());

        SoftAssertions soft = new SoftAssertions();

        soft.assertThat(loginPage.isLogoutLinkVisible())
            .as("Logout link should be visible after successful login")
            .isTrue();

        soft.assertAll();
    }

    // ── TC03: Invalid login — inline data (no DataProvider) ──────────────────
    @Test(
        description = "Verify error message for invalid credentials",
        groups      = { "negative", "regression" }
    )
    public void testInvalidLogin() {
        log.info("TC: Invalid login | Country: {}", country());

        LoginPage loginPage = new LoginPage();
        loginPage.openLoginPage()
                 .login("invalid_user@test.com", "wrongPassword");

        SoftAssertions soft = new SoftAssertions();

        soft.assertThat(loginPage.getErrorMessage())
            .as("Error message should be displayed for invalid credentials")
            .isNotBlank()
            .containsIgnoringCase("invalid");

        soft.assertThat(loginPage.isLogoutLinkVisible())
            .as("Logout link should NOT be visible after failed login")
            .isFalse();

        soft.assertAll();
    }

    // ── TC04: Empty credentials ───────────────────────────────────────────────
    @Test(
        description = "Verify validation message when credentials are empty",
        groups      = { "negative" }
    )
    public void testEmptyCredentials() {
        log.info("TC: Empty credentials | Country: {}", country());

        LoginPage loginPage = new LoginPage();
        loginPage.openLoginPage()
                 .login("", "");

        SoftAssertions soft = new SoftAssertions();

        soft.assertThat(loginPage.getErrorMessage())
            .as("Validation error should appear for empty fields")
            .isNotBlank();

        soft.assertAll();
    }
}
