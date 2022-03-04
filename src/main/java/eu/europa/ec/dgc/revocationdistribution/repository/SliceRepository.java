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
import eu.europa.ec.dgc.revocationdistribution.model.SliceType;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SliceRepository extends JpaRepository<SliceEntity, String> {


    Optional<SliceEntity> findOneByEtagAndKidAndIdAndChunkAndHashAndDataType(
        String etag, String kid, String id, String cid, String sid, SliceType dataType);

    Optional<SliceEntity> findOneByEtagAndKidAndIdIsNullAndChunkAndHashAndDataType(
        String etag, String kid, String cid, String sid, SliceType dataType);

    List<SliceEntity> findAllByEtagAndKidAndIdAndChunkAndDataType(
        String etag, String kid, String id, String cid, SliceType dataType);

    List<SliceEntity> findAllByEtagAndKidAndIdIsNullAndChunkAndDataType(
        String etag, String kid, String cid, SliceType dataType);

    @Modifying
    @Query("UPDATE SliceEntity s SET s.toBeDeleted = true WHERE s.kid in :kids")
    void setToBeDeletedForKids(@Param("kids") List<String> kids);

    List<SliceEntity> findAllByEtagAndKidAndIdAndDataType(String etag, String kid, String id, SliceType dataType);

    List<SliceEntity> findAllByEtagAndKidAndIdIsNullAndDataType(String etag, String kid, SliceType dataType);

    List<SliceEntity> findAllByEtagAndKidAndIdIsNullAndDataTypeAndLastUpdatedAfter(
        String etag, String kid, SliceType dataType, ZonedDateTime ifModifiedDateTime);

    List<SliceEntity> findAllByEtagAndKidAndIdAndDataTypeAndLastUpdatedAfter(
        String etag, String kid, String id, SliceType dataType, ZonedDateTime ifModifiedDateTime);


    List<SliceEntity> findAllByEtagAndKidAndIdIsNullAndDataTypeAndChunkIn(
        String etag, String kid, SliceType dataType, List<String> chunks);

    List<SliceEntity> findAllByEtagAndKidAndIdAndDataTypeAndChunkIn(
        String etag, String kid, String id, SliceType dataType, List<String> chunks);

    List<SliceEntity> findAllByEtagAndKidAndIdIsNullAndChunkAndDataTypeAndHashIn(
        String etag, String kid, String cid, SliceType dataType, List<String> hashes);

    List<SliceEntity> findAllByEtagAndKidAndIdAndChunkAndDataTypeAndHashIn(
        String etag, String kid, String id, String cid, SliceType dataType, List<String> hashes);

    Long countByEtagAndKidAndIdIsNullAndDataType(String etag, String kid, SliceType dataType);

    Long countByEtagAndKidAndIdAndDataType(String etag, String kid, String id, SliceType dataType);

    List<SliceEntity> findAllByEtagAndKidAndIdIsNullAndDataTypeAndChunkInAndLastUpdatedAfter(
        String etag, String kid, SliceType dataType, List<String> filter, ZonedDateTime ifModifiedDateTime);

    List<SliceEntity> findAllByEtagAndKidAndIdAndDataTypeAndChunkInAndLastUpdatedAfter(
        String etag, String kid, String id, SliceType dataType, List<String> filter, ZonedDateTime ifModifiedDateTime);

    Long countByEtagAndKidAndIdIsNullAndDataTypeAndChunkIn(
        String etag, String kid, SliceType dataType, List<String> filter);

    Long countByEtagAndKidAndIdAndDataTypeAndChunkIn(
        String etag, String kid, String id, SliceType dataType, List<String> filter);

    List<SliceEntity> findAllByEtagAndKidAndIdIsNullAndChunkAndDataTypeAndLastUpdatedAfter(
        String etag, String kid, String cid, SliceType dataType, ZonedDateTime ifModifiedDateTime);

    List<SliceEntity> findAllByEtagAndKidAndIdAndChunkAndDataTypeAndLastUpdatedAfter(
        String etag, String kid, String id, String cid, SliceType dataType, ZonedDateTime ifModifiedDateTime);

    Long countByEtagAndKidAndIdIsNullAndChunkAndDataType(String etag, String kid, String cid, SliceType dataType);

    Long countByEtagAndKidAndIdAndChunkAndDataType(String etag, String kid, String id, String cid, SliceType dataType);

    List<SliceEntity> findAllByEtagAndKidAndIdIsNullAndChunkAndDataTypeAndHashInAndLastUpdatedAfter(
        String etag, String kid, String cid, SliceType dataType, List<String> filter, ZonedDateTime ifModifiedDateTime);

    List<SliceEntity> findAllByEtagAndKidAndIdAndChunkAndDataTypeAndHashInAndLastUpdatedAfter(
        String etag, String kid, String id, String cid, SliceType dataType,
        List<String> filter, ZonedDateTime ifModifiedDateTime);

    Long countByEtagAndKidAndIdIsNullAndChunkAndDataTypeAndHashIn(
        String etag, String kid, String cid, SliceType dataType, List<String> filter);

    Long countByEtagAndKidAndIdAndChunkAndDataTypeAndHashIn(
        String etag, String kid, String id, String cid, SliceType dataType, List<String> filter);

    Optional<SliceEntity> findOneByEtagAndKidAndIdIsNullAndChunkAndHashAndDataTypeAndLastUpdatedAfter(
        String etag, String kid, String cid, String sid, SliceType dataType, ZonedDateTime ifModifiedDateTime);

    Optional<SliceEntity> findOneByEtagAndKidAndIdAndChunkAndHashAndDataTypeAndLastUpdatedAfter(
        String etag, String kid, String id, String cid, String sid,
        SliceType dataType, ZonedDateTime ifModifiedDateTime);

    Long countByEtagAndKidAndIdIsNullAndChunkAndHashAndDataType(
        String etag, String kid, String cid, String sid, SliceType dataType);

    Long countByEtagAndKidAndIdAndChunkAndHashAndDataType(
        String etag, String kid, String id, String cid, String sid, SliceType dataType);
}
