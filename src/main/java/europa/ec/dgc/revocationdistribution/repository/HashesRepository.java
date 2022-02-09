package europa.ec.dgc.revocationdistribution.repository;

import europa.ec.dgc.revocationdistribution.entity.HashesEntity;
import java.util.List;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface HashesRepository extends JpaRepository<HashesEntity, String> {

    @Modifying
    @Query("UPDATE HashesEntity h SET h.updated = false WHERE h.updated = true")
    void setAllUpdatedStatesToFalse();

    @Modifying
    @Query("DELETE HashesEntity h WHERE  h.batchId = null")
    void deleteAllOrphanedHashes();

    @Query("SELECT h.id FROM HashesEntity h WHERE h.id IN :hashes")
    List<String> getHashesPresentInListAndDb(@Param("hashes") List<String> hashes);

}
