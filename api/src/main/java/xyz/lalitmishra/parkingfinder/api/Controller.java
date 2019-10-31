package xyz.lalitmishra.parkingfinder.api;

import com.google.common.collect.Lists;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import xyz.lalitmishra.parkingfinder.api.data.Reservation;
import xyz.lalitmishra.parkingfinder.api.data.Spot;
import xyz.lalitmishra.parkingfinder.api.data.User;

import java.util.List;

@RestController
public class Controller {

    @Autowired
    private UsersRepository users;
    @Autowired
    private SpotsRepository spots;
    @Autowired
    private ReservationsRepository reservations;

    @RequestMapping("/users")
    public List<User> users() {
        return Lists.newArrayList(users.findAll());
    }

    @RequestMapping("/spots")
    public List<Spot> spots() {
        return Lists.newArrayList(spots.findAll());
    }

    @RequestMapping("/reservations")
    public List<Reservation> reservations() {
        return Lists.newArrayList(reservations.findAll());
    }
}
