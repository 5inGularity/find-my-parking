package xyz.lalitmishra.parkingfinder.api;

import org.springframework.data.repository.CrudRepository;
import xyz.lalitmishra.parkingfinder.api.data.Reservation;

public interface ReservationsRepository extends CrudRepository<Reservation, Long> {
}
