package europa.ec.dgc.revocationdistribution.repository;

import europa.ec.dgc.revocationdistribution.entity.PartitionEntity;
import europa.ec.dgc.revocationdistribution.entity.SliceEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SliceRepository extends JpaRepository<SliceEntity, String> {


    Optional<SliceEntity> findOneByEtagAndKidAndIdAndChunkAndHash(String etag, String kid, String id, String cid, String sid);

    Optional<SliceEntity> findOneByEtagAndKidAndIdIsNullAndChunkAndHash(String etag, String kid, String cid, String sid);
}
