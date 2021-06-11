package fr.enissay.proxy;

public class ProxySettings
{
    private static long timeout = 1000L;
    private static int threads = 1, batchSize = 500;


    public static void setThreads(int threads) { ProxySettings.threads = threads; }



    public static void setTimeout(long timeout) { ProxySettings.timeout = timeout; }



    public static long getTimeout() { return timeout; }



    public static int getThreads() { return threads; }



    public static void setBatchSize(int batchSize) { ProxySettings.batchSize = batchSize; }



    public static int getBatchSize() { return batchSize; }
}
