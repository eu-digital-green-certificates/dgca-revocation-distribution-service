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

import com.vladmihalcea.hibernate.type.array.StringArrayType;
import java.time.ZonedDateTime;
import javax.persistence.Column;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;
import lombok.Getter;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.springframework.data.annotation.Immutable;
import org.springframework.lang.Nullable;


@Getter
@Immutable
@MappedSuperclass
@TypeDef(name = "string_array", typeClass = StringArrayType.class)
public abstract class AbstractChunkMetaViewEntity {

    @Id
    @Column(name = "row_id")
    private String rowId;

    /**
     * The KID.
     */
    @Column(name = "kid")
    private String kid;

    /**
     * The hashes of the chunk.
     */
    @Column(name = "hashes")
    @Type(type = "string_array")
    private String[] hashes;

    /**
     * The partition id of the chunk.
     */
    @Nullable
    @Column(name = "partition_id")
    private String id;

    /**
     * x parameter of chunk.
     */
    @Nullable
    @Column(name = "x")
    @SuppressWarnings("checkstyle:membername")
    private String x;

    /**
     * x parameter of chunk.
     */
    @Nullable
    @Column(name = "y")
    @SuppressWarnings("checkstyle:membername")
    private String y;

    /**
     * x parameter of chunk.
     */
    @Column(name = "chunk")
    private String chunk;

    /**
     * The creation date of the entity.
     */
    @Column(name = "lastupdated")
    private ZonedDateTime lastUpdated;

    /**
     * The expiration date of the entity.
     */
    @Column(name = "expired")
    private ZonedDateTime expired;

}
