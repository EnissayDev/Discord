package fr.enissay.proxy;

import fr.enissay.utils.Logger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

public class ProxyVerifier
{
    public static List<Proxy> workingProxyList = Collections.synchronizedList(new ArrayList());
    public static void verifyProxies() {
        ThreadPoolExecutor executor;
        CountDownLatch countDownLatch;
        int proxyAmount = ProxyHandler.proxyList.size();
        int threadAmount = ProxySettings.getThreads();

        List<ProxyVerificationThread> checkerThreads = Collections.synchronizedList(new ArrayList());

        if (proxyAmount == threadAmount || threadAmount > proxyAmount) {
            ArrayList<Proxy> proxyArrayList = new ArrayList<Proxy>();
            for (int i = 0; i < proxyAmount; i++) {
                proxyArrayList.add((Proxy)ProxyHandler.proxyList.get(i));
            }
            countDownLatch = new CountDownLatch(1);
            executor = (ThreadPoolExecutor)Executors.newFixedThreadPool(1);
            ProxyVerificationThread proxyVerificationThread = new ProxyVerificationThread(proxyArrayList, countDownLatch);
            checkerThreads.add(proxyVerificationThread);

            proxyArrayList.forEach(proxy ->  ProxyHandler.checkProxy(proxy, countDownLatch));
        } else {

            int proxyPerThread = proxyAmount / threadAmount;

            executor = (ThreadPoolExecutor)Executors.newFixedThreadPool(ProxySettings.getThreads());

            int batchCounter = 0;

            countDownLatch = new CountDownLatch(ProxySettings.getThreads());

            for (proxyAmount = 0; proxyAmount < threadAmount; proxyAmount++) {

                int begin = batchCounter * proxyPerThread;
                batchCounter++;
                int end = batchCounter * proxyPerThread;
                if (proxyAmount == threadAmount - 1) {
                    end = proxyAmount;
                }
                ArrayList<Proxy> proxyArrayList = new ArrayList<Proxy>(ProxyHandler.proxyList.subList(begin, end));
                ProxyVerificationThread proxyVerificationThread = new ProxyVerificationThread(proxyArrayList, countDownLatch);
                checkerThreads.add(proxyVerificationThread);

            }
            try {
                executor.setMaximumPoolSize(ProxySettings.getThreads());
                executor.setCorePoolSize(ProxySettings.getThreads());
                executor.invokeAll(checkerThreads);
            } catch (InterruptedException proxyCounter) {
                InterruptedException e; Logger.logError("InterruptedException while running checker threads.");
            }
        }

        try {
            countDownLatch.await();
            executor.shutdown();
            Logger.logInfo("Finished checking.");
        } catch (InterruptedException e) {
            Logger.logError("InterruptedException while looping through proxies.", e);
        }
    }

    public static List<Proxy> getWorkingProxyList() { return workingProxyList; }
}
