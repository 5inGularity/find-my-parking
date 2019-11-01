package xyz.lalitmishra.parkingfinder.api.data;

import lombok.Data;

@Data
public class AddReservationInput {

    private long spotId;
    private int durationMinutes;
}
