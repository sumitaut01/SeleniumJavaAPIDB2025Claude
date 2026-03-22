package com.framework.pages;

import com.framework.drivers.DriverManager;
import com.framework.utils.WaitUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.PageFactory;

/**
 * All Page Objects extend BasePage.
 * Provides:
 *  - driver access via DriverManager (thread-safe)
 *  - PageFactory init
 *  - common interaction helpers (click, type, getText, etc.)
 */
public abstract class BasePage {

    protected final Logger log = LogManager.getLogger(getClass());

    protected BasePage() {
        PageFactory.initElements(DriverManager.getDriver(), this);
    }

    protected WebDriver driver() {
        return DriverManager.getDriver();
    }

    // ── Navigation ────────────────────────────────────────────────────────────
    protected void navigateTo(String url) {
        log.info("Navigating to: {}", url);
        driver().get(url);
    }

    protected String getCurrentUrl() {
        return driver().getCurrentUrl();
    }

    protected String getPageTitle() {
        return driver().getTitle();
    }

    // ── Interactions ──────────────────────────────────────────────────────────
    protected void click(By locator) {
        log.debug("Click: {}", locator);
        WaitUtil.waitForClickable(locator).click();
    }

    protected void click(WebElement element) {
        log.debug("Click element");
        WaitUtil.waitForClickable(element).click();
    }

    protected void type(By locator, String text) {
        log.debug("Type '{}' into: {}", text, locator);
        WebElement el = WaitUtil.waitForVisible(locator);
        el.clear();
        el.sendKeys(text);
    }

    protected String getText(By locator) {
        return WaitUtil.waitForVisible(locator).getText().trim();
    }

    protected boolean isDisplayed(By locator) {
        try {
            return WaitUtil.waitForVisible(locator).isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }

    protected void jsClick(WebElement element) {
        log.debug("JS click on element");
        ((JavascriptExecutor) driver()).executeScript("arguments[0].click();", element);
    }

    protected void scrollIntoView(WebElement element) {
        ((JavascriptExecutor) driver())
                .executeScript("arguments[0].scrollIntoView(true);", element);
    }
}
