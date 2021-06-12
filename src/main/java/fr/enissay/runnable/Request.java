package fr.enissay.runnable;

import java.util.Date;

public class Request {

    private GeneratorType generatorType;
    private Date time;
    private String status, link;

    public Request(GeneratorType generatorType, Date time, String status, String link) {
        this.generatorType = generatorType;
        this.time = time;
        this.status = status;
        this.link = link;
    }

    public GeneratorType getGeneratorType() {
        return generatorType;
    }

    public Date getTime() {
        return time;
    }

    public String getStatus() {
        return status;
    }

    public String getLink() {
        return link;
    }
}
