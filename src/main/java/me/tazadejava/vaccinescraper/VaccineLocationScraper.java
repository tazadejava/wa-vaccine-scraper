package me.tazadejava.vaccinescraper;

import com.google.gson.*;
import org.openqa.selenium.By;
import org.openqa.selenium.ElementClickInterceptedException;
import org.openqa.selenium.WebElement;

import java.io.*;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Scrapes the locations list from the wa.gov website. Configured to occur once every 2 days.
 * @author Darren Lim (darrenl@mit.edu)
 */
public class VaccineLocationScraper extends WebDriverClass {

    private LocalDateTime lastLocationUpdateTime;

    private Set<String> permittedCounties = new HashSet<>();

    public VaccineLocationScraper(String proxy) {
        super(proxy);

        permittedCounties.add("King County");
//        permittedCounties.add("Snohomish County");
//        permittedCounties.add("Pierce County");
    }

    private File getVaccineLocationsFile() {
        return new File(VaccineManager.SUBFOLDER + "/vaccine_locations.json");
    }

    private boolean shouldUpdateVaccineLocationsFile() {
        Gson gson = new Gson();
        File locFile = getVaccineLocationsFile();
        if(locFile.exists()) {
            try {
                FileReader reader = new FileReader(locFile);
                JsonObject data = gson.fromJson(reader, JsonObject.class);

                lastLocationUpdateTime = LocalDateTime.parse(data.get("lastLocationUpdateTime").getAsString());

                reader.close();

                System.out.println("HOURS SINCE LAST LOCATIONS UPDATE: " + ChronoUnit.HOURS.between(lastLocationUpdateTime, LocalDateTime.now()));
                return ChronoUnit.DAYS.between(lastLocationUpdateTime, LocalDateTime.now()) >= 2;
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            System.out.println("COULD NOT FIND LOCATION FILE, MUST UPDATE");
        }

        return true;
    }

    private List<VaccineLocation> getStoredVaccineLocations() {
        Gson gson = new Gson();

        File locFile = getVaccineLocationsFile();
        if(locFile.exists()) {
            try {
                FileReader reader = new FileReader(locFile);
                JsonObject data = gson.fromJson(reader, JsonObject.class);
                reader.close();

                lastLocationUpdateTime = LocalDateTime.parse(data.get("lastLocationUpdateTime").getAsString());

                List<VaccineLocation> locs = new ArrayList<>();

                JsonArray locsArray = data.getAsJsonArray("locations");
                for(JsonElement element : locsArray) {
                    locs.add(VaccineLocation.createInstance(gson, element.getAsJsonObject()));
                }

                return locs;
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return new ArrayList<>();
    }

    private void saveVaccineLocations(List<VaccineLocation> locations) {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();

        File locFile = getVaccineLocationsFile();
        try {
            if(!locFile.exists()) {
                locFile.getParentFile().mkdirs();
                locFile.createNewFile();
            }

            FileWriter writer = new FileWriter(locFile);
            JsonObject data = new JsonObject();

            data.addProperty("lastLocationUpdateTime", LocalDateTime.now().toString());

            JsonArray locs = new JsonArray();
            for(VaccineLocation loc : locations) {
                locs.add(loc.save(gson));
            }

            data.add("locations", locs);

            gson.toJson(data, writer);

            writer.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        VaccineManager.updateLastUpdateTimeFile();
    }

    private List<VaccineLocation> scrapeAllVaccineLocations() {
        String url = "https://www.doh.wa.gov/YouandYourFamily/Immunization/VaccineLocations";
        List<VaccineLocation> locations = new ArrayList<>();

        webDriver.get(url);

        if(!waitUntilPageLoadsId("accordion34979")) {
            System.out.println("ACCORDION NOT FOUND. TRY AGAIN");
            return scrapeAllVaccineLocations();
        }

        WebElement table = webDriver.findElement(By.id("accordion34979"));
        List<WebElement> tableChildren = getChildNodesWithClass(table, "panel");
        for(WebElement child : tableChildren) {
            List<WebElement> countyAndDetails = getChildNodes(child);
            List<WebElement> clickableBar = getNestedChildNodesWithClass(child, "accToggle");

            String countyName = countyAndDetails.get(0).getText();

            //filter only specific counties for now
            if(!permittedCounties.contains(countyName)) {
                continue;
            }

            System.out.println("Scrape locations for " + countyName);

            //open panel
            try {
                clickableBar.get(0).click();
            } catch(ElementClickInterceptedException ex) {
                //tried to click too early
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                clickableBar.get(0).click();
            }

            List<WebElement> listOfLocations = getChildNodes(getChildNodes(countyAndDetails.get(1)).get(0));

            for(WebElement loc : listOfLocations) {
                List<WebElement> details = getChildNodes(loc);

                if(!details.isEmpty()) {
                    String city = details.get(0).getText();
                    String[] locInfo = details.get(1).getText().split("\n");
                    String additionalDetails = details.get(2).getText();

                    String locUrl = "";
                    List<WebElement> hrefChild = getChildNodes(getChildNodes(details.get(1)).get(0));
                    if(hrefChild.size() > 0) {
                        locUrl = hrefChild.get(0).getAttribute("href");
                    }


                    locations.add(new VaccineLocation(locUrl, countyName, city, locInfo[0], locInfo.length > 2 ? locInfo[1] + ", " + locInfo[2] : "", locInfo.length > 3 ? locInfo[3] : "", locInfo.length > 4 ? locInfo[4] : "", additionalDetails));
                }
            }

            //try not to click too early
            try {
                Thread.sleep(300);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        saveVaccineLocations(locations);

        return locations;
    }

    public List<VaccineLocation> getAllVaccineLocations() {
        if(shouldUpdateVaccineLocationsFile()) {
            System.out.println("UPDATING LOCATIONS");
            return scrapeAllVaccineLocations();
        } else {
            System.out.println("LOADING LOCATIONS FROM FILE");
            return getStoredVaccineLocations();
        }
    }
}
