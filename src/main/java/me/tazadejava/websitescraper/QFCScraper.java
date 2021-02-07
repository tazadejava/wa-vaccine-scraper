package me.tazadejava.websitescraper;

import me.tazadejava.vaccinescraper.VaccineLocation;
import me.tazadejava.vaccinescraper.VaccineStatus;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.List;

/**
 * @author Darren Lim (darrenl@mit.edu)
 */
public class QFCScraper extends WebsiteScraper {

    public QFCScraper(WebDriver webDriver) {
        super(webDriver);
    }

    @Override
    public String getRequiredURLPrefix() {
        return "https://www.qfc.com";
    }

    @Override
    public VaccineStatus checkForVaccineStatus(VaccineLocation loc) throws Exception {
        webDriver.get("https://www.qfc.com/rx/guest/get-vaccinated");

        if(!waitUntilPageLoadsCustom("input", "placeholder", "ZIP code, City, State OR Name")) {
            return new VaccineStatus(VaccineStatus.VaccineAvailability.UNAVAILABLE, loc, this, "", "FAILED TO LOAD INITIAL PAGE");
        }

        WebElement input = webDriver.findElement(By.cssSelector("input[placeholder='ZIP code, City, State OR Name']"));

        char[] chars = loc.getAddress().toCharArray();
        for(char next : chars) {
            input.sendKeys("" + next);
            Thread.sleep(10 + (int) (Math.random() * 30));
        }
        input.sendKeys(Keys.ENTER);

        waitUntilPageLoadsClass("SearchResults-storeResultsDetails");

        List<WebElement> selectionDetails = webDriver.findElements(By.className("SearchResults-storeResultsDetails"));

        if(selectionDetails.isEmpty()) {
            return new VaccineStatus(VaccineStatus.VaccineAvailability.UNAVAILABLE, loc, this, "", "NO SELECTION DETAILS");
        }

        List<WebElement> selection = getChildNodes(selectionDetails.get(0));

        //click select button
        getChildNodes(getChildNodes(selection.get(0)).get(0)).get(1).click();

        //continue, confirming selection
        waitUntilPageLoadsCustom("button", "aria-label", "Continue to step 1 of 7 of the Vaccination Form");
        webDriver.findElement(By.cssSelector("button[aria-label='Continue to step 1 of 7 of the Vaccination Form']")).click();

        waitUntilPageLoadsCustom("input", "name", "COVID-19 Vaccine");
        List<WebElement> covidButton = webDriver.findElements(By.cssSelector("input[name='COVID-19 Vaccine']"));

        if(covidButton.isEmpty()) {
            return new VaccineStatus(VaccineStatus.VaccineAvailability.UNAVAILABLE, loc, this, "", "NO COVID BUTTON");
        }

        covidButton.get(0).click();

        //continue, confirming vaccine checkbox
        waitUntilPageLoadsCustom("button", "aria-label", "Continue to step 2 of 7 of the Vaccination Form");
        webDriver.findElement(By.cssSelector("button[aria-label='Continue to step 2 of 7 of the Vaccination Form']")).click();

        waitUntilPageLoadsClass("ScheduleAppointment-container");

        Thread.sleep(1000);

        if(webDriver.getPageSource().contains("Sorry, there are no available time slots at this location.")) {
            return new VaccineStatus(VaccineStatus.VaccineAvailability.UNAVAILABLE, loc, this);
        }

        if(webDriver.getPageSource().contains("Select a date to reserve your spot")) {
            return new VaccineStatus(VaccineStatus.VaccineAvailability.AVAILABLE, loc, this, "https://www.qfc.com/rx/guest/get-vaccinated", "");
        } else {
            return new VaccineStatus(VaccineStatus.VaccineAvailability.UNAVAILABLE, loc, this, "", "NO SELECT A DATE TEXT");
        }
    }
}
