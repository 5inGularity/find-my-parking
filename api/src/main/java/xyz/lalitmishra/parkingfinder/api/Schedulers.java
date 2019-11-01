package xyz.lalitmishra.parkingfinder.api;

import org.springframework.stereotype.Component;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

@Component
public class Schedulers {

    private ScheduledExecutorService subscriptionSchedule;

    public synchronized ScheduledExecutorService getSubscriptionSchedule() {
        if (subscriptionSchedule == null) {
            subscriptionSchedule = Executors.newSingleThreadScheduledExecutor();
        }
        return subscriptionSchedule;
    }
}
