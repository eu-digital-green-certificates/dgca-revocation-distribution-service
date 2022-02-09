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

package europa.ec.dgc.revocationdistribution.model;

import europa.ec.dgc.revocationdistribution.entity.KidViewEntity;
import java.time.ZonedDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
public class ChangeListItem {

    public ChangeListItem (KidViewEntity kve, String oldStorageMode){
        this.kidId = kve.getKid();
        this.lastUpdated = kve.getLastUpdated();
        this.expired = kve.getExpired();
        this.newStorageMode = kve.getStorageMode();
        this.oldStorageMode = oldStorageMode;
    }

    private String kidId;

    private String oldStorageMode;

    private String newStorageMode;

    private ZonedDateTime lastUpdated;

    private ZonedDateTime expired;


}
