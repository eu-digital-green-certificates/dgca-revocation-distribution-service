package europa.ec.dgc.revocationdistribution.entity;

import com.vladmihalcea.hibernate.type.json.JsonBinaryType;
import europa.ec.dgc.revocationdistribution.dto.RevocationListJsonResponseDto;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.PrePersist;
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
@Table(name = "revocation_list_json")
@AllArgsConstructor
@NoArgsConstructor
@TypeDef(name = "jsonb", typeClass = JsonBinaryType.class)
public class RevocationListJsonEntity {

    /**
     * The revoked hash.
     */
    @Id
    @Column(name = "etag", nullable = false, length = 36)
    private String etag;


    /**
     *  The creation date of the entity
     */
    @Column(name = "created_at")
    private ZonedDateTime createdAt;

    @PrePersist
    private void prePersistFunction(){

        if(createdAt == null){
            createdAt = ZonedDateTime.now();
        }
    }


    @Type(type = "jsonb")
    @Column(name = "json_data", columnDefinition = "jsonb")
    private List<RevocationListJsonResponseDto.RevocationListJsonResponseItemDto> jsonData;


}
