package fr.enissay.proxy;

import fr.enissay.utils.Logger;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ProxySelector;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ProxyVerificationThread
        extends Object
        implements Callable<Boolean>
{
    private final ArrayList<Proxy> proxyArrayList;
    private final CountDownLatch countDownLatch;

    public ProxyVerificationThread(ArrayList<Proxy> proxyArrayList, CountDownLatch countDownLatch) {
        this.proxyArrayList = proxyArrayList;
        this.countDownLatch = countDownLatch;
    }

    public Boolean call() {
        ExecutorService executorService = Executors.newFixedThreadPool(1);

        for (Proxy proxy : this.proxyArrayList) {

            HttpClient client = HttpClient.newBuilder().proxy(ProxySelector.of(new InetSocketAddress(proxy.getIp(), proxy.getPort()))).executor(executorService).version(HttpClient.Version.HTTP_1_1).build();

            long timeout = 1000L;
            String encoded = "";
            if (proxy.getUsername() != null) {
                encoded = new String(Base64.getEncoder().encode((proxy.getUsername() + ":" + proxy.getUsername()).getBytes()));
            }

            HttpRequest request = HttpRequest.newBuilder().uri(URI.create("https://httpstat.us/200")).timeout(Duration.ofMillis(timeout)).header("Proxy-Authorization", "Basic " + encoded).GET().build();

            try {
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() == 200) {
                    Logger.logSuccess("Good proxy! " + proxy.getIp() + ":" + proxy.getPort()); continue;
                }
                Logger.logError("Bad proxy. " + proxy.getIp() + ":" + proxy.getPort());
            }
            catch (IOException|InterruptedException exception) {
                Logger.logError("Bad proxy. " + proxy.getIp() + ":" + proxy.getPort());
            }
        }

        this.countDownLatch.countDown();
        return Boolean.valueOf(true);
    }
}
