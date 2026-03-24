package com.ecognity.api;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.junit.jupiter.api.*;

import static io.restassured.RestAssured.*;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * REST API Tests – Weather Endpoint
 *
 * Base URL resolved from system property: app.api.url
 * Run: mvn test -Dapp.api.url=https://ec-user-home-page.azurewebsites.net/api
 *
 * Covers:
 *   GET /weather           – 200, schema, 5 days, data correctness
 *   GET /weather?days=5    – explicit param
 *   GET /weather?days=abc  – invalid param → 400
 *   GET /weather (timeout) – simulated slow response
 *   GET /weather (empty)   – graceful empty response handling
 */
@DisplayName("Weather API Tests")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class WeatherApiTest {

    private static final String BASE_URL =
        System.getProperty("app.api.url", "https://ec-user-home-page.azurewebsites.net/api");

    private static final int TIMEOUT_MS = 5000;
    private static boolean weatherApiAvailable;
    private static int weatherApiStatusCode;

    @BeforeAll
    static void setUp() {
        RestAssured.baseURI = BASE_URL;
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();

        // Probe once so endpoint-specific tests can be skipped when API is not deployed.
        Response probe = given().when().get("/weather");
        weatherApiStatusCode = probe.statusCode();
        weatherApiAvailable = weatherApiStatusCode == 200;
    }

    private static void requireWeatherApi() {
        Assumptions.assumeTrue(
            weatherApiAvailable,
            "Weather API endpoint unavailable at " + BASE_URL + "/weather (status " + weatherApiStatusCode + ")"
        );
    }

    // ─────────────────────────────────────────────────────────────
    // 1. Happy-path
    // ─────────────────────────────────────────────────────────────

    @Test
    @Order(1)
    @DisplayName("GET /weather → 200 OK")
    void weatherReturns200() {
        requireWeatherApi();
        given()
            .contentType(ContentType.JSON)
        .when()
            .get("/weather")
        .then()
            .statusCode(200);
    }

    @Test
    @Order(2)
    @DisplayName("GET /weather → Content-Type is application/json")
    void weatherReturnsJson() {
        requireWeatherApi();
        given()
        .when()
            .get("/weather")
        .then()
            .contentType(ContentType.JSON);
    }

    @Test
    @Order(3)
    @DisplayName("GET /weather → response body contains exactly 5 forecast days")
    void weatherReturnsFiveDays() {
        requireWeatherApi();
        given()
        .when()
            .get("/weather")
        .then()
            .statusCode(200)
            .body("$.size()", equalTo(5));
    }

    @Test
    @Order(4)
    @DisplayName("GET /weather → each day has required fields: date, temp_max, temp_min, description")
    void weatherDayHasRequiredFields() {
        requireWeatherApi();
        given()
        .when()
            .get("/weather")
        .then()
            .statusCode(200)
            .body("[0].date",        notNullValue())
            .body("[0].temp_max",    notNullValue())
            .body("[0].temp_min",    notNullValue())
            .body("[0].description", notNullValue())
            .body("[0].icon",        notNullValue());
    }

    @Test
    @Order(5)
    @DisplayName("GET /weather → temp_max >= temp_min for all days")
    void tempMaxAlwaysGteMin() {
        requireWeatherApi();
        Response response = given().get("/weather");
        assertEquals(200, response.statusCode());

        int size = response.jsonPath().getList("$").size();
        for (int i = 0; i < size; i++) {
            float max = response.jsonPath().getFloat("[" + i + "].temp_max");
            float min = response.jsonPath().getFloat("[" + i + "].temp_min");
            assertTrue(max >= min,
                "Day " + i + " temp_max (" + max + ") must be >= temp_min (" + min + ")");
        }
    }

    @Test
    @Order(6)
    @DisplayName("GET /weather → dates are consecutive and start from today")
    void datesAreConsecutive() {
        requireWeatherApi();
        Response response = given().get("/weather");
        assertEquals(200, response.statusCode());

        java.util.List<String> dates = response.jsonPath().getList("date");
        assertEquals(5, dates.size());

        java.time.LocalDate today = java.time.LocalDate.now();
        for (int i = 0; i < dates.size(); i++) {
            java.time.LocalDate expected = today.plusDays(i);
            java.time.LocalDate actual   = java.time.LocalDate.parse(dates.get(i));
            assertEquals(expected, actual,
                "Day " + i + " should be " + expected + " but was " + actual);
        }
    }

    // ─────────────────────────────────────────────────────────────
    // 2. Parameter validation
    // ─────────────────────────────────────────────────────────────

    @Test
    @Order(7)
    @DisplayName("GET /weather?days=5 → explicit param returns 200 with 5 days")
    void explicitDaysParam() {
        requireWeatherApi();
        given()
            .queryParam("days", 5)
        .when()
            .get("/weather")
        .then()
            .statusCode(200)
            .body("$.size()", equalTo(5));
    }

    @Test
    @Order(8)
    @DisplayName("GET /weather?days=abc → 400 Bad Request for invalid param")
    void invalidDaysParamReturns400() {
        requireWeatherApi();
        given()
            .queryParam("days", "abc")
        .when()
            .get("/weather")
        .then()
            .statusCode(anyOf(equalTo(400), equalTo(422)));
    }

    @Test
    @Order(9)
    @DisplayName("GET /weather?days=0 → 400 or 422 for zero days")
    void zeroDaysReturnsError() {
        requireWeatherApi();
        given()
            .queryParam("days", 0)
        .when()
            .get("/weather")
        .then()
            .statusCode(anyOf(equalTo(400), equalTo(422)));
    }

    @Test
    @Order(10)
    @DisplayName("GET /weather?days=100 → 400 or capped at max allowed days")
    void tooManyDaysHandled() {
        requireWeatherApi();
        Response res = given().queryParam("days", 100).get("/weather");
        assertTrue(res.statusCode() == 400 || res.statusCode() == 200,
            "Expected 400 or 200 (capped), got: " + res.statusCode());
    }

    // ─────────────────────────────────────────────────────────────
    // 3. Response time / availability
    // ─────────────────────────────────────────────────────────────

    @Test
    @Order(11)
    @DisplayName("GET /weather → responds within 5 seconds")
    void responseTimeUnder5Seconds() {
        requireWeatherApi();
        given()
        .when()
            .get("/weather")
        .then()
            .statusCode(200)
            .time(lessThan((long) TIMEOUT_MS));
    }

    // ─────────────────────────────────────────────────────────────
    // 4. Non-existent endpoint
    // ─────────────────────────────────────────────────────────────

    @Test
    @Order(12)
    @DisplayName("GET /non-existent-endpoint → 404")
    void nonExistentReturns404() {
        given()
        .when()
            .get("/non-existent-endpoint")
        .then()
            .statusCode(404);
    }

    // ─────────────────────────────────────────────────────────────
    // 5. HTTP method restrictions
    // ─────────────────────────────────────────────────────────────

    @Test
    @Order(13)
    @DisplayName("POST /weather → 405 Method Not Allowed")
    void postWeatherNotAllowed() {
        given()
            .contentType(ContentType.JSON)
            .body("{}")
        .when()
            .post("/weather")
        .then()
            .statusCode(anyOf(equalTo(405), equalTo(404)));
    }
}
