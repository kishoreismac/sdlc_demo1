package com.ecognity.ui;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

import java.time.Duration;

/**
 * Base class for all Selenium UI tests.
 * Sets up a headless Chrome driver with common timeouts.
 */
public abstract class BaseUITest {

    protected static final String BASE_URL =
        System.getProperty("app.base.url", "https://ec-user-home-page.azurewebsites.net");

    protected static final Duration IMPLICIT_WAIT  = Duration.ofSeconds(10);
    protected static final Duration PAGE_LOAD_WAIT = Duration.ofSeconds(30);
    protected static final Duration SCRIPT_WAIT    = Duration.ofSeconds(10);
    protected static final boolean HEADLESS =
        Boolean.parseBoolean(System.getProperty("ui.headless", "true"));
    protected static final long VISUAL_DELAY_MS =
        Long.parseLong(System.getProperty("ui.visual.delay.ms", "0"));

    protected WebDriver driver;

    @BeforeEach
    void initDriver() {
        WebDriverManager.chromedriver().setup();

        ChromeOptions options = new ChromeOptions();
        if (HEADLESS) {
            options.addArguments("--headless=new");
        }
        options.addArguments(
            "--no-sandbox",
            "--disable-dev-shm-usage",
            "--disable-gpu",
            "--window-size=1920,1080",
            "--disable-extensions",
            "--disable-popup-blocking",
            "--ignore-certificate-errors"
        );

        driver = new ChromeDriver(options);
        driver.manage().timeouts().implicitlyWait(IMPLICIT_WAIT);
        driver.manage().timeouts().pageLoadTimeout(PAGE_LOAD_WAIT);
        driver.manage().timeouts().scriptTimeout(SCRIPT_WAIT);

        pauseForVisualMode();
    }

    @AfterEach
    void tearDown() {
        if (driver != null) {
            pauseForVisualMode();
            driver.quit();
        }
    }

    protected void pauseForVisualMode() {
        if (VISUAL_DELAY_MS <= 0) {
            return;
        }

        try {
            Thread.sleep(VISUAL_DELAY_MS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
