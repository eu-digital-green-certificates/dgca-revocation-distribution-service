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

package eu.europa.ec.dgc.revocationdistribution.entity;

import com.vladmihalcea.hibernate.type.json.JsonBinaryType;
import eu.europa.ec.dgc.revocationdistribution.dto.RevocationListJsonResponseDto;
import java.time.ZonedDateTime;
import java.util.List;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.PrePersist;
import javax.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;


@Getter
@Setter
@Entity
@Table(name = "revocation_list_json")
@AllArgsConstructor
@NoArgsConstructor
@TypeDef(name = "jsonb", typeClass = JsonBinaryType.class)
public class RevocationListJsonEntity {

    /**
     * The revoked hash.
     */
    @Id
    @Column(name = "etag", nullable = false, length = 36)
    private String etag;


    /**
     * The creation date of the entity
     */
    @Column(name = "created_at")
    private ZonedDateTime createdAt;

    @PrePersist
    private void prePersistFunction() {

        if (createdAt == null) {
            createdAt = ZonedDateTime.now();
        }
    }


    @Type(type = "jsonb")
    @Column(name = "json_data", columnDefinition = "jsonb")
    private List<RevocationListJsonResponseDto.RevocationListJsonResponseItemDto> jsonData;


}
