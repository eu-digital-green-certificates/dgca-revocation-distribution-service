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

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PostLoad;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import lombok.Getter;
import org.springframework.data.annotation.Immutable;


@Getter
@Entity
@Table(name = "kid_view")
@Immutable
public class KidViewEntity {

    public static final List<String> TYPES_NAMES = new ArrayList<>(List.of("UCI", "SIGNATURE", "COUNTRYCODEUCI"));

    /**
     * The KID of the Key used to sign the CMS.
     */
    @Id
    @Column(name = "kid")
    private String kid;

    /**
     * Types of revocation hashes stored as comma separated string.
     */
    @Column(name = "hashtypes")
    private String typesString;

    /**
     * One of the storage modes (POINT, VECTOR; COORDINATE).
     */
    @Column(name = "storage_mode")
    private String storageMode;

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

    /**
     * Indicates if the dataset gets updated.
     */
    @Column(name = "updated")
    private boolean updated;

    /*
     * Types of revocation hashes stored as list.
     */
    @Transient
    private List<String> types;

    @PostLoad
    private void onLoad() {

        this.types = Arrays.stream(typesString.split(","))
            .filter(TYPES_NAMES::contains).collect(Collectors.toList());

    }


}
