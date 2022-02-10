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

package eu.europa.ec.dgc.revocationdistribution.service;


import eu.europa.ec.dgc.revocationdistribution.entity.InfoEntity;
import eu.europa.ec.dgc.revocationdistribution.repository.InfoRepository;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@RequiredArgsConstructor
@Service
public class InfoService {

    public final static String LAST_UPDATED_KEY = "LASTUPDATED";
    public final static String CURRENT_ETAG = "CURRENTETAG";

    private final InfoRepository infoRepository;

    public String getValueForKey(String key) {
        Optional<InfoEntity> optionalValue = infoRepository.findById(key);

        if(optionalValue.isPresent() && !optionalValue.isEmpty()) {
            return optionalValue.get().getValue();
        } else {
            return null;
        }
    }

    public void setValueForKey(String key, String value) {
        InfoEntity infoEntity = new InfoEntity(key, value);
        infoRepository.save(infoEntity);
    }

    public void setNewEtag(String etag) {
        infoRepository.setNewEtag(etag);
    }
}
