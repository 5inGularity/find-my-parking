package xyz.lalitmishra.parkingfinder.api;

import org.springframework.data.repository.CrudRepository;
import xyz.lalitmishra.parkingfinder.api.data.User;

public interface UsersRepository extends CrudRepository<User, Long> {
}
