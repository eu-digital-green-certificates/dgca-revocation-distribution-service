package europa.ec.dgc.revocationdistribution.repository;

import europa.ec.dgc.revocationdistribution.entity.HashesEntity;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface HashesRepository extends JpaRepository<HashesEntity, String> {

    @Modifying
    @Query("UPDATE HashesEntity h SET h.updated = false WHERE h.updated = true")
    void setAllUpdatedStatesToFalse();

    @Modifying
    @Query("DELETE HashesEntity h WHERE  h.batchId = null")
    void deleteAllOrphanedHashes();

}
