package com.framework.pages;

import com.framework.config.ConfigManager;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

/**
 * Page Object for the Login page.
 *
 * Locators use @FindBy (PageFactory).
 * For dynamic locators, use By.* directly.
 *
 * Replace selector values with your actual application's selectors.
 */
public class LoginPage extends BasePage {

    // ── Locators ──────────────────────────────────────────────────────────────
    @FindBy(id = "username")
    private WebElement usernameField;

    @FindBy(id = "password")
    private WebElement passwordField;

    @FindBy(id = "loginBtn")
    private WebElement loginButton;

    private final By errorMessage   = By.cssSelector(".error-message");
    private final By successMessage = By.cssSelector(".success-banner");
    private final By logoutLink     = By.linkText("Logout");

    // ── Constructor ───────────────────────────────────────────────────────────
    public LoginPage() {
        super();  // triggers PageFactory.initElements
    }

    // ── Actions ───────────────────────────────────────────────────────────────

    /** Navigate to the app's login page (base URL comes from ConfigManager). */
    public LoginPage openLoginPage() {
        String url = ConfigManager.getInstance().getBaseUrl() + "/login";
        navigateTo(url);
        log.info("Login page opened for country: {}",
                ConfigManager.getInstance().getCountry());
        return this;
    }

    public LoginPage enterUsername(String username) {
        log.info("Entering username: {}", username);
        usernameField.clear();
        usernameField.sendKeys(username);
        return this;
    }

    public LoginPage enterPassword(String password) {
        log.info("Entering password: [hidden]");
        passwordField.clear();
        passwordField.sendKeys(password);
        return this;
    }

    public void clickLogin() {
        log.info("Clicking Login button");
        click(loginButton);
    }

    /** Fluent one-shot login. */
    public void login(String username, String password) {
        enterUsername(username)
                .enterPassword(password)
                .clickLogin();
    }

    // ── Verifications (return raw values — assertions live in tests) ──────────

    public String getErrorMessage() {
        return getText(errorMessage);
    }

    public String getSuccessMessage() {
        return getText(successMessage);
    }

    public boolean isLogoutLinkVisible() {
        return isDisplayed(logoutLink);
    }

    public String getTitle() {
        return getPageTitle();
    }

    public String getCurrentPageUrl() {
        return getCurrentUrl();
    }
}
