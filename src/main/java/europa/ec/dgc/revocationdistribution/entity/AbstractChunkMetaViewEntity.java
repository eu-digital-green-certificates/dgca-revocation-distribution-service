package europa.ec.dgc.revocationdistribution.entity;

import com.vladmihalcea.hibernate.type.array.StringArrayType;
import java.time.ZonedDateTime;
import java.util.List;
import javax.persistence.Column;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;
import lombok.Getter;
import org.hibernate.annotations.RowId;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.springframework.data.annotation.Immutable;


@Getter
@Immutable
@MappedSuperclass
@TypeDef(name = "string_array", typeClass = StringArrayType.class)
public abstract class AbstractChunkMetaViewEntity {

    @Id
    @Column(name = "row_id")
    private String rowId;

    /**
     * The KID.
     */
    @Column(name = "kid")
    private String kid;

    /**
     * The hashes of the chunk.
     */
    @Column(name = "hashes")
    @Type(type = "string_array")
    private String[] hashes;

    /**
     * Id of chunk
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
    @Column(name = "chunk")
    private String chunk;

    /**
     *  The creation date of the entity.
     */
    @Column(name = "lastupdated")
    private ZonedDateTime lastUpdated;

    /**
     *  The expiration date of the entity.
     */
    @Column(name = "expired")
    private ZonedDateTime expired;

}
