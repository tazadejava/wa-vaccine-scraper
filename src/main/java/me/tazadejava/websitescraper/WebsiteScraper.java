package me.tazadejava.websitescraper;

import me.tazadejava.vaccinescraper.VaccineLocation;
import me.tazadejava.vaccinescraper.VaccineStatus;
import org.openqa.selenium.By;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.util.List;

/**
 * General class, to be extended when scraping a particular website. Includes helper methods.
 * @author Darren Lim (darrenl@mit.edu)
 */
public abstract class WebsiteScraper {

    private static final int TIMEOUT_SECONDS = 6;
    protected WebDriver webDriver;

    /**
     * Webdriver should be obtained from a class that extends WebDriverClass
     * @param webDriver
     */
    public WebsiteScraper(WebDriver webDriver) {
        this.webDriver = webDriver;
    }

    public abstract String getRequiredURLPrefix();
    public abstract VaccineStatus checkForVaccineStatus(VaccineLocation loc) throws Exception;

    protected boolean waitUntilPageLoadsClass(String className) {
        return waitUntilPageLoadsClass(className, 0);
    }

    protected boolean waitUntilPageLoadsClass(String className, int largerThanAmount) {
        try {
            new WebDriverWait(webDriver, TIMEOUT_SECONDS).until(webDriver -> webDriver.findElements(By.className(className)).size() > largerThanAmount);
        } catch (TimeoutException ex) {
            return false;
        }

        return true;
    }

    protected boolean waitUntilPageLoadsId(String idName) {
        try {
            new WebDriverWait(webDriver, TIMEOUT_SECONDS).until(webDriver -> webDriver.findElements(By.id(idName)).size() > 0);
        } catch (TimeoutException ex) {
            return false;
        }

        return true;
    }

    protected boolean waitUntilPageLoadsCustom(String elementType, String parameter, String parameterValue) {
        try {
            new WebDriverWait(webDriver, TIMEOUT_SECONDS).until(webDriver -> webDriver.findElements(By.cssSelector(elementType + "[" + parameter + "='" + parameterValue + "']")).size() > 0);
        } catch (TimeoutException ex) {
            return false;
        }

        return true;
    }

    protected List<WebElement> getChildNodes(WebElement webElement) {
        return webElement.findElements(By.xpath("./*"));
    }

    protected List<WebElement> getChildNodesWithClass(WebElement webElement, String className) {
        return webElement.findElements(By.xpath("./*[contains(@class, '" + className + "')]"));
    }

    protected List<WebElement> getNestedChildNodesWithClass(WebElement webElement, String className) {
        return webElement.findElements(By.xpath(".//*[contains(@class, '" + className + "')]"));
    }

    protected WebElement getButtonWithText(String text) {
        List<WebElement> buttons = webDriver.findElements(By.tagName("button"));
        for(WebElement button : buttons) {
            if(button.getText().equals(text)) {
                return button;
            }
        }

        return null;
    }

    public String getVisiblePageText() {
        return webDriver.findElement(By.xpath("//*[contains(.,.)]")).getText();
    }
}
