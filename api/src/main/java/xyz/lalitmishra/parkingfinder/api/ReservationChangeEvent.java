package xyz.lalitmishra.parkingfinder.api;

import org.springframework.context.ApplicationEvent;
import xyz.lalitmishra.parkingfinder.api.data.Reservation;

public class ReservationChangeEvent extends ApplicationEvent {

    private Reservation reservation;

    /**
     * Create a new ApplicationEvent.
     *
     * @param source the object on which the event initially occurred (never {@code null})
     */
    public ReservationChangeEvent(Object source, Reservation reservation) {
        super(source);
        this.reservation = reservation;
    }

    public Reservation getReservation() {
        return reservation;
    }
}
