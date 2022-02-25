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
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PartitionRepository extends JpaRepository<PartitionEntity, String> {


    List<PartitionEntity> findAllByEtagAndKid(String etag, String kid);

    List<PartitionEntity> findAllByEtagAndKidAndLastUpdatedAfter(
        String etag, String kid, ZonedDateTime ifModifiedSince);

    @Modifying
    @Query("UPDATE PartitionEntity p SET p.toBeDeleted = true WHERE p.kid in :kids")
    void setToBeDeletedForKids(@Param("kids") List<String> kids);


    Optional<PartitionEntity> findOneByEtagAndKidAndId(String etag, String kid, String id);

    Optional<PartitionEntity> findOneByEtagAndKidAndIdIsNull(String etag, String kid);


    Optional<PartitionEntity> findOneByEtagAndKidAndIdAndLastUpdatedAfter(
        String etag, String kid, String id, ZonedDateTime ifModifiedSince);

    Optional<PartitionEntity> findOneByEtagAndKidAndIdIsNullAndLastUpdatedAfter(
        String etag, String kid, ZonedDateTime ifModifiedSince);
}
