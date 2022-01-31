package europa.ec.dgc.revocationdistribution.entity;

import com.vladmihalcea.hibernate.type.array.StringArrayType;
import java.time.ZonedDateTime;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.TypeDef;
import org.hibernate.annotations.TypeDefs;
import org.springframework.data.annotation.Immutable;

@Getter
@Setter
@Entity
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "info")
public class InfoEntity {

    /**
     * The KID of the Key used to sign the CMS.
     */
    @Id
    @Column(name = "key")
    private String key;

    /**
     * Type of Revocation Hashes.
     */
    @Column(name = "value")
    private String value;

}
