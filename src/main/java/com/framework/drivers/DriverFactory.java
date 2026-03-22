package com.framework.drivers;

import com.framework.config.ConfigManager;
import com.framework.constants.FrameworkConstants;
import com.framework.enums.Browser;
import com.framework.enums.ExecutionType;
import io.github.bonigarcia.wdm.WebDriverManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.edge.EdgeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.remote.RemoteWebDriver;

import java.net.MalformedURLException;
import java.net.URL;
import java.time.Duration;

/**
 * Creates WebDriver instances.
 * Supports LOCAL (WebDriverManager auto-download) and
 *           REMOTE (Selenium Grid / BrowserStack / SauceLabs).
 *
 * Never call this directly — always go through DriverManager.
 */
public class DriverFactory {

    private static final Logger log = LogManager.getLogger(DriverFactory.class);

    private DriverFactory() { /* static factory */ }

    public static WebDriver createDriver() {
        ConfigManager cfg      = ConfigManager.getInstance();
        Browser       browser  = cfg.getBrowser();
        ExecutionType execType = cfg.getExecutionType();

        log.info("Creating {} driver | execution={}", browser, execType);

        WebDriver driver = (execType == ExecutionType.REMOTE)
                ? createRemoteDriver(browser, cfg.getGridUrl())
                : createLocalDriver(browser);

        setTimeouts(driver);
        return driver;
    }

    // ── Local drivers ────────────────────────────────────────────────────────
    private static WebDriver createLocalDriver(Browser browser) {
        return switch (browser) {
            case CHROME  -> {
                WebDriverManager.chromedriver().setup();
                ChromeOptions opts = new ChromeOptions();
                opts.addArguments("--start-maximized", "--disable-notifications",
                        "--disable-popup-blocking", "--no-sandbox",
                        "--disable-dev-shm-usage");
                yield new ChromeDriver(opts);
            }
            case FIREFOX -> {
                WebDriverManager.firefoxdriver().setup();
                FirefoxOptions opts = new FirefoxOptions();
                opts.addArguments("--start-maximized");
                yield new FirefoxDriver(opts);
            }
            case EDGE    -> {
                WebDriverManager.edgedriver().setup();
                EdgeOptions opts = new EdgeOptions();
                opts.addArguments("--start-maximized");
                yield new EdgeDriver(opts);
            }
        };
    }

    // ── Remote / Grid driver ─────────────────────────────────────────────────
    private static WebDriver createRemoteDriver(Browser browser, String gridUrl) {
        try {
            return switch (browser) {
                case CHROME  -> new RemoteWebDriver(new URL(gridUrl), new ChromeOptions());
                case FIREFOX -> new RemoteWebDriver(new URL(gridUrl), new FirefoxOptions());
                case EDGE    -> new RemoteWebDriver(new URL(gridUrl), new EdgeOptions());
            };
        } catch (MalformedURLException e) {
            throw new RuntimeException("Invalid Selenium Grid URL: " + gridUrl, e);
        }
    }

    // ── Timeouts ─────────────────────────────────────────────────────────────
    private static void setTimeouts(WebDriver driver) {
        driver.manage().timeouts()
                .implicitlyWait(Duration.ofSeconds(FrameworkConstants.IMPLICIT_WAIT))
                .pageLoadTimeout(Duration.ofSeconds(FrameworkConstants.PAGE_LOAD_TIMEOUT))
                .scriptTimeout(Duration.ofSeconds(FrameworkConstants.SCRIPT_TIMEOUT));
    }
}
