package xyz.lalitmishra.parkingfinder.api.data;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import javax.persistence.*;

@Entity
@Table(name = "users")
@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private long id;
    @Column(name = "phone_number")
    private String phoneNumber;
    private boolean validated;
    @Column(name = "validation_code")
    private String validationCode;
    @Column(name = "password_hash")
    @JsonIgnore
    @ToString.Exclude
    private String passwordHash;
    @Column(name = "password_salt")
    @JsonIgnore
    @ToString.Exclude
    private String passwordSalt;
}
