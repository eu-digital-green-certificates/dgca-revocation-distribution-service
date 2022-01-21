package europa.ec.dgc.revocationdistribution.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.vladmihalcea.hibernate.type.json.JsonBinaryType;
import europa.ec.dgc.revocationdistribution.dto.PartitionChunksJsonItemDto;
import java.time.ZonedDateTime;
import java.util.Map;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;

@Getter
@Setter
@Entity
@Table(name = "partitions")
@AllArgsConstructor
@NoArgsConstructor
@TypeDef(name = "jsonb", typeClass = JsonBinaryType.class)
public class PartitionEntity {

    /**
     * The KID.
     */
    @Id
    @Column(name = "kid")
    private String kid;

    /**
     * Id .
     */
    @Column(name = "partition_id")
    private String id;

    /**
     * x parameter of chunk.
     */
    @Column(name = "x")
    private String x;

    /**
     * x parameter of chunk.
     */
    @Column(name = "y")
    private String y;

    /**
     * x parameter of chunk.
     */
    @Column(name = "z")
    private String z;


    /**
     *  The creation date of the entity.
     */
    @Column(name = "lastupdated")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ssXXX", timezone = "UTC")
    private ZonedDateTime lastUpdated;

    /**
     *  The expiration date of the entity.
     */
    @Column(name = "expired")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ssXXX", timezone = "UTC")
    private ZonedDateTime expired;

    /**
     * The hashes of the chunk.
     */
    @Type(type = "jsonb")
    @Column(name = "chunks_json_data", columnDefinition = "jsonb")
    private Map<String, Map<String,PartitionChunksJsonItemDto>> chunks;

}
