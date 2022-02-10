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

import com.fasterxml.jackson.annotation.JsonFormat;
import com.vladmihalcea.hibernate.type.json.JsonBinaryType;
import eu.europa.ec.dgc.revocationdistribution.dto.PartitionChunksJsonItemDto;
import java.time.ZonedDateTime;
import java.util.Map;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
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
@Table(name = "partitions")
@AllArgsConstructor
@NoArgsConstructor
@TypeDef(name = "jsonb", typeClass = JsonBinaryType.class)
public class PartitionEntity {

    @Id
    @Column(name = "db_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long dbId;


    /**
     * The etag of the partition.
     */
    @Column(name = "etag", nullable = false, length = 36)
    private String etag;

    /**
     * The KID.
     */
    @Column(name = "kid")
    private String kid;

    /**
     * Id .
     */
    @Column(name = "partition_id", nullable = true)
    private String id;

    /**
     * x parameter of chunk.
     */
    @Column(name = "x", nullable = true)
    private String x;

    /**
     * x parameter of chunk.
     */
    @Column(name = "y", nullable = true)
    private String y;

    /**
     * x parameter of chunk.
     */
    @Column(name = "z", nullable = true)
    private String z;

    /**
     *  The creation date of the entity.
     */
    @Column(name = "lastupdated")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ssXXX", timezone = "UTC")
    private ZonedDateTime lastUpdated;

    /**
     *  The expiration date of the entity.
     */
    @Column(name = "expired")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ssXXX", timezone = "UTC")
    private ZonedDateTime expired;

    /**
     * The hashes of the chunk.
     */
    @Type(type = "jsonb")
    @Column(name = "chunks_json_data", columnDefinition = "jsonb")
    private Map<String, Map<String,PartitionChunksJsonItemDto>> chunks;

    /**
     * Indicates if the partition needs to be deleted on etag change.
     */
    @Column(name= "to_be_deleted")
    private boolean toBeDeleted;

}
