package europa.ec.dgc.revocationdistribution.repository;

import europa.ec.dgc.revocationdistribution.entity.SliceEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SliceRepository extends JpaRepository<SliceEntity, String> {


    Optional<SliceEntity> findOneByEtagAndKidAndIdAndChunkAndHash(String etag, String kid, String id, String cid, String sid);

    Optional<SliceEntity> findOneByEtagAndKidAndIdIsNullAndChunkAndHash(String etag, String kid, String cid, String sid);

    List<SliceEntity> findAllByEtagAndKidAndIdAndChunk(String etag, String kid, String id, String cid);

    List<SliceEntity> findAllByEtagAndKidAndIdIsNullAndChunk(String etag, String kid, String cid);

    @Modifying
    @Query("UPDATE SliceEntity s SET s.toBeDeleted = true WHERE s.kid in :kids")
    void setToBeDeletedForKids(@Param("kids") List<String> kIds);

    List<SliceEntity> findAllByEtagAndKidAndId(String etag, String kid, String id);

    List<SliceEntity> findAllByEtagAndKidAndIdIsNull(String etag, String kid);

    List<SliceEntity> findAllByEtagAndKidAndIdIsNullAndChunkIn(String etag, String kid, List<String> chunks);

    List<SliceEntity> findAllByEtagAndKidAndIdAndChunkIn(String etag, String kid, String id, List<String> chunks);

    List<SliceEntity> findAllByEtagAndKidAndIdIsNullAndChunkAndHashIn(String etag, String kid, String cid, List<String> hashes);

    List<SliceEntity> findAllByEtagAndKidAndIdAndChunkAndHashIn(String etag, String kid, String id, String cid, List<String> hashes);
}
