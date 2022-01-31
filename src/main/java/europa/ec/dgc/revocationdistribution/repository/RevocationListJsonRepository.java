package europa.ec.dgc.revocationdistribution.repository;

import europa.ec.dgc.revocationdistribution.entity.RevocationListJsonEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RevocationListJsonRepository extends JpaRepository<RevocationListJsonEntity, String> {

}
