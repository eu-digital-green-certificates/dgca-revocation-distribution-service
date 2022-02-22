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

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


@Getter
@Setter
@Entity
@Table(name = "hashes")
@AllArgsConstructor
@NoArgsConstructor
public class HashesEntity {

    /**
     * The revoked hash.
     */
    @Id
    @Column(name = "hash", nullable = false)
    private String hash;

    /**
     * The KID of the Key used to sign the CMS.
     */
    @Column(name = "kid", length = 12)
    private String kid;

    /**
     * The first byte of the hash.
     */
    @SuppressWarnings("checkstyle:membername")
    @Column(name = "x", nullable = false, length = 1, columnDefinition = "CHAR")
    private char x;

    /**
     * The second byte of the hash.
     */
    @SuppressWarnings("checkstyle:membername")
    @Column(name = "y", nullable = false, length = 1, columnDefinition = "CHAR")
    private char y;

    /**
     * The third byte of the hash.
     */
    @SuppressWarnings("checkstyle:membername")
    @Column(name = "z", nullable = false, length = 1, columnDefinition = "CHAR")
    private char z;

    /**
     * ID of the Batch.
     */
    @Column(name = "batch_id", length = 36)
    private String batchId;

    /**
     * Update status of the hash value.
     */
    @Column(name = "updated")
    private boolean updated;

}
