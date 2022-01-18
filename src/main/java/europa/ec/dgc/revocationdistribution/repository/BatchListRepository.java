package europa.ec.dgc.revocationdistribution.repository;

import europa.ec.dgc.revocationdistribution.entity.BatchListEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BatchListRepository extends JpaRepository<BatchListEntity, String> {

    void deleteByBatchIdIn(List<String> batchIds);

}
