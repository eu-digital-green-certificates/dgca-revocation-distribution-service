package europa.ec.dgc.revocationdistribution.repository;

import europa.ec.dgc.revocationdistribution.entity.BatchListEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BatchListRepository extends JpaRepository<BatchListEntity, String> {
}
