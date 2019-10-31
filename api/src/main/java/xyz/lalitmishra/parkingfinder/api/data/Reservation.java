package xyz.lalitmishra.parkingfinder.api.data;

import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.persistence.*;
import java.util.Date;

@Entity
@Table(name="reservations")
@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Reservation {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @EqualsAndHashCode.Include
    private long id;

    @ManyToOne
    @JoinColumn(name="spotid")
    private Spot spot;

    @Column(name="userid")
    private long userId;

    @Temporal(TemporalType.TIMESTAMP)
    private Date start;
    @Temporal(TemporalType.TIMESTAMP)
    private Date end;
    private int duration;
    private int cost;
}
