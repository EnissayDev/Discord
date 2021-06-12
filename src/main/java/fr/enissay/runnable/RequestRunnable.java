package fr.enissay.runnable;

import fr.enissay.DiscordGen;
import fr.enissay.proxy.Proxy;
import fr.enissay.proxy.ProxyHandler;
import fr.enissay.proxy.ProxyVerifier;
import fr.enissay.proxy.scraper.SourceHandler;
import fr.enissay.utils.DiscWebhook;
import org.json.JSONObject;
import fr.enissay.utils.Logger;
import fr.enissay.utils.Mode;

import java.net.InetSocketAddress;
import java.net.ProxySelector;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.Date;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

public class RequestRunnable implements Runnable{

    private int reqAmount;
    private AtomicLong delay;

    public RequestRunnable(int reqAmount, AtomicLong delay) {
        this.reqAmount = reqAmount;
        this.delay = delay;
    }

    @Override
    public void run() {
        String inputCode = Mode.getString(DiscordGen.getGeneratorType().getLength(), Mode.DISCORD);

        final ExecutorService executor = Executors.newFixedThreadPool(reqAmount);

        for (int i = 0; i < reqAmount; i++){
            try {
                /*Random random = new Random();
                int r = random.nextInt(ProxyVerifier.workingProxyList.size());

                Proxy proxy = ProxyVerifier.workingProxyList.get(r);*/
                HttpClient client = HttpClient.newBuilder()
                        .executor(executor)
                        //.proxy(ProxySelector.of(new InetSocketAddress(proxy.getIp(), proxy.getPort())))
                        .version(HttpClient.Version.HTTP_1_1)
                        .build();

                //Logger.logDebug("Choosed proxy: " + proxy.toString());

                inputCode = Mode.getString(DiscordGen.getGeneratorType().getLength(), Mode.DISCORD);

                final HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create("https://discordapp.com/api/v6/entitlements/gift-codes/" + inputCode))
                        //.header("Proxy-Authorization", "Basic " + encoded)
                        .timeout(Duration.ofSeconds(2))//OG: 5
                        .GET()
                        .build();

                String finalInputCode = inputCode;
                client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                        .thenApplyAsync(response -> {
                            Logger.logInfo(" status: " + response.statusCode());
                            Logger.logDebug(response.body());
                            final JSONObject obj = new JSONObject(response.body());

                            String status = "INVALID";

                            if (obj.has("sku_id")) {
                                status = "VALID";
                                DiscordGen.valid++;
                            }
                            else if (obj.has("message") && obj.getString("message").equalsIgnoreCase("Unknown Gift Code")){
                                status = "INVALID";
                                DiscordGen.invalid++;
                            }
                            else if (obj.has("message") && obj.getString("message").equalsIgnoreCase("You are being rate limited.")) {
                                status = "RATE-LIMIT";
                                DiscordGen.rate++;
                            }

                            if (status.equalsIgnoreCase("RATE-LIMIT")) {
                                Logger.logWarning("You just got rate limited. The thread will be paused. Time Left @ " + obj.getLong("retry_after") / 1000L + " seconds");
                                try {
                                    Thread.sleep(obj.getLong("retry_after")); // Pause
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                            }
                            else if (status.equalsIgnoreCase("VALID")) Logger.logSuccess("Got a valid gift code : https://discord.gift/" + finalInputCode + "/" + " @ TRIES: " + DiscordGen.tries + "/" + reqAmount + " @ REMAINING USES: " + (obj.getInt("max_uses") - obj.getInt("uses")) + " @ DESCRIPTION: " + obj.getJSONObject("promotion").getString("inbound_header_text"));
                            else Logger.logInvalid("Code not working :( | CODE: " + "https://discord.gift/" + finalInputCode + "/" + " @ TRIES: " + DiscordGen.tries + "/" + reqAmount);

                            final Request discRequest = new Request(DiscordGen.getGeneratorType(), Date.from(Instant.now()), status, "https://discord.gift/" + finalInputCode + "/");
                            RequestManager.add(discRequest);

                            new DiscWebhook("https://discordapp.com/api/webhooks/853039663610593301/klElZe20jAdeUjI4s-Fk3kPdjN1mEtNXwhBKz5pQkDUrGIWYOI1hbZYzs2Fk4O60RXae", discRequest);

                            if (DiscordGen.tries >= reqAmount) {
                                final double reqPerSec = 1000 / delay.get();

                                Logger.logInfo("Finished the Generator with :");
                                Logger.logInfo("Valid @ " + DiscordGen.valid);
                                Logger.logInfo("Invalid @ " + DiscordGen.invalid);
                                Logger.logInfo("Rate @ " + DiscordGen.rate);
                                Logger.logInfo("Request/Sec @ " + reqPerSec);
                                Logger.logInfo("Delay @ " + delay.get() / 1000 + " seconds");

                                System.exit(-1);
                            }
                            DiscordGen.tries++;
                            return response;
                        });
                Thread.sleep(delay.get());
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public int getReqAmount() {
        return reqAmount;
    }

    public AtomicLong getDelay() {
        return delay;
    }
}
