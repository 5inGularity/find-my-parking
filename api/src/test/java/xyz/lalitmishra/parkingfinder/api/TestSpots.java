package xyz.lalitmishra.parkingfinder.api;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import xyz.lalitmishra.parkingfinder.api.data.Spot;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class TestSpots extends TestAPI {


    @Test
    public void testOneFreeSpotWithinRadius() {
        ResponseEntity<Spot[]> resp = template.exchange(base() + "/spots?lat={lat}&lon={lon}&radius={radius}",
                HttpMethod.GET, entity(), Spot[].class,
                18.9219841,
                72.8324656,
                2);
        List<Spot> freeSpots = Arrays.stream(resp.getBody()).filter(spot -> spot.getState().equals("FREE")).
                collect(Collectors.toList());
        Assert.assertEquals(1, freeSpots.size());
        Assert.assertEquals("Gateway of India", freeSpots.get(0).getName());

        List<Spot> reservedSpots = Arrays.stream(resp.getBody()).filter(spot -> spot.getState().equals("RESERVED")).
                collect(Collectors.toList());
        Assert.assertEquals(1, reservedSpots.size());
        Assert.assertEquals("Taj Hotel", reservedSpots.get(0).getName());
    }

    @Test
    public void testTwoFreeSpotWithinRadius() {
        ResponseEntity<Spot[]> resp = template.exchange(base() + "/spots?lat={lat}&lon={lon}&radius={radius}",
                HttpMethod.GET, entity(), Spot[].class,
                18.9219841,
                72.8324656,
                1500);
        List<Spot> freeSpots = Arrays.stream(resp.getBody()).filter(spot -> spot.getState().equals("FREE")).
                collect(Collectors.toList());
        Assert.assertEquals(2, freeSpots.size());
        Assert.assertEquals("Gateway of India", freeSpots.get(0).getName());
        Assert.assertEquals("India Gate", freeSpots.get(1).getName());

        List<Spot> reservedSpots = Arrays.stream(resp.getBody()).filter(spot -> spot.getState().equals("RESERVED")).
                collect(Collectors.toList());
        Assert.assertEquals(1, reservedSpots.size());
        Assert.assertEquals("Taj Hotel", reservedSpots.get(0).getName());
    }
}
