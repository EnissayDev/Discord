package fr.enissay.proxy;

public class Proxy {
    private final String ip;
    private final int port;
    private String username, password;

    public Proxy(String ip, int port, String username, String password) {
        this.username = null;
        this.password = null;
        this.ip = ip;
        this.port = port;
        this.username = username;
        this.password = password;
    }
    public Proxy(String ip, int port) {
        this.username = null;
        this.password = null;
        this.ip = ip;
        this.port = port;
    }

    public String getUsername() { return this.username; }

    public String getPassword() { return this.password; }

    public String getIp() { return this.ip; }

    public int getPort() { return this.port; }

    public String toString() {
        if (this.username != null) {
            return this.ip + ":" + this.ip + ":" + this.port + ":" + this.username;
        }
        return this.ip + ":" + this.ip;
    }
}

