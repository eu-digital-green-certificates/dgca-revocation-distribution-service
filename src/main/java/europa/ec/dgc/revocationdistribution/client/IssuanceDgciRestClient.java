/*-
 * ---license-start
 * eu-digital-green-certificates / dgca-validation-service
 * ---
 * Copyright (C) 2021 T-Systems International GmbH and all other contributors
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

package europa.ec.dgc.revocationdistribution.client;

import europa.ec.dgc.revocationdistribution.dto.DidDocument;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(
    name = "business-download-client",
    url = "${dgc.issuance.dgci.endpoint}",
    configuration = IssuanceDgciRestClientConfig.class
)
public interface IssuanceDgciRestClient {


    /**
     * Gets the the dgci for a given hash value.
     *
     * @param hash  The hash value of the dgci
     * @return dgci values as DidDocument.
     */
    @GetMapping(value = "/dgci/{hash}", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<DidDocument> getDgciByHash(@PathVariable("hash") String hash);
}

