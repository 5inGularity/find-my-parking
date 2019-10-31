package xyz.lalitmishra.parkingfinder.api;

import org.springframework.data.repository.CrudRepository;
import xyz.lalitmishra.parkingfinder.api.data.Spot;

public interface SpotsRepository extends CrudRepository<Spot, Long> {
}
