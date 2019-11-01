package xyz.lalitmishra.parkingfinder.api;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import xyz.lalitmishra.parkingfinder.api.data.Spot;

import java.util.Date;
import java.util.List;

public interface SpotsRepository extends CrudRepository<Spot, Long> {

    /*
    Using a combination of haversine formula and boxing to optimize the query.
     */
    @Query(value = "select id, lat, lon, state, rate, reserved_till, spot_name, distance " +
            "from (select s.id, s.lat, s.lon, s.state, s.rate, s.reserved_till, s.spot_name, " +
            "111.045 " +
            "* DEGREES(ACOS(COS(RADIANS(?1)) " +
            "* COS(RADIANS(s.lat)) " +
            "* COS(RADIANS(?2 - s.lon)) " +
            "+ SIN(RADIANS(?1)) " +
            "* SIN(RADIANS(s.lat)))) AS distance " +
            "from spots as s " +
            "where " +
            "s.lat between ?1 - (?3 / 111.045) and ?1 + (?3 / 111.045) " +
            "and " +
            "s.lon between ?2 - (?3 / (111.045 * COS(RADIANS(?1)))) and " +
            "?2 + (?3 / (111.045 * COS(RADIANS(?1)))) " +
            ") as d " +
            "where distance <= ?3 " +
            "order by distance",
            nativeQuery = true)
    List<Spot> findByLocAndRadius(double lat, double lon, int radius);


    @Modifying
    @Query("update Spot set state='RESERVED', reserved_till=?2 where state='FREE' and id = ?1")
    int reserveSpot(long id, Date till);

    List<Spot> findByStateOrderByReservedTillAsc(String state);
}
