package europa.ec.dgc.revocationdistribution.repository;

import europa.ec.dgc.revocationdistribution.entity.CoordinateViewEntity;
import europa.ec.dgc.revocationdistribution.entity.VectorViewEntity;
import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CoordinateViewRepository extends ReadOnlyRepository<CoordinateViewEntity, String>{

    List<CoordinateViewEntity> findAllByKidAndId(String kid, String id);

    @Query("SELECT DISTINCT c.id FROM CoordinateViewEntity c WHERE c.kid = :kid")
    List<String> findDistinctIdsByKid(@Param("kid") String kId);
}
