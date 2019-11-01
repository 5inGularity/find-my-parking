package xyz.lalitmishra.parkingfinder.api;

import com.google.common.collect.Lists;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import xyz.lalitmishra.parkingfinder.api.data.AddReservationInput;
import xyz.lalitmishra.parkingfinder.api.data.Reservation;
import xyz.lalitmishra.parkingfinder.api.data.Spot;
import xyz.lalitmishra.parkingfinder.api.data.User;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

@RestController
@Transactional
public class Controller {

    private static Logger logger = LoggerFactory.getLogger(Controller.class);

    @Autowired
    private UsersRepository users;
    @Autowired
    private SpotsRepository spots;
    @Autowired
    private ReservationsRepository reservations;
    @Autowired
    private ApplicationEventPublisher publisher;

    @RequestMapping("/users")
    public List<User> getUsers() {
        return Lists.newArrayList(users.findAll());
    }

    @RequestMapping("/spots")
    public List<Spot> getSpots(@RequestParam("lat") Double lat,
                               @RequestParam("lon") Double lon,
                               @RequestParam(value = "radius", defaultValue = "5") int radiusKM) {
        if (lat == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Parameter 'lat' is mandatory.");
        }
        if (lon == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Parameter 'lon' is mandatory.");
        }
        return spots.findByLocAndRadius(lat, lon, radiusKM);
    }

    @RequestMapping("/users/{userid}/reservations")
    public List<Reservation> getReservations(
            @PathVariable(value = "userid") long userId,
            @RequestParam(value = "state", defaultValue = "") String state) {
        // TODO: Validate that userId is same as logged in user.
        if (state.isEmpty()) {
            return Lists.newArrayList(reservations.findByUserId(userId));
        } else {
            return Lists.newArrayList(reservations.findByUserIdAndState(userId, state));
        }
    }

    @RequestMapping(value = "/users/{userid}/reservations", method = RequestMethod.POST,
            consumes = "application/json")
    public Reservation addReservation(
            @PathVariable(value = "userid") long userId,
            @RequestBody AddReservationInput input) {

        long now = Calendar.getInstance().getTimeInMillis();
        Date start = new Date(now);
        Date till = new Date(now + (input.getDurationMinutes() * 60 * 1000));
        int updated = spots.reserveSpot(input.getSpotId(), till);
        if (updated != 1) {
            logger.error("Invalid value {} for num of updated spots for id {}. Expecting exactly 1",
                    updated, input.getSpotId());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Spot has already been reserved!");
        }

        Spot spot = spots.findById(input.getSpotId()).orElseThrow(
                () -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid spotId"));

        Reservation reservation = new Reservation();
        reservation.setCost(input.getDurationMinutes() * spot.getRate());
        reservation.setSpot(spot);
        reservation.setStart(start);
        reservation.setEnd(till);
        reservation.setDurationMinutes(input.getDurationMinutes());
        reservation.setUserId(userId);
        reservation.setState("ACTIVE");

        try {
            return reservations.save(reservation);
        } finally {
            publisher.publishEvent(new ReservationChangeEvent(this, reservation));
        }
    }

    @RequestMapping(value = "/users/{userid}/reservations/{resid}", method = RequestMethod.PUT,
            consumes = "application/json", produces = "application/json")
    public Reservation updateReservation(
            @PathVariable("userid") long userId,
            @PathVariable("resid") long resId,
            @RequestBody Reservation input
    ) {
        Reservation reservation = reservations.findById(resId).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "No entry found for reservationId " + resId));

        if (!reservation.getState().equals("ACTIVE")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Can only update active reservations");
        }
        if (input.getState() != null && !input.getState().equals("CANCELLED")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid reservation state. Can only be updated to CANCELLED");
        }

        if (input.getDurationMinutes() != null) {
            logger.error("Current end {} duration {}", reservation.getEnd(), reservation.getDurationMinutes());
            reservation.setDurationMinutes(input.getDurationMinutes());
            reservation.setEnd(new Date(reservation.getStart().getTime() +
                    (reservation.getDurationMinutes() * 60 * 1000)));
            reservation.setCost(input.getDurationMinutes() * reservation.getSpot().getRate());
            logger.error("New end {} duration {}", reservation.getEnd(), reservation.getDurationMinutes());
        }
        if (input.getState() != null) {
            reservation.setState(input.getState());
            Spot spot = reservation.getSpot();
            spot.setState("FREE");
            spots.save(spot);
        }
        try {
            return reservations.save(reservation);
        } finally {
            publisher.publishEvent(new ReservationChangeEvent(this, reservation));
        }
    }

}
