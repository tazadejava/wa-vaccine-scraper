package me.tazadejava.vaccinescraper;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Darren Lim (darrenl@mit.edu)
 */
public class ProxyScraper extends WebDriverClass {

    public ProxyScraper() {
        super(null);
    }

    /**
     * Scrapes proxies from a free proxy site. Proxy list is obtained successfully, but the proxies CURRENTLY DO NOT WORK.
     * @return
     */
    public List<String> scrapeProxies() {
        List<String> proxies = new ArrayList<>();

        webDriver.get("https://sslproxies.org");

        waitUntilPageLoadsClass("fa-clipboard");

        webDriver.findElement(By.className("fa-clipboard")).click();

        waitUntilPageLoadsClass("modal-dialog");

        WebElement dialogBox = getChildNodes(getChildNodes(webDriver.findElement(By.className("modal-dialog"))).get(0)).get(1);

        String[] ips = getChildNodes(dialogBox).get(0).getAttribute("value").split("\n");

        for(int i = 3; i < ips.length; i++) {
            proxies.add(ips[i]);
        }

        return proxies;
    }
}
