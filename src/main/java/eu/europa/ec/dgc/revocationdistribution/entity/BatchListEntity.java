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


import java.time.ZonedDateTime;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.PrePersist;
import javax.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "batch_list")
@AllArgsConstructor
@NoArgsConstructor
public class BatchListEntity {


    /**
     * ID of the Batch.
     */
    @Id
    @Column(name = "batch_id", nullable = false, length = 36, unique = true)
    private String batchId;

    /**
     * Timestamp when the Batch will expire.
     */
    @Column(name = "expires", nullable = false)
    private ZonedDateTime expires;

    /**
     * ISO 3166 Alpha-2 Country Code.
     * (plus code "EU" for administrative European Union entries).
     */
    @Column(name = "country", nullable = false, length = 2)
    private String country;

    /**
     * Type of Revocation Hashes.
     */
    @Column(name = "type", nullable = false)
    @Enumerated(EnumType.STRING)
    private RevocationHashType type;

    /**
     * The KID of the Key used to sign the CMS.
     */
    @Column(name = "kid", length = 12)
    private String kid;

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

    /**
     * Available types of Hash.
     */
    public enum RevocationHashType {
        SIGNATURE,
        UCI,
        COUNTRYCODEUCI
    }

}
