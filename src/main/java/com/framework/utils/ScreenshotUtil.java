package com.framework.utils;

import com.framework.constants.FrameworkConstants;
import com.framework.drivers.DriverManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Captures browser screenshots and saves them to the reports/screenshots/ folder.
 * Returns the absolute path so ExtentReports can embed it.
 */
public class ScreenshotUtil {

    private static final Logger log = LogManager.getLogger(ScreenshotUtil.class);

    private ScreenshotUtil() {}

    /**
     * Takes a screenshot for the current thread's WebDriver.
     *
     * @param testName used as the file name prefix
     * @return absolute path of the saved screenshot, or null on failure
     */
    public static String captureScreenshot(String testName) {
        try {
            WebDriver driver = DriverManager.getDriver();
            if (!(driver instanceof TakesScreenshot)) {
                log.warn("Current driver does not support screenshots.");
                return null;
            }

            byte[] screenshotBytes = ((TakesScreenshot) driver)
                    .getScreenshotAs(OutputType.BYTES);

            String timestamp = LocalDateTime.now()
                    .format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss_SSS"));
            String fileName  = sanitize(testName) + "_" + timestamp + ".png";

            Path screenshotDir = Paths.get(FrameworkConstants.SCREENSHOT_DIR);
            Files.createDirectories(screenshotDir);

            Path destination = screenshotDir.resolve(fileName);
            Files.write(destination, screenshotBytes);

            log.info("Screenshot saved: {}", destination.toAbsolutePath());
            return destination.toAbsolutePath().toString();

        } catch (Exception e) {
            log.error("Screenshot capture failed: {}", e.getMessage());
            return null;
        }
    }

    private static String sanitize(String name) {
        return name.replaceAll("[^a-zA-Z0-9_\\-]", "_");
    }
}
