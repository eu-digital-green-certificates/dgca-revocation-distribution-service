package europa.ec.dgc.revocationdistribution.entity;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.PostLoad;
import javax.persistence.Table;
import javax.persistence.Transient;
import lombok.Getter;
import org.springframework.data.annotation.Immutable;


@Getter
@Entity
@Table(name = "kid_view")
@Immutable
public class KidViewEntity {

     public static final List<String> TYPES_NAMES = new ArrayList<>(List.of( "UCI", "SIGNATURE", "COUNTRYCODEUCI"));

    /**
     * The KID of the Key used to sign the CMS.
     */
    @Id
    @Column(name = "kid")
    private String kid;

    /**
     * Type of Revocation Hashes.
     */
    @Column(name = "hashtypes")
    private String typesString;

    /**
     * One of the storage modes (POINT, VECTOR; COORDINATE)
     */
    @Column(name = "storage_mode")
    private String storageMode;

    /**
     *  The creation date of the entity
     */
    @Column(name = "lastupdated")
    private ZonedDateTime lastUpdated;

    /**
     *  The expiration date of the entity
     */
    @Column(name = "expired")
    private ZonedDateTime expired;

    @Column(name = "updated")
    private boolean updated;

    @Transient
    private List<String> types;

    @PostLoad
    private void onLoad() {

        this.types = Arrays.stream(typesString.split(","))
            .filter(i -> TYPES_NAMES.contains(i)).collect(Collectors.toList());

    }


}
