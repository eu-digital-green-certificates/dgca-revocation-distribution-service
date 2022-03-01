/*-
 * ---license-start
 * eu-digital-green-certificates / dgca-revocation-distribution-service
 * ---
 * Copyright (C) 2022 T-Systems International GmbH and all other contributors
 * ---
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ---license-end
 */

package eu.europa.ec.dgc.revocationdistribution.repository;

import eu.europa.ec.dgc.revocationdistribution.entity.SliceEntity;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SliceRepository extends JpaRepository<SliceEntity, String> {


    Optional<SliceEntity> findOneByEtagAndKidAndIdAndChunkAndHash(
        String etag, String kid, String id, String cid, String sid);

    Optional<SliceEntity> findOneByEtagAndKidAndIdIsNullAndChunkAndHash(
        String etag, String kid, String cid, String sid);

    List<SliceEntity> findAllByEtagAndKidAndIdAndChunk(String etag, String kid, String id, String cid);

    List<SliceEntity> findAllByEtagAndKidAndIdIsNullAndChunk(String etag, String kid, String cid);

    @Modifying
    @Query("UPDATE SliceEntity s SET s.toBeDeleted = true WHERE s.kid in :kids")
    void setToBeDeletedForKids(@Param("kids") List<String> kids);

    List<SliceEntity> findAllByEtagAndKidAndId(String etag, String kid, String id);

    List<SliceEntity> findAllByEtagAndKidAndIdIsNull(String etag, String kid);

    List<SliceEntity> findAllByEtagAndKidAndIdIsNullAndLastUpdatedAfter(
        String etag, String kid, ZonedDateTime ifModifiedDateTime);

    List<SliceEntity> findAllByEtagAndKidAndIdAndLastUpdatedAfter(
        String etag, String kid, String id, ZonedDateTime ifModifiedDateTime);


    List<SliceEntity> findAllByEtagAndKidAndIdIsNullAndChunkIn(String etag, String kid, List<String> chunks);

    List<SliceEntity> findAllByEtagAndKidAndIdAndChunkIn(String etag, String kid, String id, List<String> chunks);

    List<SliceEntity> findAllByEtagAndKidAndIdIsNullAndChunkAndHashIn(
        String etag, String kid, String cid, List<String> hashes);

    List<SliceEntity> findAllByEtagAndKidAndIdAndChunkAndHashIn(
        String etag, String kid, String id, String cid, List<String> hashes);

    Long countByEtagAndKidAndIdIsNull(String etag, String kid);

    Long countByEtagAndKidAndId(String etag, String kid, String id);

    List<SliceEntity> findAllByEtagAndKidAndIdIsNullAndChunkInAndLastUpdatedAfter(
        String etag, String kid, List<String> filter, ZonedDateTime ifModifiedDateTime);

    List<SliceEntity> findAllByEtagAndKidAndIdAndChunkInAndLastUpdatedAfter(
        String etag, String kid, String id, List<String> filter, ZonedDateTime ifModifiedDateTime);

    Long countByEtagAndKidAndIdIsNullAndChunkIn(String etag, String kid, List<String> filter);

    Long countByEtagAndKidAndIdAndChunkIn(String etag, String kid, String id, List<String> filter);

    List<SliceEntity> findAllByEtagAndKidAndIdIsNullAndChunkAndLastUpdatedAfter(
        String etag, String kid, String cid, ZonedDateTime ifModifiedDateTime);

    List<SliceEntity> findAllByEtagAndKidAndIdAndChunkAndLastUpdatedAfter(
        String etag, String kid, String id, String cid, ZonedDateTime ifModifiedDateTime);

    Long countByEtagAndKidAndIdIsNullAndChunk(String etag, String kid, String cid);

    Long countByEtagAndKidAndIdAndChunk(String etag, String kid, String id, String cid);

    List<SliceEntity> findAllByEtagAndKidAndIdIsNullAndChunkAndHashInAndLastUpdatedAfter(
        String etag, String kid, String cid, List<String> filter, ZonedDateTime ifModifiedDateTime);

    List<SliceEntity> findAllByEtagAndKidAndIdAndChunkAndHashInAndLastUpdatedAfter(
        String etag, String kid, String id, String cid, List<String> filter, ZonedDateTime ifModifiedDateTime);

    Long countByEtagAndKidAndIdIsNullAndChunkAndHashIn(String etag, String kid, String cid, List<String> filter);

    Long countByEtagAndKidAndIdAndChunkAndHashIn(String etag, String kid, String id, String cid, List<String> filter);

    Optional<SliceEntity> findOneByEtagAndKidAndIdIsNullAndChunkAndHashAndLastUpdatedAfter(
        String etag, String kid, String cid, String sid, ZonedDateTime ifModifiedDateTime);

    Optional<SliceEntity> findOneByEtagAndKidAndIdAndChunkAndHashAndLastUpdatedAfter(
        String etag, String kid, String id, String cid, String sid, ZonedDateTime ifModifiedDateTime);

    Long countByEtagAndKidAndIdIsNullAndChunkAndHash(String etag, String kid, String cid, String sid);

    Long countByEtagAndKidAndIdAndChunkAndHash(String etag, String kid, String id, String cid, String sid);
}
