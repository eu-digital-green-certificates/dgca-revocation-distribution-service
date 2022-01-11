package europa.ec.dgc.revocationdistribution.repository;

import europa.ec.dgc.revocationdistribution.entity.HashesEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HashesRepository extends JpaRepository<HashesEntity, String> {
}
