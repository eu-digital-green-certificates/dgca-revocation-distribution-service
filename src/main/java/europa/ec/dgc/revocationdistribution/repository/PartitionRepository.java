package europa.ec.dgc.revocationdistribution.repository;

import europa.ec.dgc.revocationdistribution.dto.PartitionResponseDto;
import europa.ec.dgc.revocationdistribution.entity.PartitionEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PartitionRepository extends JpaRepository<PartitionEntity, String> {


    List<PartitionEntity> findAllByEtagAndKid(String etag, String kid);
}
