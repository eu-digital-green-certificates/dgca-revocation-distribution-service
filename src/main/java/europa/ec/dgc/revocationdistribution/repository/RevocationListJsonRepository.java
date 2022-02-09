package europa.ec.dgc.revocationdistribution.repository;

import europa.ec.dgc.revocationdistribution.entity.RevocationListJsonEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RevocationListJsonRepository extends JpaRepository<RevocationListJsonEntity, String> {

    @Modifying
    @Query("DELETE RevocationListJsonEntity r WHERE  r.etag != :etag")
    void deleteAllOutdatedEntries(@Param("etag") String etag);

}
