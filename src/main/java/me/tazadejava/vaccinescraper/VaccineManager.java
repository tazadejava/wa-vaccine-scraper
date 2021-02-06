package me.tazadejava.vaccinescraper;

import com.google.gson.*;

import java.io.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Manages the scraping service overall
 * @author Darren Lim (darrenl@mit.edu)
 */
public class VaccineManager {

    public static final String SUBFOLDER = "vaccine-tracker-wa";
    public static final boolean USE_MULTIPLE_THREADS = true;
    public static final int VERSION_INT = 1;

    private List<String> proxiesList;
    private int proxyIndex;
    private long lastProxyScrapeTime;

    private HashMap<String, Integer> totalSecondsPerUrl = new HashMap<>();
    private HashMap<String, Integer> totalCallsPerUrl = new HashMap<>();

    private HashMap<VaccineLocation, List<AvailableVaccineLocation>> availableLocationsHistory = new HashMap<>();

    private List<VaccineStatus> lastAvailableStatuses = new ArrayList<>();

    private ExecutorService workerPool;
//    private List<VaccineStatusScraper> scraperPool;
    private HashMap<VaccineStatus.VaccineAvailability, Integer> availabilityCount = new HashMap<>();

    public VaccineManager() {
//        scrapeProxiesList(); //TEMP DISABLED; does not work
        proxiesList = new ArrayList<>();
        proxiesList.add("");

        loadData();
        loadWorkers();
    }

    private void loadData() {
        try {
            File saveFile = new File("internalData/data.json");

            if(!saveFile.exists()) {
                return;
            }

            Gson gson = new Gson();

            FileReader reader = new FileReader(saveFile);

            JsonObject data = gson.fromJson(reader, JsonObject.class);

            if(data.has("totalSecondsPerUrl")) {
                JsonObject totalSeconds = data.get("totalSecondsPerUrl").getAsJsonObject();
                for (String key : totalSeconds.keySet()) {
                    totalSecondsPerUrl.put(key, totalSeconds.get(key).getAsInt());
                }
            }

            if(data.has("totalCallsPerUrl")) {
                JsonObject totalCalls = data.get("totalCallsPerUrl").getAsJsonObject();
                for (String key : totalCalls.keySet()) {
                    totalCallsPerUrl.put(key, totalCalls.get(key).getAsInt());
                }
            }

            if(data.has("locHistory")) {
                VaccineLocationScraper locationScraper = new VaccineLocationScraper(proxiesList.get(proxyIndex % proxiesList.size()));
                List<VaccineLocation> locations = locationScraper.getAllVaccineLocations();
                locationScraper.close();

                HashMap<String, VaccineLocation> stringToLocation = new HashMap<>();
                for(VaccineLocation loc : locations) {
                    stringToLocation.put(loc.toString(), loc);
                }

                JsonObject locHistory = data.get("locHistory").getAsJsonObject();

                for (String key : locHistory.keySet()) {
                    if (!stringToLocation.containsKey(key)) {
                        continue;
                    }
                    List<AvailableVaccineLocation> details = new ArrayList<>();

                    JsonArray availableArray = locHistory.get(key).getAsJsonArray();
                    for (JsonElement element : availableArray) {
                        details.add(new AvailableVaccineLocation(element.getAsJsonObject()));
                    }

                    availableLocationsHistory.put(stringToLocation.get(key), details);
                }
            }

            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private File getVaccineHistoryFile() {
        return new File(VaccineManager.SUBFOLDER + "/vaccine_history.json");
    }

    private void saveData() {
        try {
            //save internal data used for optimizing worker thread times
            File saveFile = new File("internalData/data.json");

            if(!saveFile.exists()) {
                saveFile.getParentFile().mkdirs();
                saveFile.createNewFile();
            }

            Gson gson = new GsonBuilder().setPrettyPrinting().create();

            FileWriter writer = new FileWriter(saveFile);

            JsonObject data = new JsonObject();

            data.add("totalSecondsPerUrl", gson.toJsonTree(totalSecondsPerUrl));
            data.add("totalCallsPerUrl", gson.toJsonTree(totalCallsPerUrl));

            gson.toJson(data, writer);

            writer.close();

            //save vaccine history
            writer = new FileWriter(getVaccineHistoryFile());
            data = new JsonObject();

            JsonObject availableHistory = new JsonObject();
            for(VaccineLocation loc : availableLocationsHistory.keySet()) {
                JsonArray locHistory = new JsonArray();

                for(AvailableVaccineLocation locData : availableLocationsHistory.get(loc)) {
                    locHistory.add(locData.save());
                }

                availableHistory.add(loc.toString(), locHistory);
            }

            data.add("locHistory", availableHistory);

            gson.toJson(data, writer);
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void scrapeProxiesList() {
        ProxyScraper proxies = new ProxyScraper();

        proxiesList = proxies.scrapeProxies();
        Collections.shuffle(proxiesList);
        proxyIndex = 0;

        proxies.close();

        lastProxyScrapeTime = System.currentTimeMillis();
    }

    private void saveAllStatuses(List<VaccineStatus> statuses) {
        //sort by available first, then unavailable
        statuses.sort(new Comparator<VaccineStatus>() {
            @Override
            public int compare(VaccineStatus o1, VaccineStatus o2) {
                return Integer.compare(o1.getAvailability().value, o2.getAvailability().value);
            }
        });

        Gson gson = new GsonBuilder().setPrettyPrinting().create();

        File locFile = getVaccineStatusesFile();
        try {
            if(!locFile.exists()) {
                locFile.getParentFile().mkdirs();
                locFile.createNewFile();
            }

            FileWriter writer = new FileWriter(locFile);
            JsonObject data = new JsonObject();

            data.addProperty("lastScrapeUpdate", LocalDateTime.now().toString());

            JsonArray statusArray = new JsonArray();
            for(VaccineStatus status : statuses) {
                statusArray.add(status.save(gson));
            }

            data.add("statuses", statusArray);

            gson.toJson(data, writer);

            writer.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        VaccineManager.updateLastUpdateTimeFile();
    }

    private File getVaccineStatusesFile() {
        return new File(SUBFOLDER + "/vaccine_statuses.json");
    }

    /**
     * Starts a service that runs every few minutes until the program is terminated. Will check for updates in all websites.
     */
    public void startScrapeUpdateService() {
        System.out.println("START SCRAPING SERVICE! VERSION NUMBER: " + VERSION_INT);
        try {
            while(true) {
                if(System.currentTimeMillis() - lastProxyScrapeTime >= 1000 * 60 * 10) {
//                    System.out.println("Updating proxies list...");
//                    scrapeProxiesList();
                }

                scrapeAllStatuses();
                updateFileServer();
                saveData();

                System.out.println("Done scraping! Now wait 1 minute before repeating.");
                for(int i = 0; i < 2; i++) {
                    Thread.sleep(1000 * 30); //sleep in increments of 30 seconds so that status checks can be made
                    System.out.println("Sleeping... " + ((i + 1) / 2d * 100d) + "%");

                    if(!lastAvailableStatuses.isEmpty()) {
                        System.out.println("Check the appointments that are currently available...");

                        List<VaccineLocation> locs = new ArrayList<>();

                        for(VaccineStatus status : lastAvailableStatuses) {
                            locs.add(status.getLocation());
                        }

                        scrapeStatusesAndMarkUnavailableStatuses(locs);
                        updateFileServer();
                        saveData();
                    }
                }
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void sortLocationsByTimeCommitment(List<VaccineLocation> locations, int processors) {
        if(totalCallsPerUrl.isEmpty()) {
            Collections.shuffle(locations);
        } else {
            HashMap<String, Integer> averageTimes = new HashMap<>();

            for(String key : totalCallsPerUrl.keySet()) {
                averageTimes.put(key, totalSecondsPerUrl.get(key) / totalCallsPerUrl.get(key));
            }

            HashMap<VaccineLocation, Integer> locationTimes = new HashMap<>();

            for(VaccineLocation loc : locations) {
                boolean found = false;
                for(String key : totalCallsPerUrl.keySet()) {
                    if(loc.getUrl().startsWith(key)) {
                        found = true;
                        locationTimes.put(loc, averageTimes.get(key));
                        break;
                    }
                }

                if(!found) {
                    locationTimes.put(loc, 0);
                }
            }

            //sort by smallest to largest commitment
            locations.sort(new Comparator<VaccineLocation>() {
                @Override
                public int compare(VaccineLocation o1, VaccineLocation o2) {
                    return Integer.compare(locationTimes.get(o2), locationTimes.get(o1));
                }
            });

            //place into processor number of slots, then merge after
            List<LinkedList<VaccineLocation>> slots = new ArrayList<>();
            int[] timeEstimates = new int[processors];
            for(int i = 0; i < processors; i++) {
                slots.add(new LinkedList<>());
            }

            int index = 0;
            for(VaccineLocation loc : locations) {
                slots.get(index % processors).add(loc);
                timeEstimates[index % processors] += locationTimes.get(loc);
                index++;
            }

            //apply

            locations.clear();
            int count = 0;
            for(LinkedList<VaccineLocation> slot : slots) {
                System.out.println("TIME ESTIMATES: WORKER " + count + " - " + timeEstimates[count] + " SECONDS");
                locations.addAll(slot);
                count++;
            }
        }
    }

    private void scrapeAllStatuses() {
        int processors = USE_MULTIPLE_THREADS ? Runtime.getRuntime().availableProcessors() : 1;

        VaccineLocationScraper locationScraper = new VaccineLocationScraper(proxiesList.get(proxyIndex % proxiesList.size()));
        proxyIndex++;
        List<VaccineLocation> locations = locationScraper.getAllVaccineLocations();
        locations.removeIf(loc -> loc.getUrl().isEmpty());
        locationScraper.close();

        //get a unique shuffle based on the number of processors
        sortLocationsByTimeCommitment(locations, processors);

        scrapeStatusesAndMarkUnavailableStatuses(locations);
    }

    private void scrapeStatusesAndMarkUnavailableStatuses(List<VaccineLocation> locations) {
        List<VaccineStatus> oldStatuses = new ArrayList<>(lastAvailableStatuses);

        lastAvailableStatuses.clear();

        scrapeStatuses(locations);

        //mark the unavailable statuses
        for(VaccineStatus status : oldStatuses) {
            if(!lastAvailableStatuses.contains(status)) {
                VaccineLocation loc = status.getLocation();
                availableLocationsHistory.get(loc).get(availableLocationsHistory.get(loc).size() - 1).markUnavailable();
            }
        }
    }

    private void loadWorkers() {
        availabilityCount.clear();
//        scraperPool = new ArrayList<>();

        int processors = USE_MULTIPLE_THREADS ? Runtime.getRuntime().availableProcessors() : 1;
        workerPool = Executors.newFixedThreadPool(processors);

//        System.out.println("CREATING " + processors + " WORKER WEBDRIVERS");
//        for(int i = 0; i < processors; i++) {
//            scraperPool.add(new VaccineStatusScraper(proxiesList.get(proxyIndex % proxiesList.size())));
//            proxyIndex++;
//        }
    }

    private void scrapeStatuses(List<VaccineLocation> locations) {
        long startTime = System.currentTimeMillis();
        System.out.println("START SCRAPING!");

        int processors;
        if(!USE_MULTIPLE_THREADS) {
            processors = 1;
        } else if(locations.size() < Runtime.getRuntime().availableProcessors()) {
            processors = locations.size();
        } else {
            processors = Runtime.getRuntime().availableProcessors();
        }

        System.out.println("WILL BE SCRAPING " + locations.size() + " LOCATIONS");

        if(processors == 1) {
            System.out.println("THERE IS 1 AVAILABLE WORKER");
        } else {
            System.out.println("THERE ARE " + processors + " AVAILABLE WORKERS");
        }

        List<VaccineStatus> statuses = new ArrayList<>();
        int splitSize = locations.size() / processors;

        int[] finishedProcessors = new int[] {0};

        availabilityCount.clear();
        availabilityCount.put(VaccineStatus.VaccineAvailability.AVAILABLE, 0);
        availabilityCount.put(VaccineStatus.VaccineAvailability.UNAVAILABLE, 0);
        availabilityCount.put(VaccineStatus.VaccineAvailability.UNKNOWN, 0);

        List<Callable<String>> tasks = new ArrayList<>();

        for(int i = 0; i < processors; i++) {
            int startIndex = i * splitSize;
            int endIndex;
            if(i == processors - 1) {
                endIndex = locations.size();
            } else {
                endIndex = (i + 1) * splitSize;
            }

            int processorNumber = i;

            tasks.add(new Callable<String>() {
                @Override
                public String call() throws Exception {
                    //wait before starting
                    try {
                        Thread.sleep((processorNumber * 200) + (int) (Math.random() * 400));
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    System.out.println("[WORKER " + processorNumber + "] STARTED WORKING!");
                    long startTimeThread = System.currentTimeMillis();
                    VaccineStatusScraper scraper = new VaccineStatusScraper(proxiesList.get(proxyIndex % proxiesList.size()));

                    for (int j = startIndex; j < endIndex; j++) {
                        VaccineLocation loc = locations.get(j);

                        long startTime = System.currentTimeMillis();
                        VaccineStatus status = scraper.scrapeVaccineStatus(loc);

                        availabilityCount.put(status.getAvailability(), availabilityCount.get(status.getAvailability()) + 1);

                        if(status.getWebsiteScraperSource() != null) {
                            String key = status.getWebsiteScraperSource().getRequiredURLPrefix();

                            totalSecondsPerUrl.putIfAbsent(key, 0);
                            totalCallsPerUrl.putIfAbsent(key, 0);

                            totalSecondsPerUrl.put(key, totalSecondsPerUrl.get(key) + (int) ((System.currentTimeMillis() - startTime) / 1000));
                            totalCallsPerUrl.put(key, totalCallsPerUrl.get(key) + 1);
                        }

                        if(status.getAvailability() == VaccineStatus.VaccineAvailability.AVAILABLE) {
                            System.out.println("[WORKER " + processorNumber + "] " + "SITE: " + loc.getUrl());
                            System.out.println("[WORKER " + processorNumber + "] \t" + status.toString().replaceAll("\n", "\n\t"));

                            statuses.add(status);
                            lastAvailableStatuses.add(status);

                            availableLocationsHistory.putIfAbsent(loc, new ArrayList<>());
                            availableLocationsHistory.get(loc).add(new AvailableVaccineLocation());

                            //intermediate update
                            saveAllStatuses(statuses);
                            updateFileServer();
                        }

                        if(status.getAvailability() != VaccineStatus.VaccineAvailability.UNKNOWN) {
                            //wait before continuing
                            try {
                                Thread.sleep(300 + (int) (Math.random() * 200));
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                    }

                    scraper.close();

                    finishedProcessors[0]++;
                    System.out.println("[WORKER " + processorNumber + "] DONE (" + finishedProcessors[0] + "/" + processors + ")! TOOK " + ((System.currentTimeMillis() - startTimeThread) / 1000) + " SECONDS TO COMPLETION");

                    return null;
                }
            });

//            workerPool.execute(new Runnable() {
//                @Override
//                public void run() {
//                    long startTimeThread = System.currentTimeMillis();
//                    VaccineStatusScraper scraper = scraperPool.get(processorNumber);
//
//                    for (int j = startIndex; j < endIndex; j++) {
//                        VaccineLocation loc = locations.get(j);
//
//                        long startTime = System.currentTimeMillis();
//                        VaccineStatus status = scraper.scrapeVaccineStatus(loc);
//
//                        availabilityCount.put(status.getAvailability(), availabilityCount.get(status.getAvailability()) + 1);
//
//                        if(status.getWebsiteScraperSource() != null) {
//                            String key = status.getWebsiteScraperSource().getRequiredURLPrefix();
//
//                            totalSecondsPerUrl.putIfAbsent(key, 0);
//                            totalCallsPerUrl.putIfAbsent(key, 0);
//
//                            totalSecondsPerUrl.put(key, totalSecondsPerUrl.get(key) + (int) ((System.currentTimeMillis() - startTime) / 1000));
//                            totalCallsPerUrl.put(key, totalCallsPerUrl.get(key) + 1);
//                        }
//
//                        if(status.getAvailability() == VaccineStatus.VaccineAvailability.AVAILABLE) {
//                            System.out.println("[WORKER " + processorNumber + "] " + "SITE: " + loc.getUrl());
//                            System.out.println("[WORKER " + processorNumber + "] \t" + status.toString().replaceAll("\n", "\n\t"));
//
//                            statuses.add(status);
//                            lastAvailableStatuses.add(status);
//
//                            availableLocationsHistory.putIfAbsent(loc, new ArrayList<>());
//                            availableLocationsHistory.get(loc).add(new AvailableVaccineLocation());
//
//                            //intermediate update
//                            saveAllStatuses(statuses);
//                            updateFileServer();
//                        }
//
//                        if(status.getAvailability() != VaccineStatus.VaccineAvailability.UNKNOWN) {
//                            //wait before continuing
//                            try {
//                                Thread.sleep(300 + (int) (Math.random() * 200));
//                            } catch (InterruptedException e) {
//                                e.printStackTrace();
//                            }
//                        }
//                    }
//
////                    scraper.close();
//
//                    finishedProcessors[0]++;
//                    System.out.println("[WORKER " + processorNumber + "] DONE (" + finishedProcessors[0] + "/" + processors + ")! TOOK " + ((System.currentTimeMillis() - startTimeThread) / 1000) + " SECONDS TO COMPLETION");
//                }
//            });
        }

        try {
            workerPool.invokeAll(tasks);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        saveAllStatuses(statuses);

        System.out.println("DONE IN " + ((System.currentTimeMillis() - startTime) / 1000) + " SECONDS!");
        System.out.println("THERE'S " + availabilityCount.get(VaccineStatus.VaccineAvailability.AVAILABLE) + " AVAILABLE APPT(S)");
        System.out.println("THERE'S " + availabilityCount.get(VaccineStatus.VaccineAvailability.UNAVAILABLE) + " UNAVAILABLE APPT(S)");
        System.out.println("THERE'S " + availabilityCount.get(VaccineStatus.VaccineAvailability.UNKNOWN) + " UNKNOWN APPT(S)");
    }

    /**
     * portToXVM.sh SCRIPT REQUIRES LINUX and SSHPASS installation
     */
    private void updateFileServer() {
        System.out.println("UPDATING XVM SERVER");
        executeCommand("./portToXVM.sh");
    }

    private void executeCommand(String command) {
        try {
            Process p = Runtime.getRuntime().exec(command);

            p.waitFor();

            String line;

            BufferedReader error = new BufferedReader(new InputStreamReader(p.getErrorStream()));
            while((line = error.readLine()) != null){
                System.out.println(line);
            }
            error.close();

            BufferedReader input = new BufferedReader(new InputStreamReader(p.getInputStream()));
            while((line=input.readLine()) != null){
                System.out.println(line);
            }

            input.close();

            OutputStream outputStream = p.getOutputStream();
            PrintStream printStream = new PrintStream(outputStream);
            printStream.println();
            printStream.flush();
            printStream.close();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void updateLastUpdateTimeFile() {
        Gson gson = new Gson();
        try {
            File file = new File(VaccineManager.SUBFOLDER + "/last_update_stamp.json");
            FileWriter writer = new FileWriter(file);

            JsonObject data = new JsonObject();

            data.addProperty("time", LocalDateTime.now().toString());

            gson.toJson(data, writer);

            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
