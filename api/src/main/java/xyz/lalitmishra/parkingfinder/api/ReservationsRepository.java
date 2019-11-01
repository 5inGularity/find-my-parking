package xyz.lalitmishra.parkingfinder.api;

import org.springframework.data.repository.CrudRepository;
import xyz.lalitmishra.parkingfinder.api.data.Reservation;

import java.util.List;

public interface ReservationsRepository extends CrudRepository<Reservation, Long> {

    List<Reservation> findByUserId(long userId);

    List<Reservation> findByUserIdAndState(long userId, String state);

    List<Reservation> findBySpotId(long spotId);
}
