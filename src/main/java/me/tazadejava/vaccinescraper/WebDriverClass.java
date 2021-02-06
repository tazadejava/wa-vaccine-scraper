package me.tazadejava.vaccinescraper;

import org.openqa.selenium.By;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeDriverService;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.util.Collections;
import java.util.List;

/**
 * Class to be implemented on; gives proper webDriver setup, and includes helper methods. Make sure to call close() after done with use.
 * @author Darren Lim (darrenl@mit.edu)
 */
public class WebDriverClass {

    private static final int TIMEOUT_SECONDS = 6;

//    protected FirefoxDriver webDriver;
    protected ChromeDriver webDriver;

    public WebDriverClass(String proxy) {
        String userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/88.0.4324.104 Safari/537.36";

        System.setProperty("webdriver.chrome.driver", "chromedriver");
        System.setProperty(ChromeDriverService.CHROME_DRIVER_SILENT_OUTPUT_PROPERTY, "true");

        ChromeOptions options = new ChromeOptions();
        options.addArguments("user-agent=" + userAgent);
        options.addArguments("--disable-extensions");
        options.addArguments("--headless");
        options.addArguments("--window-size=1400,800");
        options.addArguments("--disable-blink-features=AutomationControlled");

        //currently does not work, since the ProxyScraper does not output correct proxies.
        if(proxy != null) { //this might be super super slow?
//            System.out.println("USING PROXY " + proxy);
//            Proxy proxyObject = new Proxy();
//            proxyObject.setHttpProxy(proxy);
//            proxyObject.setSslProxy(proxy);
//            options.setCapability("proxy", proxyObject);
//            options.setCapability("acceptInsecureCerts", true);
        }

        options.setExperimentalOption("excludeSwitches", Collections.singletonList("enable-automation"));
        options.setExperimentalOption("useAutomationExtension", false);

        webDriver = new ChromeDriver(options);

        //testing code to verify proxy works
//        System.out.println("TIME TO LOAD WHATISMYIP");
//        webDriver.get("http://www.whatismyip.com/my-ip-information/");
//        System.out.println(webDriver.getPageSource());

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void close() {
        webDriver.quit();
    }

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
}
