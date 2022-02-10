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

package eu.europa.ec.dgc.revocationdistribution.controller;

import eu.europa.ec.dgc.revocationdistribution.dto.RevocationCheckTokenPayload;
import eu.europa.ec.dgc.revocationdistribution.service.LookupService;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.List;
import javax.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/revocation/lookup")
@Slf4j
@RequiredArgsConstructor
public class LookupController {

    private final LookupService lookupService;

    /**
     * Http Method for getting the revocation list.
     * @return
     */
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<String>> lockupRevocation(
        @Valid @RequestBody(required = false)  List<String> revocationCheckTokenList
    ){
        if (revocationCheckTokenList.isEmpty()) {
            return ResponseEntity.ok(new ArrayList<>());
        }

        List<RevocationCheckTokenPayload> tokenPayloads =
            lookupService.validateRevocationCheckTokens(revocationCheckTokenList);

        List<String> result = lookupService.checkForRevocation(tokenPayloads);
        return ResponseEntity.ok(result);
    }

    @GetMapping(path= "/key", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> getKey() {
        PublicKey result;
        String hash = "Pu3LWoDPQv3lH53fcYmCOb12mHPd354tAXdWJDQns1U%3d";
        result = lookupService.downloadPublicKey(hash);
        return ResponseEntity.ok(result.toString());
    }


}

