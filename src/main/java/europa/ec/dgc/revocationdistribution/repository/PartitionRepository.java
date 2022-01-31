package europa.ec.dgc.revocationdistribution.repository;

import europa.ec.dgc.revocationdistribution.dto.PartitionResponseDto;
import europa.ec.dgc.revocationdistribution.entity.PartitionEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PartitionRepository extends JpaRepository<PartitionEntity, String> {


    List<PartitionEntity> findAllByEtagAndKid(String etag, String kid);

    @Modifying
    @Query("UPDATE PartitionEntity p SET p.toBeDeleted = true WHERE p.kid in :kids")
    void setToBeDeletedForKids(@Param("kids") List<String> kIds);


    Optional<PartitionEntity> findOneByEtagAndKidAndId(String etag, String kid, String id);
}
