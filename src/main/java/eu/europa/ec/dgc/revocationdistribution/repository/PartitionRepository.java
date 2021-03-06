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

import eu.europa.ec.dgc.revocationdistribution.entity.PartitionEntity;
import eu.europa.ec.dgc.revocationdistribution.model.SliceType;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PartitionRepository extends JpaRepository<PartitionEntity, String> {


    List<PartitionEntity> findAllByEtagAndKidAndDataType(String etag, String kid, SliceType dataType);

    List<PartitionEntity> findAllByEtagAndKidAndDataTypeAndLastUpdatedAfter(
        String etag, String kid, SliceType dataType, ZonedDateTime ifModifiedSince);

    @Modifying
    @Query("UPDATE PartitionEntity p SET p.toBeDeleted = true WHERE p.kid in :kids")
    void setToBeDeletedForKids(@Param("kids") List<String> kids);


    Optional<PartitionEntity> findOneByEtagAndKidAndIdAndDataType(
        String etag, String kid, String id, SliceType dataType);

    Optional<PartitionEntity> findOneByEtagAndKidAndIdIsNullAndDataType(
        String etag, String kid, SliceType dataType);


    Optional<PartitionEntity> findOneByEtagAndKidAndIdAndDataTypeAndLastUpdatedAfter(
        String etag, String kid, String id, SliceType dataType, ZonedDateTime ifModifiedSince);

    Optional<PartitionEntity> findOneByEtagAndKidAndIdIsNullAndDataTypeAndLastUpdatedAfter(
        String etag, String kid, SliceType dataType, ZonedDateTime ifModifiedSince);

    Long countByEtagAndKidAndIdAndDataType(String etag, String kid, String id, SliceType dataType);

    Long countByEtagAndKidAndIdIsNullAndDataType(String etag, String kid,SliceType dataType);
}
