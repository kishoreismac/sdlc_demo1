package com.ecognity.service;

import com.ecognity.model.WeatherDay;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit Tests – WeatherService
 *
 * Coverage:
 *   ✔ Positive  – valid 5-day forecast, temp formatting
 *   ✔ Negative  – null/empty JSON, wrong forecast count, missing fields
 *   ✔ Edge      – exactly 4 or 6 days, inverted temps, special characters
 */
@DisplayName("WeatherService Unit Tests")
class WeatherServiceTest {

    private WeatherService service;

    @BeforeEach
    void setUp() { service = new WeatherService(); }

    // ─────────────────────────────────────────────────────────────
    // 1. parse()
    // ─────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("parse()")
    class ParseTests {

        private static final String VALID_JSON = """
            [
              {"date":"2026-03-24","temp_max":28.0,"temp_min":18.0,"description":"Sunny","icon":"01d"},
              {"date":"2026-03-25","temp_max":27.5,"temp_min":17.5,"description":"Partly cloudy","icon":"02d"},
              {"date":"2026-03-26","temp_max":25.0,"temp_min":16.0,"description":"Cloudy","icon":"03d"},
              {"date":"2026-03-27","temp_max":22.0,"temp_min":14.0,"description":"Light rain","icon":"10d"},
              {"date":"2026-03-28","temp_max":20.0,"temp_min":13.0,"description":"Showers","icon":"09d"}
            ]
            """;

        @Test
        @DisplayName("Positive – valid JSON returns 5 WeatherDay objects")
        void parsesValidJson() {
            List<WeatherDay> days = service.parse(VALID_JSON);
            assertEquals(5, days.size());
        }

        @Test
        @DisplayName("Positive – fields mapped correctly")
        void fieldsMappedCorrectly() {
            List<WeatherDay> days = service.parse(VALID_JSON);
            WeatherDay first = days.get(0);
            assertEquals("2026-03-24", first.getDate());
            assertEquals(28.0, first.getTempMax());
            assertEquals(18.0, first.getTempMin());
            assertEquals("Sunny", first.getDescription());
            assertEquals("01d", first.getIcon());
        }

        @ParameterizedTest(name = "parse({0}) returns empty list")
        @NullAndEmptySource
        @ValueSource(strings = {"   ", "not-json", "{}", "null", "<xml/>", "[]"})
        void negativeInvalidInputReturnsEmpty(String input) {
            List<WeatherDay> result = service.parse(input);
            assertNotNull(result, "Result should never be null");
            // null/blank/invalid JSON all yield empty; empty array also yields empty (size 0)
            assertTrue(result.size() == 0,
                "Expected empty list for input: " + input);
        }

        @Test
        @DisplayName("Negative – malformed JSON returns empty list (no exception)")
        void malformedJsonReturnsEmpty() {
            String bad = "[{\"date\":\"2026-03-24\", BROKEN}]";
            assertDoesNotThrow(() -> {
                List<WeatherDay> result = service.parse(bad);
                assertTrue(result.isEmpty());
            });
        }

        @Test
        @DisplayName("Edge – extra unknown fields are ignored")
        void extraFieldsIgnored() {
            String json = """
                [{"date":"2026-03-24","temp_max":20.0,"temp_min":10.0,
                  "description":"Sunny","icon":"01d","humidity":80,"wind_speed":5.2}]
                """;
            assertDoesNotThrow(() -> service.parse(json));
            assertEquals(1, service.parse(json).size());
        }
    }

    // ─────────────────────────────────────────────────────────────
    // 2. hasValidForecast()
    // ─────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("hasValidForecast()")
    class ValidForecastTests {

        @Test
        @DisplayName("Positive – exactly 5 valid days returns true")
        void fiveValidDaysReturnsTrue() {
            List<WeatherDay> days = build5ValidDays();
            assertTrue(service.hasValidForecast(days));
        }

        @Test
        @DisplayName("Negative – null input returns false")
        void nullInputReturnsFalse() {
            assertFalse(service.hasValidForecast(null));
        }

        @Test
        @DisplayName("Negative – empty list returns false")
        void emptyListReturnsFalse() {
            assertFalse(service.hasValidForecast(Collections.emptyList()));
        }

        @Test
        @DisplayName("Edge – 4 days returns false (must be exactly 5)")
        void fourDaysReturnsFalse() {
            List<WeatherDay> days = build5ValidDays().subList(0, 4);
            assertFalse(service.hasValidForecast(days));
        }

        @Test
        @DisplayName("Edge – 6 days returns false (must be exactly 5)")
        void sixDaysReturnsFalse() {
            List<WeatherDay> days = new ArrayList<>(build5ValidDays());
            days.add(new WeatherDay("2026-03-29", 19.0, 12.0, "Fog", "50d"));
            assertFalse(service.hasValidForecast(days));
        }

        @Test
        @DisplayName("Negative – one day missing description fails validation")
        void missingDescriptionFails() {
            List<WeatherDay> days = build5ValidDays();
            days.set(2, new WeatherDay("2026-03-26", 25.0, 15.0, null, "03d"));
            assertFalse(service.hasValidForecast(days));
        }

        @Test
        @DisplayName("Negative – one day with null temperature fails validation")
        void nullTempFails() {
            List<WeatherDay> days = build5ValidDays();
            days.set(0, new WeatherDay("2026-03-24", null, 18.0, "Sunny", "01d"));
            assertFalse(service.hasValidForecast(days));
        }
    }

    // ─────────────────────────────────────────────────────────────
    // 3. formatTempRange()
    // ─────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("formatTempRange()")
    class FormatTempRangeTests {

        @Test
        @DisplayName("Positive – normal temps formatted correctly")
        void normalTemps() {
            WeatherDay d = new WeatherDay("2026-03-24", 28.0, 18.0, "Sunny", "01d");
            assertEquals("28°C / 18°C", service.formatTempRange(d));
        }

        @Test
        @DisplayName("Positive – negative temps formatted correctly")
        void negativeTemps() {
            WeatherDay d = new WeatherDay("2026-12-24", -2.0, -10.0, "Snow", "13d");
            assertEquals("-2°C / -10°C", service.formatTempRange(d));
        }

        @Test
        @DisplayName("Edge – same max and min temps")
        void sameMaxMin() {
            WeatherDay d = new WeatherDay("2026-03-24", 20.0, 20.0, "Fog", "50d");
            assertEquals("20°C / 20°C", service.formatTempRange(d));
        }

        @Test
        @DisplayName("Negative – null WeatherDay returns N/A")
        void nullDayReturnsNA() {
            assertEquals("N/A", service.formatTempRange(null));
        }

        @Test
        @DisplayName("Negative – null tempMax returns N/A")
        void nullTempMaxReturnsNA() {
            WeatherDay d = new WeatherDay("2026-03-24", null, 18.0, "Sunny", "01d");
            assertEquals("N/A", service.formatTempRange(d));
        }
    }

    // ─────────────────────────────────────────────────────────────
    // 4. areTempRangesConsistent()
    // ─────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("areTempRangesConsistent()")
    class TempConsistencyTests {

        @Test
        @DisplayName("Positive – max >= min for all days returns true")
        void consistentTemps() {
            assertTrue(service.areTempRangesConsistent(build5ValidDays()));
        }

        @Test
        @DisplayName("Negative – one day has max < min returns false")
        void invertedTempsFails() {
            List<WeatherDay> days = build5ValidDays();
            days.set(1, new WeatherDay("2026-03-25", 10.0, 25.0, "Error", "01d")); // min > max
            assertFalse(service.areTempRangesConsistent(days));
        }

        @Test
        @DisplayName("Negative – null list returns false")
        void nullReturnsFalse() {
            assertFalse(service.areTempRangesConsistent(null));
        }
    }

    // ─────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────

    private List<WeatherDay> build5ValidDays() {
        List<WeatherDay> days = new ArrayList<>();
        days.add(new WeatherDay("2026-03-24", 28.0, 18.0, "Sunny",        "01d"));
        days.add(new WeatherDay("2026-03-25", 27.0, 17.0, "Partly cloudy","02d"));
        days.add(new WeatherDay("2026-03-26", 25.0, 16.0, "Cloudy",       "03d"));
        days.add(new WeatherDay("2026-03-27", 22.0, 14.0, "Light rain",   "10d"));
        days.add(new WeatherDay("2026-03-28", 20.0, 13.0, "Showers",      "09d"));
        return days;
    }
}
