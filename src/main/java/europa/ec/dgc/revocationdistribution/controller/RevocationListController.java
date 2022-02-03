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

import com.nimbusds.jose.util.Base64URL;
import europa.ec.dgc.revocationdistribution.dto.PartitionResponseDto;
import europa.ec.dgc.revocationdistribution.dto.RevocationListJsonResponseDto;
import europa.ec.dgc.revocationdistribution.entity.RevocationListJsonEntity;
import europa.ec.dgc.revocationdistribution.exception.PreconditionFaildException;
import europa.ec.dgc.revocationdistribution.service.InfoService;
import europa.ec.dgc.revocationdistribution.service.RevocationListService;
import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import javax.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
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

        return ResponseEntity.ok().eTag(currentEtag).body(revocationListJsonEntity.get().getJsonData());
    }


    /**
     * Http Method for getting the all partitions a kid.
     * @return
     */
    @GetMapping(path = "lists/{kid}/partitions", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<PartitionResponseDto>> getPartitionListForKid(
        @PathVariable String kid,
        @RequestHeader(value = HttpHeaders.IF_MATCH, required = true) String ifMatch,
        @RequestHeader(value = HttpHeaders.IF_MODIFIED_SINCE, required = false) String ifModifiedSince
    ) {
        
        kid = transformBase64Url(kid);

        String currentEtag = infoService.getValueForKey(InfoService.CURRENT_ETAG);

        if (!ifMatch.equals(currentEtag)) {
            log.info("etag failed given {} expexted {}", ifMatch, currentEtag);
            throw new PreconditionFaildException();
        }
        List<PartitionResponseDto> result;

        if (ifModifiedSince != null) {
            ZonedDateTime ifModifiedDateTime;
            try {
                ifModifiedDateTime = ZonedDateTime.parse(ifModifiedSince);
            } catch (DateTimeParseException e) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
            }
            result = revocationListService.getPartitionsByKidAndDate(kid, ifModifiedDateTime);
        } else {
            result = revocationListService.getPartitionsByKid(currentEtag, kid);
        }

        if (result.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_MODIFIED).build();
        }

        return ResponseEntity.ok(result);
    }

    /**
     * Http Method for getting the a partition of a kid.
     * @return
     */
    @GetMapping(path = "lists/{kid}/partitions/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<PartitionResponseDto> getPartitionForKid(
        @PathVariable String kid,
        @PathVariable String id,
        @RequestHeader(value = HttpHeaders.IF_MATCH, required = true) String ifMatch,
        @RequestHeader(value = HttpHeaders.IF_MODIFIED_SINCE, required = false) String ifModifiedSince
    ) {

        kid = transformBase64Url(kid);
        
        String currentEtag = infoService.getValueForKey(InfoService.CURRENT_ETAG);

        if (!ifMatch.equals(currentEtag)) {
            log.info("etag failed given {} expexted {}", ifMatch, currentEtag);
            throw new PreconditionFaildException();
        }
        PartitionResponseDto result;

        if (ifModifiedSince != null) {
            ZonedDateTime ifModifiedDateTime;
            try {
                ifModifiedDateTime = ZonedDateTime.parse(ifModifiedSince);
            } catch (DateTimeParseException e) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
            }
            result = revocationListService.getPartitionsByKidAndId(currentEtag, kid, id );// ifModifiedDateTime);
        } else {
            result = revocationListService.getPartitionsByKidAndId(currentEtag, kid, id);
        }


        return ResponseEntity.ok(result);
    }


    /**
     * Http Method for getting the data of a partition.
     * @return gzip file containing data
     */
    @PostMapping(path = "lists/{kid}/partitions/{id}/slices",
        consumes = MediaType.APPLICATION_JSON_VALUE,
        produces ="application/gzip")
    public ResponseEntity<byte[]> getPartitionChunksData(
        @PathVariable String kid,
        @PathVariable String id,
        @RequestHeader(value = HttpHeaders.IF_MATCH, required = true) String ifMatch,
        @Valid @RequestBody(required = false)  List<String> reqestedChunksList
    ) {

        kid = transformBase64Url(kid);
        
        String currentEtag = infoService.getValueForKey(InfoService.CURRENT_ETAG);

        if (!ifMatch.equals(currentEtag)) {
            throw new PreconditionFaildException();
        }
        byte[] result;

        if (reqestedChunksList == null){

            result = revocationListService.getAllChunkDataFromPartition(currentEtag, kid, id);
        }
        else {
            result = revocationListService.getAllChunkDataFromPartitionWithFilter(currentEtag, kid, id, reqestedChunksList);
        }

        return ResponseEntity.ok(result);
    }

    /**
     * Http Method for getting the slice data.
     * @return gzip file containing slice data
     */
    @GetMapping(path = "lists/{kid}/partitions/{id}/chunks/{cid}/slices", produces ="application/gzip")
    public ResponseEntity<byte[]> getChunk(
        @PathVariable String kid,
        @PathVariable String id,
        @PathVariable String cid,
        @RequestHeader(value = HttpHeaders.IF_MATCH, required = true) String ifMatch
    ) {
        
        kid = transformBase64Url(kid);

        String currentEtag = infoService.getValueForKey(InfoService.CURRENT_ETAG);

        if (!ifMatch.equals(currentEtag)) {
            throw new PreconditionFaildException();
        }

        byte[] result = revocationListService.getChunkData(currentEtag, kid, id, cid);

        return ResponseEntity.ok(result);
    }

    /**
     * Http Method for getting the data of a partition.
     * @return gzip file containing data
     */
    @PostMapping(path = "lists/{kid}/partitions/{id}/chunks/{cid}/slices",
        consumes = MediaType.APPLICATION_JSON_VALUE,
        produces ="application/gzip")
    public ResponseEntity<byte[]> getPartitionChunks(
        @PathVariable String kid,
        @PathVariable String id,
        @PathVariable String cid,
        @RequestHeader(value = HttpHeaders.IF_MATCH, required = true) String ifMatch,
        @Valid @RequestBody(required = false)  List<String> reqestedSliceList
    ) {
        
        kid = transformBase64Url(kid);

        String currentEtag = infoService.getValueForKey(InfoService.CURRENT_ETAG);

        if (!ifMatch.equals(currentEtag)) {
            throw new PreconditionFaildException();
        }
        byte[] result;

        if (reqestedSliceList == null){
            result = revocationListService.getChunkData(currentEtag, kid, id, cid);
        }
        else {
            result = revocationListService.getAllSliceDataForChunkWithFilter(currentEtag, kid, id, cid, reqestedSliceList);
        }

        return ResponseEntity.ok(result);
    }



    /**
     * Http Method for getting the slice data.
     * @return gzip file containing slice data
     */
    @GetMapping(path = "lists/{kid}/partitions/{id}/chunks/{cid}/slices/{sid}",
        produces ="application/gzip")
    public ResponseEntity<byte[]> getSlice(
        @PathVariable String kid,
        @PathVariable String id,
        @PathVariable String cid,
        @PathVariable String sid,
        @RequestHeader(value = HttpHeaders.IF_MATCH, required = true) String ifMatch,
        @RequestHeader(value = HttpHeaders.IF_MODIFIED_SINCE, required = false) String ifModifiedSince
    ) {
        
        kid = transformBase64Url(kid);

        String currentEtag = infoService.getValueForKey(InfoService.CURRENT_ETAG);

        if (!ifMatch.equals(currentEtag)) {
            throw new PreconditionFaildException();
        }

        byte[] result = revocationListService.getSliceData(currentEtag, kid, id, cid, sid);

        if (result== null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        return ResponseEntity.ok(result);
    }
    
     /**
     * Method to transform a base64url object
     * returns a base64 object from a base64url object
     */
    private String transformBase64Url(String kid) {
        return Base64.getEncoder().encodeToString(Base64URL.from(kid).decode());
    }

}
