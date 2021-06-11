package fr.enissay.proxy;

import fr.enissay.utils.Logger;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ProxySelector;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.concurrent.CountDownLatch;

public class ProxyHandler {
    public static ArrayList<Proxy> proxyList = new ArrayList();

    public static void loadProxies(boolean verifyProxies) {
        File file = new File("src/main/resources/proxies.txt");
        if (file.exists()) {
            try {
                BufferedReader bufferedReader = new BufferedReader(new FileReader(file));
                int proxyAmount = 0;
                String proxyString = bufferedReader.readLine();

                while (proxyString != null) {
                    if (proxyString.contains(":")) {
                        String[] proxyParts = proxyString.replaceAll(" ", "").split(":");
                        if (proxyParts[1].matches("^[0-9]*$")) {
                            Proxy proxy;
                            if (proxyParts.length == 4) {
                                String username = proxyParts[2], password = proxyParts[3];
                                proxy = new Proxy(proxyParts[0], Integer.parseInt(proxyParts[1]), username, password);
                            } else {
                                proxy = new Proxy(proxyParts[0], Integer.parseInt(proxyParts[1]));
                            }
                            Logger.logInfo("Found proxy: " + proxy.getIp());
                            proxyList.add(proxy);
                            proxyAmount++;
                        } else {

                            Logger.logWarning("Port is not an integer, removing proxy from pool...");
                        }
                    }

                    proxyString = bufferedReader.readLine();
                }

                if (proxyAmount == 0) {
                    if (!verifyProxies) {
                        Logger.logWarning("Couldn't find any proxies, will not use proxies.");
                    } else {
                        Logger.logWarning("Couldn't find any proxies.");
                    }

                    return;
                }
                if (verifyProxies)
                {
                    Instant instantBefore = Instant.now();
                    ProxyVerifier.verifyProxies();
                    Instant instantAfter = Instant.now();

                    long diff = instantAfter.getEpochSecond() - instantBefore.getEpochSecond();
                    long cpm = proxyList.size() / (1 * 60L);
                    Logger.logInfo("Time taken: " + (instantAfter.getEpochSecond() - instantBefore.getEpochSecond()) + "s | CPM: " + cpm + " | Hits: " + ProxyVerifier.workingProxyList.size());
                    if (ProxyVerifier.getWorkingProxyList().isEmpty()) {
                        Logger.logError("No working proxies found.");
                    } else {
                        Logger.logSuccess("Found working proxies! Saving them to proxies.txt");
                        File proxySaveFile = new File("src/main/resources/proxies.txt");
                        String fileName = "proxies.txt";
                        if (proxySaveFile.exists()) {
                            if (!proxySaveFile.delete()) {
                                Logger.logError("Couldn't delete proxies.txt, saving to workingproxies.txt");
                                fileName = "workingproxies.txt";
                            } else {
                                try {
                                    if (!proxySaveFile.createNewFile()) {
                                        Logger.logError("Couldn't create proxies.txt, saving to workingproxies.txt");
                                        fileName = "workingproxies.txt";
                                    }
                                } catch (IOException exception) {
                                    Logger.logError("IOexception occurred while creating proxies.txt", exception);
                                    fileName = "workingproxies.txt";
                                }
                            }
                        }

                        try {
                            FileWriter fileWriter = new FileWriter(fileName);
                            for (Proxy proxy : ProxyVerifier.getWorkingProxyList()) {
                                fileWriter.write(proxy.toString() + "\n");
                            }
                            fileWriter.close();
                            Logger.logSuccess("Saved proxies to " + fileName);
                        } catch (IOException exception) {
                            Logger.logError("IOException occurred while saving proxies.", exception);
                        }

                    }

                }

            } catch (IOException ex) {
                Logger.logError("IOException occurred while loading proxies.", ex);
            }

        } else if (!verifyProxies) {
            Logger.logError("Couldn't find proxies.txt, creating it, but will not use proxies.");
            try {
                boolean created = file.createNewFile();
                if (created) {
                    Logger.logSuccess("Created proxies.txt file, please fill it in");
                } else {
                    Logger.logError("Couldn't create proxies.txt file, please create it yourself then fill it in");
                }
            } catch (IOException ex) {
                Logger.logError("Couldn't create proxies.txt file, please create it yourself then fill it in");
            }
        } else {
            Logger.logError("Couldn't find proxies.txt, cancelling verification...");
        }
    }

    public static void checkProxy(Proxy proxy, CountDownLatch countDownLatch) {
        HttpClient client = HttpClient.newBuilder().proxy(ProxySelector.of(new InetSocketAddress(proxy.getIp(), proxy.getPort()))).version(HttpClient.Version.HTTP_1_1).build();

        long timeout = 1000L;
        String encoded = "";
        if (proxy.getUsername() != null) {
            encoded = new String(Base64.getEncoder().encode((proxy.getUsername() + ":" + proxy.getUsername()).getBytes()));
        }

        HttpRequest request = HttpRequest.newBuilder().uri(URI.create("https://httpstat.us/200")).timeout(Duration.ofMillis(timeout)).header("Proxy-Authorization", "Basic " + encoded).GET().build();

        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                Logger.logSuccess("Good proxy! " + proxy.getIp() + ":" + proxy.getPort());
                ProxyVerifier.workingProxyList.add(proxy);
            } else {
                Logger.logError("Bad proxy. " + proxy.getIp() + ":" + proxy.getPort());
            }
            countDownLatch.countDown();
        } catch (IOException|InterruptedException exception) {
            Logger.logError("Bad proxy. " + proxy.getIp() + ":" + proxy.getPort());
            countDownLatch.countDown();
        }
    }
}