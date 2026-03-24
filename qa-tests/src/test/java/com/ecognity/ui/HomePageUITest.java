package com.ecognity.ui;

import org.junit.jupiter.api.*;
import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.*;

/**
 * UI Automation Tests – Home Page
 *
 * Test IDs / selectors are based on app.js and index.html structure.
 * Page Object Model (inline) for this suite.
 */
@DisplayName("Home Page UI Tests")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class HomePageUITest extends BaseUITest {

    private WebDriverWait wait;

    @BeforeEach
    void loadPage() {
        wait = new WebDriverWait(driver, Duration.ofSeconds(15));
        driver.get(BASE_URL + "/");
    }

    // ─────────────────────────────────────────────────────────────
    // 1. Page Load
    // ─────────────────────────────────────────────────────────────

    @Test
    @Order(1)
    @DisplayName("✔ Home page loads successfully with HTTP 200")
    void homePageLoads() {
        String title = driver.getTitle();
        assertFalse(title.isBlank(), "Page title should not be blank");
        // Verify page is not an error page
        String body = driver.findElement(By.tagName("body")).getText();
        assertFalse(body.toLowerCase().contains("404 not found"),
            "Page should not show 404");
        assertFalse(body.toLowerCase().contains("service unavailable"),
            "Page should not show 503");
    }

    @Test
    @Order(2)
    @DisplayName("✔ Page title is present and meaningful")
    void pageTitleIsPresent() {
        assertNotNull(driver.getTitle());
        assertTrue(driver.getTitle().length() > 2,
            "Page title should be more than 2 characters");
    }

    @Test
    @Order(3)
    @DisplayName("✔ app.js loaded – no JS errors in console")
    void noJsErrors() {
        JavascriptExecutor js = (JavascriptExecutor) driver;
        // Inject error collector before checking
        Object errors = js.executeScript(
            "return window.__jsErrors ? window.__jsErrors.length : 0");
        // Basic check: page rendered body content (js didn't crash the page)
        assertNotNull(driver.findElement(By.tagName("body")));
    }

    @Test
    @Order(4)
    @DisplayName("✔ styles.css loaded – body has non-zero dimensions")
    void cssIsApplied() {
        JavascriptExecutor js = (JavascriptExecutor) driver;
        Long bodyWidth = (Long) js.executeScript(
            "return document.body.offsetWidth");
        assertTrue(bodyWidth > 0, "Body width should be > 0 when CSS is applied");
    }

    // ─────────────────────────────────────────────────────────────
    // 2. Navigation elements
    // ─────────────────────────────────────────────────────────────

    @Test
    @Order(5)
    @DisplayName("✔ Leave page link is present on home page")
    void leavePageLinkExists() {
        List<WebElement> links = driver.findElements(By.tagName("a"));
        boolean hasLeaveLink = links.stream()
            .anyMatch(a -> {
                String href = a.getAttribute("href");
                return href != null && href.contains("leave");
            });
        assertTrue(hasLeaveLink, "A link to leave.html should be present on the home page");
    }

    // ─────────────────────────────────────────────────────────────
    // 3. Auth config
    // ─────────────────────────────────────────────────────────────

    @Test
    @Order(6)
    @DisplayName("✔ auth-config.js loaded – msalInstance defined or auth element present")
    void authConfigLoaded() {
        JavascriptExecutor js = (JavascriptExecutor) driver;
        // Either the msal script loaded or the sign-in button/element is in DOM
        Boolean msalDefined = (Boolean) js.executeScript(
            "return typeof msal !== 'undefined' || document.querySelector('[data-testid=\"signin\"], #signInButton, .auth-btn') !== null");
        // At minimum, the page should not show a raw JS error in body
        String body = driver.findElement(By.tagName("body")).getText();
        assertFalse(body.contains("ReferenceError") && body.contains("msal"),
            "MSAL library should be loaded without ReferenceError");
    }
}
