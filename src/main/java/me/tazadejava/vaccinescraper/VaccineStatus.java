package me.tazadejava.vaccinescraper;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import me.tazadejava.websitescraper.WebsiteScraper;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Objects;

/**
 * Immutable class that holds a current appointment status. Typically holds additional information only when AVAILABLE
 * @author Darren Lim (darrenl@mit.edu)
 */
public class VaccineStatus {

    public enum VaccineAvailability {
        AVAILABLE(0), UNAVAILABLE(1), UNKNOWN(2);

        final int value;

        VaccineAvailability(int value) {
            this.value = value;
        }
    }

    private final VaccineAvailability availability;
    private transient VaccineLocation loc;
    private final String infoMessage, currentUrl;
    private final LocalDateTime scrapeTime;

    private transient final WebsiteScraper websiteScraperSource;

    public VaccineStatus(VaccineAvailability availability, VaccineLocation loc, WebsiteScraper websiteScraperSource) {
        this(availability, loc, websiteScraperSource, "", "");
    }

    public VaccineStatus(VaccineAvailability availability, VaccineLocation loc, WebsiteScraper websiteScraperSource, String currentUrl, String infoMessage) {
        this.availability = availability;
        this.loc = loc;
        this.websiteScraperSource = websiteScraperSource;
        this.currentUrl = currentUrl;
        this.infoMessage = infoMessage;

        scrapeTime = LocalDateTime.now();
    }

    public JsonObject save(Gson gson) {
        JsonObject data = new JsonObject();

        data.addProperty("loc", loc.getID());

        data.add("serialized", gson.toJsonTree(this));

        return data;
    }

    public static VaccineStatus createInstance(HashMap<String, VaccineLocation> locs, Gson gson, JsonObject data) {
        VaccineStatus status = gson.fromJson(data.get("serialized"), VaccineStatus.class);

        status.loc = locs.get(data.get("loc").getAsString());

        return status;
    }

    public VaccineAvailability getAvailability() {
        return availability;
    }

    public VaccineLocation getLocation() {
        return loc;
    }

    @Override
    public String toString() {
        if(availability == VaccineAvailability.AVAILABLE) {
            return "VaccineStatus: AVAILABLE\n" +
                    "Loc: " + loc + "\n" +
                    "URL: " + currentUrl + "\n" +
                    "Scrape Time: " + scrapeTime.toString() + "\n";
        } else {
            return "VaccineStatus: " + availability + "\n" +
                    "Loc: " + loc;
        }
    }

    public WebsiteScraper getWebsiteScraperSource() {
        return websiteScraperSource;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        VaccineStatus that = (VaccineStatus) o;
        return availability == that.availability && Objects.equals(infoMessage, that.infoMessage) && Objects.equals(currentUrl, that.currentUrl) && Objects.equals(scrapeTime, that.scrapeTime);
    }

    @Override
    public int hashCode() {
        return Objects.hash(availability, infoMessage, currentUrl, scrapeTime);
    }
}
