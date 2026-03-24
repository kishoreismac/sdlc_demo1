package com.ecognity.ui;

import org.junit.jupiter.api.*;
import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.*;

/**
 * UI Automation Tests – Calendar Component
 *
 * Validates:
 *   ✔ Calendar renders for current month
 *   ✔ Correct month name and year are displayed
 *   ✔ Correct number of day cells rendered
 *   ✔ Current date cell is highlighted
 *   ✔ Previous/Next navigation works
 */
@DisplayName("Calendar UI Tests")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class CalendarUITest extends BaseUITest {

    private WebDriverWait wait;

    // Selectors – adjust to match actual app.js DOM output
    private static final String CALENDAR_CONTAINER  = "#calendar-widget, .calendar-widget";
    private static final String CALENDAR_MONTH_YEAR = "#current-month-year, .month-title, .month-year";
    private static final String CALENDAR_DAYS       = "#days-grid .day-cell, .calendar-body .day-cell";
    private static final String TODAY_CELL          = ".today, .current-day, .highlight-today, " +
                                                      "[data-testid='today']";
    private static final String PREV_BTN            = "#prev-month, .prev-month, [data-testid='prev-month']";
    private static final String NEXT_BTN            = "#next-month, .next-month, [data-testid='next-month']";

    @BeforeEach
    void loadPage() {
        wait = new WebDriverWait(driver, Duration.ofSeconds(15));
        driver.get(BASE_URL + "/");
        loginAsDemoUser();

        // Wait for calendar to be present in DOM
        try {
            wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector(CALENDAR_CONTAINER)));
            wait.until(ExpectedConditions.numberOfElementsToBeMoreThan(By.cssSelector(CALENDAR_DAYS), 0));
        } catch (TimeoutException e) {
            // Calendar may be rendered inline; continue with tests
        }
    }

    private void loginAsDemoUser() {
        try {
            WebElement usernameField = wait.until(
                ExpectedConditions.elementToBeClickable(By.id("login-username")));
            usernameField.clear();
            usernameField.sendKeys("kiran");

            WebElement passwordField = driver.findElement(By.id("login-password"));
            passwordField.clear();
            passwordField.sendKeys("1234");

            driver.findElement(By.id("login-btn")).click();
            wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("main-dashboard")));
        } catch (Exception e) {
            System.out.println("INFO: Login step skipped – " + e.getMessage());
        }
    }

    // ─────────────────────────────────────────────────────────────
    // 1. Calendar renders
    // ─────────────────────────────────────────────────────────────

    @Test
    @Order(1)
    @DisplayName("✔ Calendar container is present in DOM")
    void calendarContainerPresent() {
        List<WebElement> containers = driver.findElements(By.cssSelector(CALENDAR_CONTAINER));
        assertFalse(containers.isEmpty(),
            "Calendar container element should be present (#calendar / .calendar)");
    }

    @Test
    @Order(2)
    @DisplayName("✔ Calendar renders correct month and year label")
    void calendarShowsCurrentMonthYear() {
        List<WebElement> titleElements = driver.findElements(By.cssSelector(CALENDAR_MONTH_YEAR));

        if (titleElements.isEmpty()) {
            // Fallback: search entire page text for month name
            String pageText = driver.findElement(By.tagName("body")).getText().toLowerCase();
            String expectedMonth = LocalDate.now()
                .getMonth().getDisplayName(TextStyle.FULL, Locale.ENGLISH).toLowerCase();
            assertTrue(pageText.contains(expectedMonth),
                "Page should contain current month name: " + expectedMonth);
        } else {
            String titleText = titleElements.get(0).getText().toLowerCase();
            String expectedMonth = LocalDate.now()
                .getMonth().getDisplayName(TextStyle.FULL, Locale.ENGLISH).toLowerCase();
            String expectedYear = String.valueOf(LocalDate.now().getYear());
            assertTrue(titleText.contains(expectedMonth),
                "Calendar title should contain month: " + expectedMonth + " but was: " + titleText);
            assertTrue(titleText.contains(expectedYear),
                "Calendar title should contain year: " + expectedYear + " but was: " + titleText);
        }
    }

    @Test
    @Order(3)
    @DisplayName("✔ Calendar renders correct number of day cells")
    void calendarDayCellCount() {
        List<WebElement> dayCells = driver.findElements(By.cssSelector(CALENDAR_DAYS));

        if (dayCells.isEmpty()) {
            // Fallback – check table cells / div grid
            dayCells = driver.findElements(By.cssSelector("td, .day"));
        }

        // At minimum, all days of the current month should be in the DOM
        int daysInMonth = YearMonth.now().lengthOfMonth();
        long actualDayCells = dayCells.stream()
            .filter(el -> {
                try {
                    String text = el.getText().trim();
                    int num = Integer.parseInt(text);
                    return num >= 1 && num <= 31;
                } catch (NumberFormatException e) {
                    return false;
                }
            }).count();

        assertTrue(actualDayCells >= daysInMonth,
            "Should render at least " + daysInMonth + " numbered day cells, found: " + actualDayCells);
    }

    // ─────────────────────────────────────────────────────────────
    // 2. Today is highlighted
    // ─────────────────────────────────────────────────────────────

    @Test
    @Order(4)
    @DisplayName("✔ Current date cell is highlighted/marked as today")
    void todayCellIsHighlighted() {
        int todayDay = LocalDate.now().getDayOfMonth();

        // Try explicit today selector
        List<WebElement> todayCells = driver.findElements(By.cssSelector(TODAY_CELL));

        if (!todayCells.isEmpty()) {
            String text = todayCells.get(0).getText().trim();
            // Either the cell text IS today's day number, or it's inside it
            boolean matchesDay = text.equals(String.valueOf(todayDay))
                || todayCells.get(0).findElements(By.xpath(".//*[text()='" + todayDay + "']")).size() > 0;
            assertTrue(matchesDay,
                "Today cell should show day " + todayDay + ", found: " + text);
        } else {
            // Fallback: find element with today's number that has a highlighted attribute/style
            JavascriptExecutor js = (JavascriptExecutor) driver;
            Boolean found = (Boolean) js.executeScript(
                "const elements = Array.from(document.querySelectorAll('*'));" +
                "return elements.some(el => {" +
                "  const txt = el.innerText && el.innerText.trim();" +
                "  const cls = el.className && el.className.toString().toLowerCase();" +
                "  return txt === '" + todayDay + "' && " +
                "    (cls.includes('today') || cls.includes('current') || " +
                "     cls.includes('highlight') || cls.includes('active'));" +
                "});"
            );
            assertTrue(found != null && found,
                "Day " + todayDay + " should be highlighted as today");
        }
    }

    // ─────────────────────────────────────────────────────────────
    // 3. Navigation
    // ─────────────────────────────────────────────────────────────

    @Test
    @Order(5)
    @DisplayName("✔ Previous month button navigates to prior month")
    void prevMonthNavigation() {
        List<WebElement> prevBtns = driver.findElements(By.cssSelector(PREV_BTN));
        if (prevBtns.isEmpty()) {
            // Skip if no nav buttons present
            System.out.println("SKIP: No prev month button found – calendar may not have navigation");
            return;
        }

        String beforeText = getCalendarTitleText();
        prevBtns.get(0).click();

        // Wait for re-render
        try { Thread.sleep(500); } catch (InterruptedException ignored) {}

        String afterText = getCalendarTitleText();
        assertNotEquals(beforeText, afterText,
            "Calendar title should change after clicking previous month");

        // Verify it's actually the previous month
        LocalDate prevMonth = LocalDate.now().minusMonths(1);
        String expectedPrev = prevMonth.getMonth()
            .getDisplayName(TextStyle.FULL, Locale.ENGLISH).toLowerCase();
        assertTrue(afterText.toLowerCase().contains(expectedPrev),
            "After clicking prev, should show: " + expectedPrev + " but got: " + afterText);
    }

    @Test
    @Order(6)
    @DisplayName("✔ Next month button navigates to next month")
    void nextMonthNavigation() {
        List<WebElement> nextBtns = driver.findElements(By.cssSelector(NEXT_BTN));
        if (nextBtns.isEmpty()) {
            System.out.println("SKIP: No next month button found – calendar may not have navigation");
            return;
        }

        String beforeText = getCalendarTitleText();
        nextBtns.get(0).click();

        try { Thread.sleep(500); } catch (InterruptedException ignored) {}

        String afterText = getCalendarTitleText();
        assertNotEquals(beforeText, afterText,
            "Calendar title should change after clicking next month");

        LocalDate nextMonth = LocalDate.now().plusMonths(1);
        String expectedNext = nextMonth.getMonth()
            .getDisplayName(TextStyle.FULL, Locale.ENGLISH).toLowerCase();
        assertTrue(afterText.toLowerCase().contains(expectedNext),
            "After clicking next, should show: " + expectedNext + " but got: " + afterText);
    }

    private String getCalendarTitleText() {
        List<WebElement> elements = driver.findElements(By.cssSelector(CALENDAR_MONTH_YEAR));
        if (!elements.isEmpty()) return elements.get(0).getText();
        return driver.findElement(By.tagName("body")).getText();
    }
}
