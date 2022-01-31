package europa.ec.dgc.revocationdistribution.entity;

import javax.persistence.Entity;
import javax.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "vector_view")
public class VectorViewEntity extends AbstractChunkMetaViewEntity {


}
