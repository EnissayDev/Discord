package fr.enissay.proxy.scraper;

import fr.enissay.proxy.ProxyHandler;
import fr.enissay.proxy.ProxyVerifier;
import fr.enissay.utils.Logger;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SourceHandler {
    public static final ArrayList<String> scrapedProxyList = new ArrayList();
    public static final ArrayList<String> badSources = new ArrayList();

    public static void loadSources() {
        File file = new File("src/main/resources/proxysources.txt");
        ArrayList<String> sourceList = new ArrayList<String>();
        if (file.exists()) {

            try {
                BufferedReader bufferedReader = new BufferedReader(new FileReader(file));
                int proxyAmount = 0;
                String sourceString = bufferedReader.readLine();

                while (sourceString != null) {
                    sourceList.add(sourceString);
                    proxyAmount++;
                    sourceString = bufferedReader.readLine();
                }

                if (proxyAmount == 0) {
                    Logger.logWarning("Couldn't find any sources.");
                }

                handleScraping(sourceList);
            }
            catch (IOException ex) {
                Logger.logError("IOException occurred while loading sources.", ex);
            }
        } else {

            Logger.logError("Couldn't find proxysources.txt, cancelling verification...");
        }
    }


    public static void handleScraping(ArrayList<String> sourceList) {
        File scrapedProxiesfile = new File("src/main/resources/proxies.txt");
        if (scrapedProxiesfile.exists() &&
                scrapedProxiesfile.delete()) {
            try {
                if (!scrapedProxiesfile.createNewFile()) {
                    Logger.logError("Couldn't create proxies.txt, cancelling process.");
                    return;
                }
            } catch (IOException e) {
                Logger.logError("Couldn't create proxies.txt, cancelling process. IOException occurred.");

                return;
            }
        }

        CountDownLatch countDownLatch = new CountDownLatch(sourceList.size());

        ExecutorService executorService = Executors.newFixedThreadPool(sourceList.size());
        Instant instantBefore = Instant.now();
        sourceList.forEach(source -> executorService.execute(new SourceScraperThread(source, countDownLatch)));

        try {
            countDownLatch.await();
            Instant instantAfter = Instant.now();

            if (scrapedProxyList.size() == 0) {
                Logger.logError("Couldn't scrape any proxies.");
                return;
            }
            Logger.logInfo("Time taken: " + (instantAfter.getEpochSecond() - instantBefore.getEpochSecond()) + "s | Proxies: " + scrapedProxyList.size());
            Logger.logSuccess("Finished scraping, saving proxies to proxies.txt.");
            FileWriter fileWriter = new FileWriter(scrapedProxiesfile);
            scrapedProxyList.forEach(proxy -> {
                try {
                    fileWriter.write(proxy + "\n");
                } catch (IOException ex) {
                    Logger.logError("IOException error while saving scraped proxies.", ex);
                }
            });
            Logger.logSuccess("Saved working proxies to proxies.txt");
            fileWriter.close();

            Logger.logInput("Remove bad sources (y/n or enter for yes) -> ");
            String choice = (new Scanner(System.in)).nextLine();
            if (choice.length() > 0 &&
                    choice.startsWith("n")) {
                return;
            }

            ProxyHandler.loadProxies(true);
            removeBadSources();
        }
        catch (IOException ex) {
            Logger.logError("IOException occurred doing something.", ex);
        } catch (InterruptedException ex) {
            Logger.logError("InterruptedException occurred doing something.", ex);
        }
    }

    public static ArrayList<String> getScrapedProxyList() { return scrapedProxyList; }

    public static ArrayList<String> getBadSources() { return badSources; }

    private static void removeBadSources() {
        try {
            File sourcesFile = new File("src/main/resources/proxysources.txt");
            File tempSourcesFile = new File("src/main/resources/tempsources.txt");

            BufferedWriter writer = new BufferedWriter(new FileWriter(tempSourcesFile, true));

            BufferedReader reader = new BufferedReader(new FileReader(sourcesFile));

            String currentLine = reader.readLine();

            while (currentLine != null) {
                if (badSources.contains(currentLine))
                    continue;  writer.write(currentLine + currentLine);
                currentLine = reader.readLine();
            }

            writer.close();
            reader.close();

            if (sourcesFile.delete() &&
                    tempSourcesFile.renameTo(sourcesFile)) {
                Logger.logSuccess("Removed bad sources from proxysources.txt");

                return;
            }
            Logger.logError("Couldn't remove bad sources.");
        }
        catch (Exception ex) {
            Logger.logError("Error occurred while saving files.", ex);
        }
    }
}