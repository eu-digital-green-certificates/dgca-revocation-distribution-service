package europa.ec.dgc.revocationdistribution.repository;

import europa.ec.dgc.revocationdistribution.entity.InfoEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.query.Procedure;

public interface InfoRepository extends JpaRepository<InfoEntity, String> {

    @Procedure("set_new_etag")
    int setNewEtag(String newEtag);

}
