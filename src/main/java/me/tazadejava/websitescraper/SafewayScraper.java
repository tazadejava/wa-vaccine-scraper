package me.tazadejava.websitescraper;

import me.tazadejava.vaccinescraper.VaccineLocation;
import me.tazadejava.vaccinescraper.VaccineStatus;
import org.openqa.selenium.By;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.util.List;

/**
 * @author Darren Lim (darrenl@mit.edu)
 */
public class SafewayScraper extends WebsiteScraper {

    public SafewayScraper(WebDriver webDriver) {
        super(webDriver);
    }

    @Override
    public String getRequiredURLPrefix() {
        return "https://kordinator.mhealthcoach.net/vcl";
    }

    @Override
    public VaccineStatus checkForVaccineStatus(VaccineLocation loc) throws Exception {
        webDriver.get(loc.getUrl());

        if(!waitUntilPageLoadsId("attestation_1002")) {
            return new VaccineStatus(VaccineStatus.VaccineAvailability.UNAVAILABLE, loc, this, "", "FAILED TO LOAD PAGE");
        }

        webDriver.findElement(By.id("attestation_1002")).click();

        List<WebElement> buttons;
        WebElement button = getButtonWithText("Submit");
        button.click();

        waitUntilPageLoadsClass("appointmentType-type");

        Select dropdown = new Select(webDriver.findElement(By.id("appointmentType-type")));

        if(dropdown.getOptions().size() == 1) {
            return new VaccineStatus(VaccineStatus.VaccineAvailability.UNAVAILABLE, loc, this);
        }

        dropdown.selectByIndex(1);

        button = getButtonWithText("Start Set up");
        button.click(); //next
        waitUntilPageLoadsClass("ng-binding", 39); //26 -> 40

        Thread.sleep(1000);

        buttons = webDriver.findElements(By.tagName("button"));
        for(WebElement element : buttons) {
            if(element.getText().equals("Next") && element.isDisplayed()) {
                element.click();
            }
        }

        Thread.sleep(1000);

        waitUntilPageLoadsCustom("p", "ng-bind", "noSlotsAvailableForMonthErrorMessage | translate");

        try {
            new WebDriverWait(webDriver, 8).until(webDriver -> !webDriver.getPageSource().contains("Loading calendar") || webDriver.getPageSource().contains("There is no availability at this time."));
        } catch (TimeoutException ex) {
            ex.printStackTrace();
            return new VaccineStatus(VaccineStatus.VaccineAvailability.UNAVAILABLE, loc, this);
        }

        if (webDriver.getPageSource().contains("There is no availability at this time.")) {
            return new VaccineStatus(VaccineStatus.VaccineAvailability.UNAVAILABLE, loc, this);
        }

        return new VaccineStatus(VaccineStatus.VaccineAvailability.AVAILABLE, loc, this, loc.getUrl(), "");
    }
}
