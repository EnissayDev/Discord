package fr.enissay.runnable;

import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

public class RequestManager {

    private static LinkedList<Request> requests = new LinkedList<>();

    public static void add(final Request request){
        requests.add(request);
    }

    public static void remove(final Request request){
        requests.remove(request);
    }

    public List<Request> getRequestsWith(final String status){
        return requests.stream().filter(request -> request.getStatus().equalsIgnoreCase(status)).collect(Collectors.toList());
    }

    public static LinkedList<Request> getRequests() {
        return requests;
    }
}
