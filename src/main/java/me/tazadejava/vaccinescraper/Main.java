package me.tazadejava.vaccinescraper;

/**
 * @author Darren Lim (darrenl@mit.edu)
 */
public class Main {

    public static void main(String[] args) throws InterruptedException {
        VaccineManager manager = new VaccineManager();
        manager.startScrapeUpdateService();
    }
}