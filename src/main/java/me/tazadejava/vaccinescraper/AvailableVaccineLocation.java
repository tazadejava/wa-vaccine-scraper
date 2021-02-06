package me.tazadejava.vaccinescraper;

import com.google.gson.JsonObject;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * Tracks data related to when a vaccine appointment is available.
 * @author Darren Lim (darrenl@mit.edu)
 */
public class AvailableVaccineLocation {

    private LocalDateTime availableTime, unavailableTime;
    private int secondsAvailable;

    public AvailableVaccineLocation() {
        availableTime = LocalDateTime.now();
    }

    public AvailableVaccineLocation(JsonObject data) {
        availableTime = LocalDateTime.parse(data.get("availableTime").getAsString());

        if(data.has("unavailableTime")) {
            unavailableTime = LocalDateTime.parse(data.get("unavailableTime").getAsString());
        }

        if(data.has("secondsAvailable")) {
            secondsAvailable = data.get("secondsAvailable").getAsInt();
        }
    }

    public JsonObject save() {
        JsonObject data = new JsonObject();

        data.addProperty("availableTime", availableTime.toString());
        if(unavailableTime != null) {
            data.addProperty("unavailableTime", unavailableTime.toString());
        }
        if (secondsAvailable != 0) {
            data.addProperty("secondsAvailable", secondsAvailable);
        }

        return data;
    }

    public void markUnavailable() {
        if(unavailableTime == null) {
            unavailableTime = LocalDateTime.now();
            secondsAvailable = (int) ChronoUnit.SECONDS.between(availableTime, unavailableTime);
        }
    }
}
