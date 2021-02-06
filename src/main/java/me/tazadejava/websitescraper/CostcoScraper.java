package me.tazadejava.websitescraper;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import me.tazadejava.vaccinescraper.VaccineLocation;
import me.tazadejava.vaccinescraper.VaccineStatus;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * @author Darren Lim (darrenl@mit.edu)
 */
public class CostcoScraper extends WebsiteScraper {

    private Gson gson = new Gson();

    public CostcoScraper(WebDriver webDriver) {
        super(webDriver);
    }

    @Override
    public String getRequiredURLPrefix() {
        return "https://book-costcopharmacy.appointment-plus.com";
    }

    @Override
    public VaccineStatus checkForVaccineStatus(VaccineLocation loc) throws Exception {
        //may be able to obtain the appt information more quickly through the API
        LocalDate startOfMonth = LocalDate.now().withDayOfMonth(1);
        LocalDate nextMonth = startOfMonth.plusMonths(1).plusDays(14);
        DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE;
        String startOfMonthString = startOfMonth.format(formatter);
        String nextMonthString = nextMonth.format(formatter);

        int costcoId = Integer.parseInt(loc.getUrl().substring(loc.getUrl().indexOf("e_id=") + 5));

        webDriver.get("https://book-costcopharmacy.appointment-plus.com/book-appointment/clients/calendar-dates?startTimestamp=" + startOfMonthString + "+00%3A00%3A00&endTimestamp=" + nextMonthString + "+00%3A00%3A00&employeeId=" + costcoId + "&services[]=119&numberOfSpotsNeeded=1&isStoreHours=true&limitNumberOfSlotsInADay=1&clientMasterId=422117&_=1612644158090");

        String text = webDriver.findElement(By.cssSelector("pre")).getText();

        JsonObject data = gson.fromJson(text, JsonObject.class);

        if(data.getAsJsonArray("data").size() > 0 && data.getAsJsonArray("errors").size() == 0) {
            System.out.println("COSTCO IS AVAILABLE BY API " + webDriver.getCurrentUrl());
            return new VaccineStatus(VaccineStatus.VaccineAvailability.AVAILABLE, loc, this, loc.getUrl(), "");
        } else {
            return new VaccineStatus(VaccineStatus.VaccineAvailability.UNAVAILABLE, loc, this, loc.getUrl(), "UNAVAILABLE BY API");
        }

        //manual check through website if no availablity was found

//        webDriver.get(loc.getUrl());
//
//        if(!waitUntilPageLoadsClass("chevron-row")) {
//            return new VaccineStatus(VaccineStatus.VaccineAvailability.UNAVAILABLE, loc, this, "", "FAILED TO LOAD PAGE");
//        }
//
//        List<WebElement> getStartedButton = webDriver.findElements(By.className("chevron-row"));
//
//        getStartedButton.get(0).click();
//        waitUntilPageLoadsClass("gridMsg");
//
//        List<WebElement> gridMessage = webDriver.findElements(By.className("gridMsg"));
//        if(gridMessage.size() == 1) {
//            List<WebElement> children = getChildNodes(gridMessage.get(0));
//            if(children.size() > 0) {
//                if(children.get(0).getText().startsWith("We’re sorry, but there are not available times.")) {
//                    return new VaccineStatus(VaccineStatus.VaccineAvailability.UNAVAILABLE, loc, this);
//                }
//            }
//        }
//
//        Thread.sleep(1000);
//
//        if(webDriver.getPageSource().contains("We're sorry, but no")) {
//            return new VaccineStatus(VaccineStatus.VaccineAvailability.UNAVAILABLE, loc, this);
//        }
//
//        return new VaccineStatus(VaccineStatus.VaccineAvailability.AVAILABLE, loc, this, loc.getUrl(), "");
    }
}
