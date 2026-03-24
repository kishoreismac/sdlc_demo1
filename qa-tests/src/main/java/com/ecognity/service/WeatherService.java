package com.ecognity.service;

import com.ecognity.model.WeatherDay;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Collections;
import java.util.List;

/**
 * Parses and validates weather forecast data.
 */
public class WeatherService {

    private static final int REQUIRED_FORECAST_DAYS = 5;
    private final ObjectMapper mapper = new ObjectMapper();

    /**
     * Parse raw JSON string into a list of WeatherDay objects.
     * Returns empty list on null / blank / invalid JSON.
     */
    public List<WeatherDay> parse(String json) {
        if (json == null || json.isBlank()) return Collections.emptyList();
        try {
            List<WeatherDay> parsed = mapper.readValue(json, new TypeReference<List<WeatherDay>>() {});
            return parsed == null ? Collections.emptyList() : parsed;
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    /** Returns true only when exactly 5 valid forecast days are present. */
    public boolean hasValidForecast(List<WeatherDay> days) {
        if (days == null || days.size() != REQUIRED_FORECAST_DAYS) return false;
        return days.stream().allMatch(WeatherDay::isValid);
    }

    /** Returns the temperature range label, e.g. "12°C / 8°C". */
    public String formatTempRange(WeatherDay day) {
        if (day == null || day.getTempMax() == null || day.getTempMin() == null) {
            return "N/A";
        }
        return String.format("%.0f°C / %.0f°C", day.getTempMax(), day.getTempMin());
    }

    /** Validates that temp_max >= temp_min for each day. */
    public boolean areTempRangesConsistent(List<WeatherDay> days) {
        if (days == null) return false;
        return days.stream()
                   .filter(d -> d.getTempMax() != null && d.getTempMin() != null)
                   .allMatch(d -> d.getTempMax() >= d.getTempMin());
    }
}
