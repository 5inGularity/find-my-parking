package xyz.lalitmishra.parkingfinder.api;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.http.*;
import org.springframework.util.MultiValueMap;
import org.springframework.web.util.UriComponentsBuilder;
import xyz.lalitmishra.parkingfinder.api.data.AddReservationInput;
import xyz.lalitmishra.parkingfinder.api.data.Reservation;
import xyz.lalitmishra.parkingfinder.api.data.Spot;

import java.util.Arrays;
import java.util.Date;

public class TestReservations extends TestAPI {

    @Test
    public void testReservationFilter() {
        Reservation[] reservations = template.getForObject(base() + "/users/1/reservations", Reservation[].class);
        Assert.assertEquals(3, reservations.length);

        reservations = template.getForObject(base() + "/users/1/reservations?state={state}", Reservation[].class, "ACTIVE");
        Assert.assertEquals(1, reservations.length);
        Assert.assertEquals("ACTIVE", reservations[0].getState());

        reservations = template.getForObject(base() + "/users/1/reservations?state={state}", Reservation[].class, "ENDED");
        Assert.assertEquals(1, reservations.length);
        Assert.assertEquals("ENDED", reservations[0].getState());

        reservations = template.getForObject(base() + "/users/1/reservations?state={state}", Reservation[].class, "CANCELLED");
        Assert.assertEquals(1, reservations.length);
        Assert.assertEquals("CANCELLED", reservations[0].getState());
    }

    @Test
    public void testErrorOnAlreadyReserved() {
        AddReservationInput input = new AddReservationInput();
        input.setSpotId(3);
        input.setDurationMinutes(60);

        ResponseEntity<Reservation> resp = template.postForEntity(base() + "/users/1/reservations",
                input, Reservation.class);

        Assert.assertEquals(HttpStatus.BAD_REQUEST, resp.getStatusCode());
    }

    @Test
    public void testAddReservationSuccess() {
        AddReservationInput input = new AddReservationInput();
        input.setSpotId(1);
        input.setDurationMinutes(60);

        ResponseEntity<Reservation> resp = template.postForEntity(base() + "/users/1/reservations",
                input, Reservation.class);

        Reservation reservation = resp.getBody();
        Assert.assertEquals(1L, reservation.getUserId());
        Assert.assertEquals(reservation.getStart().getTime() + (reservation.getDurationMinutes() * 60 * 1000),
                reservation.getEnd().getTime());
        Assert.assertEquals("ACTIVE", reservation.getState());

        ResponseEntity<Spot[]> spotResp = template.exchange(base() + "/spots?lat={lat}&lon={lon}&radius={radius}",
                HttpMethod.GET, entity(), Spot[].class,
                18.9219841,
                72.8324656,
                2);

        Spot spot1 = Arrays.stream(spotResp.getBody()).filter(spot -> spot.getId() == input.getSpotId()).
                findFirst().
                orElseThrow(() -> new AssertionError("Expected to find spot with id " + input.getSpotId()));
        Assert.assertEquals("RESERVED", spot1.getState());
    }

    @Test
    public void testExtendReservation() {

        ResponseEntity<Spot[]> resp = template.exchange(base() + "/spots?lat={lat}&lon={lon}&radius={radius}",
                HttpMethod.GET, entity(), Spot[].class,
                18.9219841,
                72.8324656,
                1500);
        Spot freeSpot = Arrays.stream(resp.getBody()).filter(spot -> spot.getState().equals("FREE"))
                .findAny().orElseThrow(() -> new AssertionError("Expecting at least one free spot"));

        AddReservationInput input = new AddReservationInput();
        input.setSpotId(freeSpot.getId());
        input.setDurationMinutes(60);

        ResponseEntity<Reservation> resResp = template.postForEntity(base() + "/users/1/reservations",
                input, Reservation.class);
        Assert.assertEquals(HttpStatus.OK, resResp.getStatusCode());

        Reservation activeReservation = resResp.getBody();
        Date end = activeReservation.getEnd();
        int oldCost = activeReservation.getCost();

        activeReservation.setDurationMinutes(activeReservation.getDurationMinutes() + 60);
        activeReservation.setState(null);   // to not indicate that we want to change the state
        MultiValueMap<String, String> headers = new HttpHeaders();
        headers.add("Content-Type", "application/json");
        headers.add("Accept", "application/json");
        RequestEntity req = new RequestEntity<Reservation>(
                activeReservation,
                headers,
                HttpMethod.PUT,
                UriComponentsBuilder.fromHttpUrl(
                        base() + "/users/1/reservations/" + activeReservation.getId()).build().toUri()
        );
        resResp = template.exchange(req, Reservation.class);

        Assert.assertEquals(HttpStatus.OK, resp.getStatusCode());

        Reservation reservation = Arrays.stream(template.getForObject(base() + "/users/1/reservations", Reservation[].class))
                .filter(res -> res.getId() == activeReservation.getId()).findAny().orElseThrow(
                        () -> new AssertionError("Expected to find at least reservation with id "
                                + activeReservation.getId())
                );

        Assert.assertEquals(end.getTime() + 3600 * 1000, reservation.getEnd().getTime());
        Assert.assertEquals(2 * oldCost, reservation.getCost());
    }

    @Test
    public void testCancelReservation() {
        Reservation activeReservation =
                Arrays.stream(template.getForObject(base() + "/users/1/reservations", Reservation[].class))
                        .filter(res -> res.getState().equals("ACTIVE")).findAny().orElseThrow(
                        () -> new AssertionError("Expected to find at least on active reservation")
                );
        activeReservation.setState("CANCELLED");
        MultiValueMap<String, String> headers = new HttpHeaders();
        headers.add("Content-Type", "application/json");
        headers.add("Accept", "application/json");
        RequestEntity req = new RequestEntity<Reservation>(
                activeReservation,
                headers,
                HttpMethod.PUT,
                UriComponentsBuilder.fromHttpUrl(
                        base() + "/users/1/reservations/" + activeReservation.getId()).build().toUri()
        );
        ResponseEntity<Reservation> resp = template.exchange(req, Reservation.class);
        Assert.assertEquals(HttpStatus.OK, resp.getStatusCode());

        Reservation reservation = Arrays.stream(template.getForObject(base() + "/users/1/reservations", Reservation[].class))
                .filter(res -> res.getId() == activeReservation.getId()).findAny().orElseThrow(
                        () -> new AssertionError("Expected to find at least reservation with id "
                                + activeReservation.getId())
                );

        Assert.assertEquals("CANCELLED", reservation.getState());

        // Spot should also get freed up after cancellation.
        ResponseEntity<Spot[]> spotResp = template.exchange(base() + "/spots?lat={lat}&lon={lon}&radius={radius}",
                HttpMethod.GET, entity(), Spot[].class,
                18.9219841,
                72.8324656,
                1500);
        Spot spot = Arrays.stream(spotResp.getBody()).filter(s -> s.getId() == reservation.getSpot().getId())
                .findAny().orElseThrow(() -> new AssertionError("Expecting at least one free spot"));

        Assert.assertEquals("FREE", spot.getState());
    }

    @Test
    public void testErrorOnUpdatingCancelledReservation() {
        Reservation cancelledReservation =
                Arrays.stream(template.getForObject(base() + "/users/1/reservations", Reservation[].class))
                        .filter(res -> res.getState().equals("CANCELLED")).findAny().orElseThrow(
                        () -> new AssertionError("Expected to find at least on cancelled reservation")
                );
        cancelledReservation.setState("CANCELLED");

        MultiValueMap<String, String> headers = new HttpHeaders();
        headers.add("Content-Type", "application/json");
        headers.add("Accept", "application/json");
        RequestEntity req = new RequestEntity<Reservation>(
                cancelledReservation,
                headers,
                HttpMethod.PUT,
                UriComponentsBuilder.fromHttpUrl(
                        base() + "/users/1/reservations/" + cancelledReservation.getId()).build().toUri()
        );
        ResponseEntity<Reservation> resp = template.exchange(req, Reservation.class);
        Assert.assertEquals(HttpStatus.BAD_REQUEST, resp.getStatusCode());
    }
}
