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

package europa.ec.dgc.revocationdistribution.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.vladmihalcea.hibernate.type.json.JsonBinaryType;
import europa.ec.dgc.revocationdistribution.dto.PartitionChunksJsonItemDto;
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
@Table(name = "slices")
@AllArgsConstructor
@NoArgsConstructor
public class SliceEntity {

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
    @Column(name = "partition_id")
    private String id;

    /**
     * chunk of slice.
     */
    @Column(name = "chunk")
    private String chunk;

    /**
     * chunk of slice.
     */
    @Column(name = "hash")
    private String hash;

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
     * The binary data of the slice.
     */
    @Column(name = "slice_binary_data")
    private byte[] binaryData;

    /**
     * Indicates if the slice needs to be deleted on etag change.
     */
    @Column(name= "to_be_deleted")
    private boolean toBeDeleted;

}
