package fr.enissay;

import fr.enissay.proxy.scraper.SourceHandler;
import fr.enissay.runnable.RequestRunnable;
import fr.enissay.utils.Logger;

import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.Date;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class DiscordGen {

    public static int tries = 1;
    public static int invalid, valid, rate = 0;

    private static ScheduledExecutorService scheduledExecutorService;
    private static ExecutorService spamDelayService;

    public static void handleSpam(final int requests){
        final int reqAmount = requests;
        final AtomicLong delay = new AtomicLong(1000L * 1); // without proxies 13 is recommended / with proxies idk

        CompletableFuture.runAsync(() -> {
            RequestRunnable requestRunnable = new RequestRunnable(reqAmount, delay);
            requestRunnable.run();
        });
    }

    public static void main(String... args) {
        scheduledExecutorService = Executors.newScheduledThreadPool(1);
        spamDelayService = Executors.newFixedThreadPool(1);

        SourceHandler.loadSources();

        Runnable checkSpam = () -> {

            handleSpam(100);

            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("HH:mm:ss.SSSSS");
            Logger.logDebug("Generator started  @ " + simpleDateFormat.format(Date.from(Instant.now())));
            spamDelayService.shutdownNow();
        };

        Runnable calculateTime = () -> {

            spamDelayService.execute(checkSpam);
        };

        scheduledExecutorService.scheduleAtFixedRate(calculateTime, 0L, 1L, TimeUnit.SECONDS);

        Logger.logInfo("This is in beta");
        Logger.logWarning("If you get rate-limited you have to wait a specific moment before using this again");

    }
}
