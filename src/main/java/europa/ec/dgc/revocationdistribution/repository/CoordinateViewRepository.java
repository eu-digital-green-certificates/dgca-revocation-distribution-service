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

package europa.ec.dgc.revocationdistribution.repository;

import europa.ec.dgc.revocationdistribution.entity.CoordinateViewEntity;
import europa.ec.dgc.revocationdistribution.entity.VectorViewEntity;
import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CoordinateViewRepository extends ReadOnlyRepository<CoordinateViewEntity, String>{

    List<CoordinateViewEntity> findAllByKidAndId(String kid, String id);

    @Query("SELECT DISTINCT c.id FROM CoordinateViewEntity c WHERE c.kid = :kid")
    List<String> findDistinctIdsByKid(@Param("kid") String kId);
}
