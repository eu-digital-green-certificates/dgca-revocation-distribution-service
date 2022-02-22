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

import com.nimbusds.jose.util.Base64URL;
import eu.europa.ec.dgc.revocationdistribution.dto.PartitionResponseDto;
import eu.europa.ec.dgc.revocationdistribution.dto.RevocationListJsonResponseDto;
import eu.europa.ec.dgc.revocationdistribution.entity.RevocationListJsonEntity;
import eu.europa.ec.dgc.revocationdistribution.exception.PreconditionFailedException;
import eu.europa.ec.dgc.revocationdistribution.service.InfoService;
import eu.europa.ec.dgc.revocationdistribution.service.RevocationListService;
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
     * @param ifNoneMatch if present, it is checked, if new data is available since the last response with this etag.
     * @return revocation list as json
     */
    @GetMapping(path = "lists", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<RevocationListJsonResponseDto.RevocationListJsonResponseItemDto>> getRevocationList(
        @RequestHeader(value = HttpHeaders.IF_NONE_MATCH, defaultValue = "") String ifNoneMatch) {

        String currentEtag = infoService.getValueForKey(InfoService.CURRENT_ETAG);

        if (ifNoneMatch.equals(currentEtag)) {
            return ResponseEntity.status(HttpStatus.NOT_MODIFIED).build();
        }

        Optional<RevocationListJsonEntity> revocationListJsonEntity =
            revocationListService.getRevocationListJsonData(currentEtag);

        if (!revocationListJsonEntity.isPresent()) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok().eTag(currentEtag).body(revocationListJsonEntity.get().getJsonData());
    }


    /**
     * Http Method for getting the all partitions a kid.
     * @param ifMatch must match the actual revocation list / available data set
     * @param kid the kid for which the partitions are requested.
     * @return a list of partitions meta data
     */
    @GetMapping(path = "lists/{kid}/partitions", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<PartitionResponseDto>> getPartitionListForKid(
        @PathVariable String kid,
        @RequestHeader(value = HttpHeaders.IF_MATCH, required = true) String ifMatch,
        @RequestHeader(value = HttpHeaders.IF_MODIFIED_SINCE, required = false) String ifModifiedSince
    ) {

        kid = transformBase64Url(kid);

        String currentEtag = checkEtag(ifMatch);

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
     * Http Method for getting a particular partition of a kid.
     * @param ifMatch must match the actual revocation list / available data set
     * @param kid the kid for which the partitions are requested.
     * @param id the id of the requested partition
     *
     * @return the partition meta data.
     */
    @GetMapping(path = "lists/{kid}/partitions/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<PartitionResponseDto> getPartitionForKid(
        @PathVariable String kid,
        @PathVariable String id,
        @RequestHeader(value = HttpHeaders.IF_MATCH, required = true) String ifMatch,
        @RequestHeader(value = HttpHeaders.IF_MODIFIED_SINCE, required = false) String ifModifiedSince
    ) {

        kid = transformBase64Url(kid);

        String currentEtag = checkEtag(ifMatch);

        PartitionResponseDto result;

        if (ifModifiedSince != null) {
            ZonedDateTime ifModifiedDateTime;
            try {
                ifModifiedDateTime = ZonedDateTime.parse(ifModifiedSince);
            } catch (DateTimeParseException e) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
            }
            result = revocationListService.getPartitionsByKidAndId(currentEtag, kid, id);// ifModifiedDateTime);
        } else {
            result = revocationListService.getPartitionsByKidAndId(currentEtag, kid, id);
        }


        return ResponseEntity.ok(result);
    }


    /**
     * Http Method for getting the  binary data of a partition.
     * @param ifMatch must match the actual revocation list / available data set
     * @param kid the kid for which the partitions are requested.
     * @param id the id of the requested partition
     *
     * @return gzip file containing binary slice data of the partition.
     */
    @PostMapping(path = "lists/{kid}/partitions/{id}/slices",
        consumes = MediaType.APPLICATION_JSON_VALUE,
        produces = "application/gzip")
    public ResponseEntity<byte[]> getPartitionChunksData(
        @PathVariable String kid,
        @PathVariable String id,
        @RequestHeader(value = HttpHeaders.IF_MATCH, required = true) String ifMatch,
        @Valid @RequestBody(required = false) List<String> reqestedChunksList
    ) {

        kid = transformBase64Url(kid);

        String currentEtag = checkEtag(ifMatch);

        byte[] result;

        if (reqestedChunksList == null) {

            result = revocationListService.getAllChunkDataFromPartition(currentEtag, kid, id);
        } else {
            result = revocationListService.getAllChunkDataFromPartitionWithFilter(
                currentEtag, kid, id, reqestedChunksList);
        }

        return ResponseEntity.ok(result);
    }

    /**
     * Http Method for getting the slice data of a chunk.
     * @param ifMatch must match the actual revocation list / available data set
     * @param kid the kid for which the partitions are requested
     * @param id the id of the requested partition
     * @param cid the id of the requested chunk
     *
     * @return gzip file containing binary slice data of a chunk
     */
    @GetMapping(path = "lists/{kid}/partitions/{id}/chunks/{cid}/slices", produces = "application/gzip")
    public ResponseEntity<byte[]> getChunk(
        @PathVariable String kid,
        @PathVariable String id,
        @PathVariable String cid,
        @RequestHeader(value = HttpHeaders.IF_MATCH, required = true) String ifMatch
    ) {

        kid = transformBase64Url(kid);

        String currentEtag = checkEtag(ifMatch);


        byte[] result = revocationListService.getChunkData(currentEtag, kid, id, cid);

        return ResponseEntity.ok(result);
    }

    /**
     * Http Method for getting a selection of slice data of a chunk.
     * @param ifMatch must match the actual revocation list / available data set
     * @param kid the kid for which the partitions are requested
     * @param id the id of the requested partition
     * @param cid the id of the requested chunk
     * @param reqestedSliceList list of slices to download
     *
     * @return gzip file containing binary slice data of a chunk
     */
    @PostMapping(path = "lists/{kid}/partitions/{id}/chunks/{cid}/slices",
        consumes = MediaType.APPLICATION_JSON_VALUE,
        produces = "application/gzip")
    public ResponseEntity<byte[]> getPartitionChunks(
        @PathVariable String kid,
        @PathVariable String id,
        @PathVariable String cid,
        @RequestHeader(value = HttpHeaders.IF_MATCH, required = true) String ifMatch,
        @Valid @RequestBody(required = false) List<String> reqestedSliceList
    ) {

        kid = transformBase64Url(kid);

        String currentEtag = checkEtag(ifMatch);

        byte[] result;

        if (reqestedSliceList == null) {
            result = revocationListService.getChunkData(currentEtag, kid, id, cid);
        } else {
            result = revocationListService.getAllSliceDataForChunkWithFilter(
                currentEtag, kid, id, cid, reqestedSliceList);
        }

        return ResponseEntity.ok(result);
    }


    /**
     * Http Method for getting specific slice data.
     * @param ifMatch must match the actual revocation list / available data set
     * @param kid the kid for which the partitions are requested
     * @param id the id of the requested partition
     * @param cid the id of the requested chunk
     * @param sid the id of the slice to download
     *
     * @return gzip file containing binary slice data
     */
    @GetMapping(path = "lists/{kid}/partitions/{id}/chunks/{cid}/slices/{sid}",
        produces = "application/gzip")
    public ResponseEntity<byte[]> getSlice(
        @PathVariable String kid,
        @PathVariable String id,
        @PathVariable String cid,
        @PathVariable String sid,
        @RequestHeader(value = HttpHeaders.IF_MATCH, required = true) String ifMatch,
        @RequestHeader(value = HttpHeaders.IF_MODIFIED_SINCE, required = false) String ifModifiedSince
    ) {

        kid = transformBase64Url(kid);

        String currentEtag = checkEtag(ifMatch);

        byte[] result = revocationListService.getSliceData(currentEtag, kid, id, cid, sid);

        if (result == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        return ResponseEntity.ok(result);
    }

    /**
     * Method to transform a base64url object.
     * returns a base64 object from a base64url object
     */
    private String transformBase64Url(String kid) {
        return Base64.getEncoder().encodeToString(Base64URL.from(kid).decode());
    }

    /**
     * Method to check Etag Header.
     *
     * @param etag to check
     * @return etag without quotes
     * @throws PreconditionFailedException is thrown when the given etag don't match the current one.
     */
    private String checkEtag(String etag) throws PreconditionFailedException {
        String currentEtag = infoService.getValueForKey(InfoService.CURRENT_ETAG);
        String parsedEtag = etag.replaceAll("^\"|\"$", "");

        if (!parsedEtag.equals(currentEtag)) {
            log.info("etag failed given {} expexted {}", etag, currentEtag);
            throw new PreconditionFailedException();
        }
        return currentEtag;
    }

}
