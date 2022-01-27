package europa.ec.dgc.revocationdistribution.repository;

import europa.ec.dgc.revocationdistribution.entity.VectorViewEntity;
import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface VectorViewRepository extends ReadOnlyRepository<VectorViewEntity, String>{

    List<VectorViewEntity> findAllByKidAndId(String kid, String id);

    @Query("SELECT DISTINCT v.id FROM VectorViewEntity v WHERE v.kid = :kid")
    List<String> findDistinctIdsByKid(@Param("kid") String kId);

}
