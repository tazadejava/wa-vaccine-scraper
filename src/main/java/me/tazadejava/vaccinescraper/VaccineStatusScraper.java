package me.tazadejava.vaccinescraper;

import me.tazadejava.websitescraper.CostcoScraper;
import me.tazadejava.websitescraper.QFCScraper;
import me.tazadejava.websitescraper.SafewayScraper;
import me.tazadejava.websitescraper.WebsiteScraper;

import java.util.ArrayList;
import java.util.List;

/**
 * Scrapes websites, using custom scrapers defined in constructor according to which website is scraped
 * @author Darren Lim (darrenl@mit.edu)
 */
public class VaccineStatusScraper extends WebDriverClass {

    private List<WebsiteScraper> scrapers;

    public VaccineStatusScraper(String proxy) {
        super(proxy);

        scrapers = new ArrayList<>();

        scrapers.add(new QFCScraper(webDriver));
        scrapers.add(new SafewayScraper(webDriver));
        scrapers.add(new CostcoScraper(webDriver));
    }

    public VaccineStatus scrapeVaccineStatus(VaccineLocation loc) {
        if(webDriver == null) {
            throw new NullPointerException();
        }

        String url = loc.getUrl();

        try {
            for(WebsiteScraper scraper : scrapers) {
                if(url.startsWith(scraper.getRequiredURLPrefix())) {
                    return scraper.checkForVaccineStatus(loc);
                }
            }

            return new VaccineStatus(VaccineStatus.VaccineAvailability.UNKNOWN, loc, null);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return new VaccineStatus(VaccineStatus.VaccineAvailability.UNAVAILABLE, loc, null);
    }
}
