package xyz.lalitmishra.parkingfinder.api.data;

import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.persistence.*;
import java.util.Date;

@Entity
@Table(name = "spots")
@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Spot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private long id;

    @Embedded
    @AttributeOverrides(value = {
            @AttributeOverride(name = "latitude", column = @Column(name = "lat")),
            @AttributeOverride(name = "longitude", column = @Column(name = "lon"))
    }
    )
    private Location location;

    @Column(name = "spot_name")
    private String name;

    private String state;

    private int rate;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "reserved_till")
    private Date reservedTill;

}
