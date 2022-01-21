package europa.ec.dgc.revocationdistribution.repository;

import europa.ec.dgc.revocationdistribution.entity.PointViewEntity;
import java.util.List;

public interface PointViewRepository extends ReadOnlyRepository<PointViewEntity, String>{

    List<PointViewEntity> findAllByKid(String kid);
}
