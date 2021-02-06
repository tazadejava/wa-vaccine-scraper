package me.tazadejava.websitescraper;

import me.tazadejava.vaccinescraper.VaccineLocation;
import me.tazadejava.vaccinescraper.VaccineStatus;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.List;

/**
 * @author Darren Lim (darrenl@mit.edu)
 */
public class CostcoScraper extends WebsiteScraper {

    public CostcoScraper(WebDriver webDriver) {
        super(webDriver);
    }

    @Override
    public String getRequiredURLPrefix() {
        return "https://book-costcopharmacy.appointment-plus.com";
    }

    @Override
    public VaccineStatus checkForVaccineStatus(VaccineLocation loc) throws Exception {
        webDriver.get(loc.getUrl());

        if(!waitUntilPageLoadsClass("chevron-row")) {
            return new VaccineStatus(VaccineStatus.VaccineAvailability.UNAVAILABLE, loc, this, "", "FAILED TO LOAD PAGE");
        }

        List<WebElement> getStartedButton = webDriver.findElements(By.className("chevron-row"));

        getStartedButton.get(0).click();
        waitUntilPageLoadsClass("gridMsg");

        List<WebElement> gridMessage = webDriver.findElements(By.className("gridMsg"));
        if(gridMessage.size() == 1) {
            List<WebElement> children = getChildNodes(gridMessage.get(0));
            if(children.size() > 0) {
                if(children.get(0).getText().startsWith("We’re sorry, but there are not available times.")) {
                    return new VaccineStatus(VaccineStatus.VaccineAvailability.UNAVAILABLE, loc, this);
                }
            }
        }

        Thread.sleep(1000);

        if(webDriver.getPageSource().contains("We're sorry, but no")) {
            return new VaccineStatus(VaccineStatus.VaccineAvailability.UNAVAILABLE, loc, this);
        }

        return new VaccineStatus(VaccineStatus.VaccineAvailability.AVAILABLE, loc, this, loc.getUrl(), "");
    }
}
