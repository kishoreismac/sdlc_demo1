package com.ecognity.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents one day's weather forecast entry.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class WeatherDay {

    @JsonProperty("date")
    private String date;

    @JsonProperty("temp_max")
    private Double tempMax;

    @JsonProperty("temp_min")
    private Double tempMin;

    @JsonProperty("description")
    private String description;

    @JsonProperty("icon")
    private String icon;

    public WeatherDay() {}

    public WeatherDay(String date, Double tempMax, Double tempMin, String description, String icon) {
        this.date = date;
        this.tempMax = tempMax;
        this.tempMin = tempMin;
        this.description = description;
        this.icon = icon;
    }

    public String getDate()        { return date; }
    public Double getTempMax()     { return tempMax; }
    public Double getTempMin()     { return tempMin; }
    public String getDescription() { return description; }
    public String getIcon()        { return icon; }

    public boolean isValid() {
        return date != null && !date.isBlank()
            && tempMax != null
            && tempMin != null
            && description != null && !description.isBlank();
    }
}
