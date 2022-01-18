/*-
 * ---license-start
 * eu-digital-green-certificates / dgca-businessrule-service
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

package europa.ec.dgc.revocationdistribution.controller;

import europa.ec.dgc.revocationdistribution.dto.RevocationListJsonResponseDto;
import europa.ec.dgc.revocationdistribution.entity.RevocationListJsonEntity;
import europa.ec.dgc.revocationdistribution.service.InfoService;
import europa.ec.dgc.revocationdistribution.service.RevocationListService;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/")
@Slf4j
@RequiredArgsConstructor
public class RevocationListController {

    private static final String API_VERSION_HEADER = "X-VERSION";

    private final InfoService infoService;
    private final RevocationListService revocationListService;


    /**
     * Http Method for getting the revocation list.
     * @return
     */
    @GetMapping(path = "lists", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<RevocationListJsonResponseDto.RevocationListJsonResponseItemDto>> getRevocationList(
        @RequestHeader(value = HttpHeaders.IF_NONE_MATCH, required = true) String ifNoneMatch) {

        String currentEtag = infoService.getValueForKey(InfoService.CURRENT_ETAG);

        if (ifNoneMatch.equals(currentEtag)) {
            return ResponseEntity.status(HttpStatus.NOT_MODIFIED).build();
        }

        Optional<RevocationListJsonEntity> revocationListJsonEntity = revocationListService.getRevocationListJsonData(currentEtag);
        if(!revocationListJsonEntity.isPresent()) {
            return ResponseEntity.notFound().build();
        }


        ResponseEntity.BodyBuilder respBuilder = ResponseEntity.ok();
        HttpHeaders responseHeaders = new HttpHeaders();
        responseHeaders.set("etag", currentEtag);
        respBuilder.headers(responseHeaders);

        return respBuilder.body(revocationListJsonEntity.get().getJsonData());
    }

    /**
     * Http Method for getting the partitions list of a kid.
     */
    @GetMapping(path = "/{kid}/partitions", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> getPartitionListForKid(
        @PathVariable String kid,
        @RequestHeader(value = HttpHeaders.IF_NONE_MATCH, required = true) String ifNoneMatch,
        @RequestHeader(value = HttpHeaders.IF_MODIFIED_SINCE, required = true) String ifModifiedSince
    ) {


        ResponseEntity<String> responseEntity;
        responseEntity = ResponseEntity.ok(String.format("Not Implemented jet. Kid: %s", kid));
        return responseEntity;
    }


}
