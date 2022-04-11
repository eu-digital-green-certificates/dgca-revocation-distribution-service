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

import eu.europa.ec.dgc.revocationdistribution.entity.HashesEntity;
import java.time.ZonedDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface HashesRepository extends JpaRepository<HashesEntity, String> {

    @Modifying
    @Query("UPDATE HashesEntity h SET h.updated = false WHERE h.updated = true")
    void setAllUpdatedStatesToFalse();

    @Modifying
    @Query("DELETE HashesEntity h WHERE  h.batch = null")
    void deleteAllOrphanedHashes();

    @Query("SELECT h.hash FROM HashesEntity h WHERE h.hash IN :hashes")
    List<String> getHashesPresentInListAndDb(@Param("hashes") List<String> hashes);

    @Query("SELECT h.hash FROM HashesEntity h INNER JOIN h.batch b WHERE h.hash IN :hashes AND b.expires > :checkTime")
    List<String> getHashesPresentInListAndDbAndNotExpired(
        @Param("hashes") List<String> hashes,
        @Param("checkTime") ZonedDateTime checkTime);

}
