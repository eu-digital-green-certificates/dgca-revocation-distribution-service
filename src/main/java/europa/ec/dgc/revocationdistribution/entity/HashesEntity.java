package europa.ec.dgc.revocationdistribution.entity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


@Getter
@Setter
@Entity
@Table(name = "hashes")
@AllArgsConstructor
@NoArgsConstructor
public class HashesEntity {

    /**
     * The revoked hash.
     */
    @Id
    @Column(name = "hash", nullable = false)
    private String hash;

    /**
     * The KID of the Key used to sign the CMS.
     */
    @Column(name = "kid", length = 12)
    private String kid;

    /**
     * The first byte of the hash
     */
    @Column(name = "x", nullable = false, length=1, columnDefinition="CHAR")
    private char x;

    /**
     * The second byte of the hash
     */
    @Column(name = "y", nullable = false, length=1, columnDefinition="CHAR")
    private char y;

    /**
     * The third byte of the hash
     */
    @Column(name = "z", nullable = false, length=1, columnDefinition="CHAR")
    private char z;

    /**
     * ID of the Batch.
     */
    @Column(name = "batchId", nullable = false, length = 36)
    private String batchId;

    /**
     * Update status of the hash value.
     */
    @Column(name = "updated")
    private boolean updated;

}
