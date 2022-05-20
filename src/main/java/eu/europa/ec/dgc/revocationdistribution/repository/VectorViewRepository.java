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

import eu.europa.ec.dgc.revocationdistribution.entity.VectorViewEntity;
import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface VectorViewRepository extends ReadOnlyRepository<VectorViewEntity, String> {

    List<VectorViewEntity> findAllByKidAndId(String kid, String id);

    @Query("SELECT DISTINCT v.id FROM VectorViewEntity v WHERE v.kid = :kid")
    List<String> findDistinctIdsByKid(@Param("kid") String kid);

}