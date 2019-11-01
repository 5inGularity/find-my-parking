package xyz.lalitmishra.parkingfinder.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import xyz.lalitmishra.parkingfinder.api.data.Reservation;
import xyz.lalitmishra.parkingfinder.api.data.Spot;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
public class SubscriptionsEnder {
    private static final Logger logger = LoggerFactory.getLogger(SubscriptionsEnder.class);

    @Autowired
    private Schedulers schedulers;

    @Autowired
    private SpotsRepository spots;

    @Autowired
    private ReservationsRepository reservations;

    private static ScheduledFuture<?> enderFuture;

    @EventListener({ContextRefreshedEvent.class})
    public void handleContextRefreshEvent(ContextRefreshedEvent ctxStartEvt) {
        logger.info("Context started");
        enderFuture = schedulers.getSubscriptionSchedule().schedule(
                new SubscriptionEnderRunnable(spots, reservations, schedulers), 1, TimeUnit.SECONDS);
    }

    @EventListener({ReservationChangeEvent.class})
    public void handleReservationChangeEvent(ReservationChangeEvent event) {
        if(!event.getReservation().getState().equals("ACTIVE")) {
            return;
        }
        synchronized (SubscriptionsEnder.class) {
            if (enderFuture == null || enderFuture.isDone()) {
                enderFuture = schedulers.getSubscriptionSchedule().schedule(
                        new SubscriptionEnderRunnable(spots, reservations, schedulers), 1, TimeUnit.MILLISECONDS);
            } else if (enderFuture.getDelay(TimeUnit.MILLISECONDS) >
                    event.getReservation().getSpot().getReservedTill().getTime()) {
                // new/change reservation is going to expire earlier. Cancel and restart the task.
                enderFuture.cancel(true);
                enderFuture = schedulers.getSubscriptionSchedule().schedule(
                        new SubscriptionEnderRunnable(spots, reservations, schedulers), 1, TimeUnit.MILLISECONDS);
            }
        }
    }

    @EventListener({ContextClosedEvent.class})
    public void handleContextClosedEvent(ContextClosedEvent evt) {
        if (enderFuture != null) {
            enderFuture.cancel(true);
        }
        schedulers.getSubscriptionSchedule().shutdownNow();
    }

    static class SubscriptionEnderRunnable implements Runnable {

        private SpotsRepository spots;
        private ReservationsRepository reservations;
        private Schedulers schedulers;

        SubscriptionEnderRunnable(SpotsRepository spots, ReservationsRepository reservations, Schedulers schedulers) {
            this.spots = spots;
            this.reservations = reservations;
            this.schedulers = schedulers;
        }

        @Override
        public void run() {
            List<Spot> resSpots = spots.findByStateOrderByReservedTillAsc("RESERVED");
            if (resSpots.isEmpty()) {
                return;
            }

            long now = System.currentTimeMillis();
            List<Long> spotsToFree = resSpots.stream().filter(s -> s.getReservedTill().getTime() <= now).map(Spot::getId)
                    .collect(Collectors.toList());
            freeSpots(spotsToFree);

            Optional<Spot> spot = resSpots.stream().filter(s -> s.getReservedTill().getTime() > now).findFirst();
            if (!spot.isPresent()) {
                return;
            }

            synchronized (SubscriptionsEnder.class) {
                logger.info("Will run next at {}, after {} ms",
                        spot.get().getReservedTill(),
                        spot.get().getReservedTill().getTime() - now);
                enderFuture = schedulers.getSubscriptionSchedule().schedule(
                        new SubscriptionEnderRunnable(spots, reservations, schedulers),
                        spot.get().getReservedTill().getTime() - now, TimeUnit.MILLISECONDS);
            }
        }

        private void freeSpots(List<Long> spotIds) {
            spotIds.forEach(spotId -> {
                try {
                    reservations.findBySpotId(spotId).stream().filter(r -> r.getState().equals("ACTIVE")).forEach(
                            (reservation) -> {
                                reservation.setState("ENDED");
                                reservations.save(reservation);

                                Spot spot = reservation.getSpot();
                                spot.setState("FREE");
                                spots.save(spot);
                                logger.info("Spot " + spot.getName() + " is now marked free");
                            }
                    );
                } catch(Exception e) {
                    logger.error("Exception freeing spot {}", spotId, e);
                }
            });
        }
    }
}
