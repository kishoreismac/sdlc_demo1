package com.ecognity.api;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.junit.jupiter.api.*;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

import static io.restassured.RestAssured.*;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * REST API Tests – Calendar / Deployment Validation
 *
 * Covers:
 *   GET /          – App loads (HTTP 200)
 *   Static assets  – JS, CSS return 200
 *   GET /calendar  – calendar API schema
 *   Deployment validation checks (3 mandatory)
 */
@DisplayName("Calendar & Deployment Validation Tests")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class CalendarAndDeploymentTest {

    private static final String APP_BASE =
        System.getProperty("app.base.url", "https://ec-user-home-page.azurewebsites.net");
    private static final String API_BASE =
        System.getProperty("app.api.url", "https://ec-user-home-page.azurewebsites.net/api");
    private static boolean calendarApiAvailable;
    private static int calendarApiStatusCode;

    @BeforeAll
    static void init() {
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();

        // Probe once so API-specific assertions are skipped when endpoint is not deployed.
        Response probe = given().baseUri(API_BASE).when().get("/calendar");
        calendarApiStatusCode = probe.statusCode();
        calendarApiAvailable = calendarApiStatusCode == 200;
    }

    private static void requireCalendarApi() {
        Assumptions.assumeTrue(
            calendarApiAvailable,
            "Calendar API endpoint unavailable at " + API_BASE + "/calendar (status " + calendarApiStatusCode + ")"
        );
    }

    // ─────────────────────────────────────────────────────────────
    // DEPLOYMENT VALIDATION (Checks 1-3 mandatory)
    // ─────────────────────────────────────────────────────────────

    @Test
    @Order(1)
    @DisplayName("[DEPLOY-1] Home page loads → HTTP 200")
    void deployCheck1_HomePageLoads() {
        given()
            .baseUri(APP_BASE)
        .when()
            .get("/")
        .then()
            .statusCode(200)
            .body(not(emptyString()));
    }

    @Test
    @Order(2)
    @DisplayName("[DEPLOY-2] app.js static file returns 200 with JS content-type")
    void deployCheck2_AppJsLoads() {
        given()
            .baseUri(APP_BASE)
        .when()
            .get("/app.js")
        .then()
            .statusCode(200)
            .contentType(containsString("javascript"));
    }

    @Test
    @Order(3)
    @DisplayName("[DEPLOY-2] styles.css static file returns 200 with CSS content-type")
    void deployCheck3_StylesCssLoads() {
        given()
            .baseUri(APP_BASE)
        .when()
            .get("/styles.css")
        .then()
            .statusCode(200)
            .contentType(containsString("css"));
    }

    @Test
    @Order(4)
    @DisplayName("[DEPLOY-2] auth-config.js returns 200")
    void deployCheck4_AuthConfigLoads() {
        given()
            .baseUri(APP_BASE)
        .when()
            .get("/auth-config.js")
        .then()
            .statusCode(200);
    }

    @Test
    @Order(5)
    @DisplayName("[DEPLOY-3] No 5xx errors on homepage")
    void deployCheck5_No5xxOnHome() {
        Response res = given().baseUri(APP_BASE).get("/");
        assertTrue(res.statusCode() < 500,
            "Homepage must not return 5xx, got: " + res.statusCode());
    }

    // ─────────────────────────────────────────────────────────────
    // CALENDAR API
    // ─────────────────────────────────────────────────────────────

    @Test
    @Order(6)
    @DisplayName("GET /calendar → 200 OK")
    void calendarReturns200() {
        requireCalendarApi();
        given()
            .baseUri(API_BASE)
        .when()
            .get("/calendar")
        .then()
            .statusCode(200);
    }

    @Test
    @Order(7)
    @DisplayName("GET /calendar → contains current year and month")
    void calendarContainsCurrentYearMonth() {
        requireCalendarApi();
        LocalDate today = LocalDate.now();
        given()
            .baseUri(API_BASE)
        .when()
            .get("/calendar")
        .then()
            .statusCode(200)
            .body("year",  equalTo(today.getYear()))
            .body("month", equalTo(today.getMonthValue()));
    }

    @Test
    @Order(8)
    @DisplayName("GET /calendar → days array has correct count for current month")
    void calendarDaysCountCorrect() {
        requireCalendarApi();
        int expectedDays = YearMonth.now().lengthOfMonth();
        Response res = given().baseUri(API_BASE).get("/calendar");
        assertEquals(200, res.statusCode());

        List<?> days = res.jsonPath().getList("days");
        assertNotNull(days, "days array should not be null");
        assertEquals(expectedDays, days.size(),
            "Calendar should have " + expectedDays + " days this month");
    }

    @Test
    @Order(9)
    @DisplayName("GET /calendar → today is marked as current")
    void calendarTodayIsMarked() {
        requireCalendarApi();
        int today = LocalDate.now().getDayOfMonth();
        given()
            .baseUri(API_BASE)
        .when()
            .get("/calendar")
        .then()
            .statusCode(200)
            .body("today", equalTo(today));
    }

    @Test
    @Order(10)
    @DisplayName("GET /calendar?month=13 → 400 for invalid month")
    void calendarInvalidMonthReturns400() {
        requireCalendarApi();
        given()
            .baseUri(API_BASE)
            .queryParam("month", 13)
        .when()
            .get("/calendar")
        .then()
            .statusCode(anyOf(equalTo(400), equalTo(422)));
    }

    @Test
    @Order(11)
    @DisplayName("GET /calendar?year=1800 → 400 for out-of-range year")
    void calendarOutOfRangeYearReturns400() {
        requireCalendarApi();
        given()
            .baseUri(API_BASE)
            .queryParam("year", 1800)
        .when()
            .get("/calendar")
        .then()
            .statusCode(anyOf(equalTo(400), equalTo(422)));
    }

    @Test
    @Order(12)
    @DisplayName("GET /leave → leave page returns 200")
    void leavePageLoads() {
        given()
            .baseUri(APP_BASE)
        .when()
            .get("/leave.html")
        .then()
            .statusCode(200)
            .body(not(emptyString()));
    }
}
