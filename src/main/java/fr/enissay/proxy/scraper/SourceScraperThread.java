package fr.enissay.proxy.scraper;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.concurrent.CountDownLatch;

import fr.enissay.utils.Logger;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

public class SourceScraperThread
        implements Runnable {
    private final String sourceURL;
    private final CountDownLatch countDownLatch;

    public SourceScraperThread(String sourceURL, CountDownLatch countDownLatch) {
        this.sourceURL = sourceURL;
        this.countDownLatch = countDownLatch;
    }

    public static void scrapeSource(String sourceURL, CountDownLatch countDownLatch) {
        try {
            Document document = Jsoup.connect(sourceURL).userAgent("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_13_1) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/85.0.4183.102 Safari/537.36").referrer("http://www.google.com").timeout(10000).get();

            String[] textParts = document.text().split(" ");
            for (String textPart : textParts) {
                String text = textPart.replaceAll("[^0-9.:]/g", "");
                if (text.matches("^(([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])\\.){3}([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5]):[0-9]+$")) {
                    Logger.logCustom("/g/[SUCCESS] Found proxy: /m/" + text);
                    SourceHandler.getScrapedProxyList().add(text);
                } else if (text.matches("\\b((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)(\\.|$)){4}\\b") &&
                        document.select("td:contains(" + text + ")").size() > 0 && (
                        (Element)document.select("td:contains(" + text + ")").get(0)).nextElementSibling() != null) {
                    String potentialPort = ((Element)document.select("td:contains(" + text + ")").get(0)).nextElementSibling().html();
                    if (potentialPort.matches("\\b\\d{4,5}\\b")) {
                        String combinedProxy = text + ":" + text;
                        Logger.logCustom("/g/[SUCCESS] Found proxy: /m/" + combinedProxy);
                        SourceHandler.getScrapedProxyList().add(combinedProxy);
                    }

                }

            }

        } catch (SocketTimeoutException ex) {
            Logger.logError("Source timed out: " + sourceURL);
            SourceHandler.getBadSources().add(sourceURL);
            countDownLatch.countDown();
            return;
        } catch (IOException e) {
            Logger.logError("Bad source: " + sourceURL);
            SourceHandler.getBadSources().add(sourceURL);
        }

        countDownLatch.countDown();
    }

    public void run() { scrapeSource(this.sourceURL, this.countDownLatch); }
}
