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
import eu.europa.ec.dgc.revocationdistribution.config.DgcConfigProperties;
import eu.europa.ec.dgc.revocationdistribution.dto.PartitionResponseDto;
import eu.europa.ec.dgc.revocationdistribution.dto.RevocationListJsonResponseDto;
import eu.europa.ec.dgc.revocationdistribution.entity.RevocationListJsonEntity;
import eu.europa.ec.dgc.revocationdistribution.exception.BadRequestException;
import eu.europa.ec.dgc.revocationdistribution.exception.PreconditionFailedException;
import eu.europa.ec.dgc.revocationdistribution.model.SliceType;
import eu.europa.ec.dgc.revocationdistribution.service.InfoService;
import eu.europa.ec.dgc.revocationdistribution.service.RevocationListService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
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
@SuppressWarnings({"DefaultAnnotationParam"})
public class RevocationListController {

    private static final String SLICE_DATA_TYPE_HEADER = "X-SLICE-FILTER-TYPE";
    private final InfoService infoService;
    private final DgcConfigProperties properties;
    private final RevocationListService revocationListService;


    /**
     * Http Method for getting the revocation list.
     * @param ifNoneMatch if present, it is checked, if new data is available since the last response with this etag.
     * @return revocation list as json
     */
    @GetMapping(path = "lists", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
        summary = "Returns an overview about all available revocation lists.",
        description = "This method returns an overview about available revocation lists for each KID. The response "
            + "contains for all available KIDs the last modification date, the used hash types etc.",
        tags = {"Revocation Lists"},
        parameters = {
            @Parameter(
                in = ParameterIn.HEADER,
                name = "IF-NONE-MATCH",
                description = "When the eTag matches the current Tag, there is a 304 response.",
                required = false,
                schema = @Schema(implementation = String.class))
        },
        responses = {
            @ApiResponse(
                responseCode = "200",
                description = "Returns the overview about available lists.",
                headers = @Header(name = HttpHeaders.ETAG, description = "ETAG of the current data set"),
                content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    array = @ArraySchema(schema =
                    @Schema(implementation = RevocationListJsonResponseDto.RevocationListJsonResponseItemDto.class)))),
            @ApiResponse(
                responseCode = "304",
                description = "Not modified.")
        }
    )
    public ResponseEntity<List<RevocationListJsonResponseDto.RevocationListJsonResponseItemDto>> getRevocationList(
        @RequestHeader(value = HttpHeaders.IF_NONE_MATCH, defaultValue = "") String ifNoneMatch) {

        String currentEtag = infoService.getValueForKey(InfoService.CURRENT_ETAG);

        if (ifNoneMatch.equals(currentEtag)) {
            return ResponseEntity.status(HttpStatus.NOT_MODIFIED).build();
        }

        Optional<RevocationListJsonEntity> revocationListJsonEntity =
            revocationListService.getRevocationListJsonData(currentEtag);

        return revocationListJsonEntity.map(listJsonEntity -> ResponseEntity.ok().eTag(currentEtag)
            .body(listJsonEntity.getJsonData())).orElseGet(() -> ResponseEntity.notFound().build());

    }


    /**
     * Http Method for getting the all partitions a kid.
     * @param ifMatch must match the actual revocation list / available data set
     * @param kid the kid for which the partitions are requested.
     * @return a list of partitions meta data
     */
    @GetMapping(path = "lists/{kid}/partitions", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
        summary = "Returns for the selected kid all Partitions.",
        description = "Returns a list of all available partitions for a kid.",
        tags = {"Revocation Lists"},
        parameters = {
            @Parameter(
                in = ParameterIn.HEADER,
                name = "If-Match",
                description = "When eTag matches (received from /lists), the call will be executed.",
                required = true,
                schema = @Schema(implementation = String.class)),
            @Parameter(
                in = ParameterIn.HEADER,
                name = "If-Modified-Since",
                description = "Returns only the objects which are modified behind the given date.",
                required = false,
                schema = @Schema(implementation = String.class)),
            @Parameter(
                in = ParameterIn.HEADER,
                name =  SLICE_DATA_TYPE_HEADER,
                description = "Can be used to select the filter type of the slice data, if the backend offers more than"
                    + " one type. Possible values are BLOOMFILTER, VARHASHLIST",
                required = false,
                schema = @Schema(implementation = String.class)),
            @Parameter(
                in = ParameterIn.PATH,
                name = "kid",
                description = "The kid, for which the partition should be returned.",
                required = true,
                schema = @Schema(implementation = String.class))
        },
        responses = {
            @ApiResponse(
                responseCode = "200",
                description = "Returns a list of partitions for the kid.",
                content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    array = @ArraySchema(schema =
                    @Schema(implementation = PartitionResponseDto.class)))),
            @ApiResponse(responseCode = "304",description = "Not modified."),
            @ApiResponse(responseCode = "404",description = "Data not found."),
            @ApiResponse(responseCode = "412",description = "Pre-Condition Failed.")
        }
    )
    public ResponseEntity<List<PartitionResponseDto>> getPartitionListForKid(
        @PathVariable String kid,
        @RequestHeader(value = HttpHeaders.IF_MATCH, required = true) String ifMatch,
        @RequestHeader(value = HttpHeaders.IF_MODIFIED_SINCE, required = false) String ifModifiedSince,
        @RequestHeader(value = SLICE_DATA_TYPE_HEADER, required = false) String sliceDataTypeHeader
    ) {

        kid = transformBase64Url(kid);

        String currentEtag = checkEtag(ifMatch);

        List<PartitionResponseDto> result;

        SliceType dataType = getSliceDataType(sliceDataTypeHeader);

        if (ifModifiedSince != null) {
            ZonedDateTime ifModifiedDateTime;
            try {
                ifModifiedDateTime = ZonedDateTime.parse(ifModifiedSince);
            } catch (DateTimeParseException e) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
            }
            result = revocationListService.getPartitionsByKidAndDate(currentEtag, kid, dataType, ifModifiedDateTime);
        } else {
            result = revocationListService.getPartitionsByKid(currentEtag, kid, dataType);
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
    @Operation(
        summary = "Returns for the selected kid a Partition.",
        description = "Returns a Partition by Id.",
        tags = {"Revocation Lists"},
        parameters = {
            @Parameter(
                in = ParameterIn.HEADER,
                name = "If-Match",
                description = "When eTag matches (received from /lists), the call will be executed.",
                required = true,
                schema = @Schema(implementation = String.class)),
            @Parameter(
                in = ParameterIn.HEADER,
                name = "If-Modified-Since",
                description = "Returns only the objects which are modified behind the given date.",
                required = false,
                schema = @Schema(implementation = String.class)),
            @Parameter(
                in = ParameterIn.HEADER,
                name =  SLICE_DATA_TYPE_HEADER,
                description = "Can be used to select the filter type of the slice data, if the backend offers more than"
                    + " one type. Possible values are BLOOMFILTER, VARHASHLIST",
                required = false,
                schema = @Schema(implementation = String.class)),
            @Parameter(
                in = ParameterIn.PATH,
                name = "kid",
                description = "The kid, for which the partition should be returned.",
                required = true,
                schema = @Schema(implementation = String.class)),
            @Parameter(
                in = ParameterIn.PATH,
                name = "id",
                description = "The id of the partition within the kid.",
                required = true,
                schema = @Schema(implementation = String.class))
        },
        responses = {
            @ApiResponse(
                responseCode = "200",
                description = "Returns the partitions for the kid.",
                content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = PartitionResponseDto.class))),
            @ApiResponse(responseCode = "304",description = "Not modified."),
            @ApiResponse(responseCode = "404",description = "Data not found."),
            @ApiResponse(responseCode = "412",description = "Pre-Condition Failed.")
        }
    )
    public ResponseEntity<PartitionResponseDto> getPartitionForKid(
        @PathVariable String kid,
        @PathVariable String id,
        @RequestHeader(value = HttpHeaders.IF_MATCH, required = true) String ifMatch,
        @RequestHeader(value = HttpHeaders.IF_MODIFIED_SINCE, required = false) String ifModifiedSince,
        @RequestHeader(value = SLICE_DATA_TYPE_HEADER, required = false) String sliceDataTypeHeader
    ) {

        kid = transformBase64Url(kid);

        String currentEtag = checkEtag(ifMatch);

        SliceType dataType = getSliceDataType(sliceDataTypeHeader);

        PartitionResponseDto result;

        if (ifModifiedSince != null) {
            ZonedDateTime ifModifiedDateTime;
            try {
                ifModifiedDateTime = ZonedDateTime.parse(ifModifiedSince);
            } catch (DateTimeParseException e) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
            }
            result = revocationListService.getPartitionsByKidAndIdAndDate(
                currentEtag, kid, id, dataType, ifModifiedDateTime);
        } else {
            result = revocationListService.getPartitionsByKidAndId(currentEtag, kid, id, dataType);
        }

        return ResponseEntity.ok(result);
    }


    /**
     * Http Method for getting the  binary data of a partition.
     * @param ifMatch must match the actual revocation list / available data set
     * @param ifModifiedSince only data newer than this date is returned
     * @param kid the kid for which the partitions are requested.
     * @param id the id of the requested partition
     *
     * @return gzip file containing binary slice data of the partition.
     */
    @PostMapping(path = "lists/{kid}/partitions/{id}/slices",
        consumes = MediaType.APPLICATION_JSON_VALUE,
        produces = "application/gzip")
    @Operation(
        summary = "Returns all slice data for the selected partition.",
        description = "Returns all slices binary data in a gzip file for a partition. The result set can be filtered "
            + "by a provided whitelist in the request body.",
        tags = {"Revocation Lists"},
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = false,
            content = @Content(array = @ArraySchema(
                schema = @Schema(implementation = String.class, name = "Slice id (sid)")))
        ),
        parameters = {
            @Parameter(
                in = ParameterIn.HEADER,
                name = "If-Match",
                description = "When eTag matches (received from /lists), the call will be executed.",
                required = true,
                schema = @Schema(implementation = String.class)),
            @Parameter(
                in = ParameterIn.HEADER,
                name = "If-Modified-Since",
                description = "Returns only the objects which are modified behind the given date.",
                required = false,
                schema = @Schema(implementation = String.class)),
            @Parameter(
                in = ParameterIn.HEADER,
                name =  SLICE_DATA_TYPE_HEADER,
                description = "Can be used to select the filter type of the slice data, if the backend offers more than"
                    + " one type. Possible values are BLOOMFILTER, VARHASHLIST",
                required = false,
                schema = @Schema(implementation = String.class)),
            @Parameter(
                in = ParameterIn.PATH,
                name = "kid",
                description = "The kid, for which the data should be returned.",
                required = true,
                schema = @Schema(implementation = String.class)),
            @Parameter(
                in = ParameterIn.PATH,
                name = "id",
                description = "The id of the partition.",
                required = true,
                schema = @Schema(implementation = String.class))
        },
        responses = {
            @ApiResponse(
                responseCode = "200",
                description = "Returns the binary slice data as gzip file.",
                content = @Content(
                    mediaType = "application/gzip")),
            @ApiResponse(responseCode = "304",description = "Not modified."),
            @ApiResponse(responseCode = "404",description = "Data not found."),
            @ApiResponse(responseCode = "412",description = "Pre-Condition Failed.")
        }
    )
    public ResponseEntity<byte[]> getPartitionChunksData(
        @PathVariable String kid,
        @PathVariable String id,
        @RequestHeader(value = HttpHeaders.IF_MATCH, required = true) String ifMatch,
        @RequestHeader(value = HttpHeaders.IF_MODIFIED_SINCE, required = false) String ifModifiedSince,
        @RequestHeader(value = SLICE_DATA_TYPE_HEADER, required = false) String sliceDataTypeHeader,
        @Valid @RequestBody(required = false) List<String> requestedChunksList
    ) {

        kid = transformBase64Url(kid);

        String currentEtag = checkEtag(ifMatch);

        SliceType dataType = getSliceDataType(sliceDataTypeHeader);

        byte[] result;

        if (ifModifiedSince != null) {
            ZonedDateTime ifModifiedDateTime;
            try {
                ifModifiedDateTime = ZonedDateTime.parse(ifModifiedSince);
            } catch (DateTimeParseException e) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
            }
            if (requestedChunksList == null) {

                result = revocationListService.getAllChunkDataFromPartitionSinceDate(
                    currentEtag, kid, id, dataType, ifModifiedDateTime);
            } else {
                result = revocationListService.getAllChunkDataFromPartitionWithFilterSinceDate(
                    currentEtag, kid, id, dataType, requestedChunksList, ifModifiedDateTime);
            }
        } else {
            if (requestedChunksList == null) {

                result = revocationListService.getAllChunkDataFromPartition(currentEtag, kid, id, dataType);
            } else {
                result = revocationListService.getAllChunkDataFromPartitionWithFilter(
                    currentEtag, kid, id, dataType, requestedChunksList);
            }
        }
        return ResponseEntity.ok(result);
    }

    /**
     * Http Method for getting the slice data of a chunk.
     * @param ifMatch must match the actual revocation list / available data set
     * @param ifModifiedSince only data newer than this date is returned
     * @param kid the kid for which the partitions are requested
     * @param id the id of the requested partition
     * @param cid the id of the requested chunk
     *
     * @return gzip file containing binary slice data of a chunk
     */
    @GetMapping(path = "lists/{kid}/partitions/{id}/chunks/{cid}/slices", produces = "application/gzip")
    @Operation(
        summary = "Returns all slice data for the selected chunk.",
        description = "Returns all slices binary data in a gzip file for the selected chunk. ",
        tags = {"Revocation Lists"},
        parameters = {
            @Parameter(
                in = ParameterIn.HEADER,
                name = "If-Match",
                description = "When eTag matches (received from /lists), the call will be executed.",
                required = true,
                schema = @Schema(implementation = String.class)),
            @Parameter(
                in = ParameterIn.HEADER,
                name = "If-Modified-Since",
                description = "Returns only the objects which are modified behind the given date.",
                required = false,
                schema = @Schema(implementation = String.class)),
            @Parameter(
                in = ParameterIn.HEADER,
                name =  SLICE_DATA_TYPE_HEADER,
                description = "Can be used to select the filter type of the slice data, if the backend offers more than"
                    + " one type. Possible values are BLOOMFILTER, VARHASHLIST",
                required = false,
                schema = @Schema(implementation = String.class)),
            @Parameter(
                in = ParameterIn.PATH,
                name = "kid",
                description = "The kid, for which the data should be returned.",
                required = true,
                schema = @Schema(implementation = String.class)),
            @Parameter(
                in = ParameterIn.PATH,
                name = "id",
                description = "The id of the partition.",
                required = true,
                schema = @Schema(implementation = String.class)),
            @Parameter(
                in = ParameterIn.PATH,
                name = "cid",
                description = "The id of the chunk.",
                required = true,
                schema = @Schema(implementation = String.class))
        },
        responses = {
            @ApiResponse(
                responseCode = "200",
                description = "Returns the binary slice data as gzip file.",
                content = @Content(
                    mediaType = "application/gzip")),
            @ApiResponse(responseCode = "304",description = "Not modified."),
            @ApiResponse(responseCode = "404",description = "Data not found."),
            @ApiResponse(responseCode = "412",description = "Pre-Condition Failed.")
        }
    )
    public ResponseEntity<byte[]> getChunk(
        @PathVariable String kid,
        @PathVariable String id,
        @PathVariable String cid,
        @RequestHeader(value = HttpHeaders.IF_MATCH, required = true) String ifMatch,
        @RequestHeader(value = HttpHeaders.IF_MODIFIED_SINCE, required = false) String ifModifiedSince,
        @RequestHeader(value = SLICE_DATA_TYPE_HEADER, required = false) String sliceDataTypeHeader
    ) {

        kid = transformBase64Url(kid);

        String currentEtag = checkEtag(ifMatch);

        SliceType dataType = getSliceDataType(sliceDataTypeHeader);

        byte[] result;

        if (ifModifiedSince != null) {
            ZonedDateTime ifModifiedDateTime;
            try {
                ifModifiedDateTime = ZonedDateTime.parse(ifModifiedSince);
            } catch (DateTimeParseException e) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
            }
            result = revocationListService.getChunkDataSinceDate(
                currentEtag, kid, id, cid, dataType, ifModifiedDateTime);
        } else {
            result = revocationListService.getChunkData(currentEtag, kid, id, cid, dataType);
        }

        return ResponseEntity.ok(result);
    }

    /**
     * Http Method for getting a selection of slice data of a chunk.
     * @param ifMatch must match the actual revocation list / available data set
     * @param ifModifiedSince only data newer than this date is returned
     * @param kid the kid for which the partitions are requested
     * @param id the id of the requested partition
     * @param cid the id of the requested chunk
     * @param requestedSliceList list of slices to download
     *
     * @return gzip file containing binary slice data of a chunk
     */
    @PostMapping(path = "lists/{kid}/partitions/{id}/chunks/{cid}/slices",
        consumes = MediaType.APPLICATION_JSON_VALUE,
        produces = "application/gzip")
    @Operation(
        summary = "Returns all slice data for the selected chunk.",
        description = "Returns all slices binary data in a gzip file for a selected chunk. "
            + "The result set can be filtered by a provided whitelist in the request body.",
        tags = {"Revocation Lists"},
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = false,
            content = @Content(array = @ArraySchema(
                schema = @Schema(implementation = String.class, name = "Slice id (sid)")))
        ),
        parameters = {
            @Parameter(
                in = ParameterIn.HEADER,
                name = "If-Match",
                description = "When eTag matches (received from /lists), the call will be executed.",
                required = true,
                schema = @Schema(implementation = String.class)),
            @Parameter(
                in = ParameterIn.HEADER,
                name = "If-Modified-Since",
                description = "Returns only the objects which are modified behind the given date.",
                required = false,
                schema = @Schema(implementation = String.class)),
            @Parameter(
                in = ParameterIn.HEADER,
                name =  SLICE_DATA_TYPE_HEADER,
                description = "Can be used to select the filter type of the slice data, if the backend offers more than"
                    + " one type. Possible values are BLOOMFILTER, VARHASHLIST",
                required = false,
                schema = @Schema(implementation = String.class)),
            @Parameter(
                in = ParameterIn.PATH,
                name = "kid",
                description = "The kid, for which the data should be returned.",
                required = true,
                schema = @Schema(implementation = String.class)),
            @Parameter(
                in = ParameterIn.PATH,
                name = "id",
                description = "The id of the partition.",
                required = true,
                schema = @Schema(implementation = String.class)),
            @Parameter(
                in = ParameterIn.PATH,
                name = "cid",
                description = "The id of the chunk.",
                required = true,
                schema = @Schema(implementation = String.class))
        },
        responses = {
            @ApiResponse(
                responseCode = "200",
                description = "Returns the binary slice data as gzip file.",
                content = @Content(
                    mediaType = "application/gzip")),
            @ApiResponse(responseCode = "304",description = "Not modified."),
            @ApiResponse(responseCode = "404",description = "Data not found."),
            @ApiResponse(responseCode = "412",description = "Pre-Condition Failed.")
        }
    )
    public ResponseEntity<byte[]> getPartitionChunks(
        @PathVariable String kid,
        @PathVariable String id,
        @PathVariable String cid,
        @RequestHeader(value = HttpHeaders.IF_MATCH, required = true) String ifMatch,
        @RequestHeader(value = HttpHeaders.IF_MODIFIED_SINCE, required = false) String ifModifiedSince,
        @RequestHeader(value = SLICE_DATA_TYPE_HEADER, required = false) String sliceDataTypeHeader,
        @Valid @RequestBody(required = false) List<String> requestedSliceList
    ) {

        kid = transformBase64Url(kid);

        String currentEtag = checkEtag(ifMatch);

        SliceType dataType = getSliceDataType(sliceDataTypeHeader);

        byte[] result;

        if (ifModifiedSince != null) {
            ZonedDateTime ifModifiedDateTime;
            try {
                ifModifiedDateTime = ZonedDateTime.parse(ifModifiedSince);
            } catch (DateTimeParseException e) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
            }
            if (requestedSliceList == null) {
                result = revocationListService.getChunkDataSinceDate(
                    currentEtag, kid, id, cid, dataType, ifModifiedDateTime);
            } else {
                result = revocationListService.getAllSliceDataForChunkWithFilterSinceDate(
                    currentEtag, kid, id, cid, dataType, requestedSliceList, ifModifiedDateTime);
            }
        } else {
            if (requestedSliceList == null) {
                result = revocationListService.getChunkData(currentEtag, kid, id, cid, dataType);
            } else {
                result = revocationListService.getAllSliceDataForChunkWithFilter(
                    currentEtag, kid, id, cid, dataType, requestedSliceList);
            }
        }

        return ResponseEntity.ok(result);
    }


    /**
     * Http Method for getting specific slice data.
     * @param ifMatch must match the actual revocation list / available data set
     * @param ifModifiedSince only newer data than this date is returned
     * @param kid the kid for which the partitions are requested
     * @param id the id of the requested partition
     * @param cid the id of the requested chunk
     * @param sid the id of the slice to download
     *
     * @return gzip file containing binary slice data
     */
    @GetMapping(path = "lists/{kid}/partitions/{id}/chunks/{cid}/slices/{sid}",
        produces = "application/gzip")
    @Operation(
        summary = "Returns the slice data for the selected slice.",
        description = "Returns the slices binary data in a gzip file for a selected slice.",
        tags = {"Revocation Lists"},
        parameters = {
            @Parameter(
                in = ParameterIn.HEADER,
                name = "If-Match",
                description = "When eTag matches (received from /lists), the call will be executed.",
                required = true,
                schema = @Schema(implementation = String.class)),
            @Parameter(
                in = ParameterIn.HEADER,
                name = "If-Modified-Since",
                description = "Returns only the objects which are modified behind the given date.",
                required = false,
                schema = @Schema(implementation = String.class)),
            @Parameter(
                in = ParameterIn.HEADER,
                name =  SLICE_DATA_TYPE_HEADER,
                description = "Can be used to select the filter type of the slice data, if the backend offers more than"
                    + " one type. Possible values are BLOOMFILTER, VARHASHLIST",
                required = false,
                schema = @Schema(implementation = String.class)),
            @Parameter(
                in = ParameterIn.PATH,
                name = "kid",
                description = "The kid, for which the data should be returned.",
                required = true,
                schema = @Schema(implementation = String.class)),
            @Parameter(
                in = ParameterIn.PATH,
                name = "id",
                description = "The id of the partition.",
                required = true,
                schema = @Schema(implementation = String.class)),
            @Parameter(
                in = ParameterIn.PATH,
                name = "cid",
                description = "The id of the chunk.",
                required = true,
                schema = @Schema(implementation = String.class)),
            @Parameter(
                in = ParameterIn.PATH,
                name = "sid",
                description = "The id of the slice.",
                required = true,
                schema = @Schema(implementation = String.class))
        },
        responses = {
            @ApiResponse(
                responseCode = "200",
                description = "Returns the binary slice data as gzip file.",
                content = @Content(
                    mediaType = "application/gzip")),
            @ApiResponse(responseCode = "304",description = "Not modified."),
            @ApiResponse(responseCode = "404",description = "Data not found."),
            @ApiResponse(responseCode = "412",description = "Pre-Condition Failed.")
        }
    )
    public ResponseEntity<byte[]> getSlice(
        @PathVariable String kid,
        @PathVariable String id,
        @PathVariable String cid,
        @PathVariable String sid,
        @RequestHeader(value = HttpHeaders.IF_MATCH, required = true) String ifMatch,
        @RequestHeader(value = HttpHeaders.IF_MODIFIED_SINCE, required = false) String ifModifiedSince,
        @RequestHeader(value = SLICE_DATA_TYPE_HEADER, required = false) String sliceDataTypeHeader
    ) {

        kid = transformBase64Url(kid);

        String currentEtag = checkEtag(ifMatch);

        SliceType dataType = getSliceDataType(sliceDataTypeHeader);

        byte[] result;

        if (ifModifiedSince != null) {
            ZonedDateTime ifModifiedDateTime;
            try {
                ifModifiedDateTime = ZonedDateTime.parse(ifModifiedSince);
            } catch (DateTimeParseException e) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
            }
            result = revocationListService.getSliceDataSinceDate(
                currentEtag, kid, id, cid, sid, dataType, ifModifiedDateTime);
        } else {
            result = revocationListService.getSliceData(currentEtag, kid, id, cid, sid, dataType);
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

    private SliceType getSliceDataType(String sliceDataTypeHeader) {
        if (sliceDataTypeHeader != null) {
            try {
                return SliceType.valueOf(sliceDataTypeHeader);
            } catch (IllegalArgumentException e) {
                log.info("Unknown slice data type requested {}", sliceDataTypeHeader);
                throw new BadRequestException("Requested slice data type unknown: " + sliceDataTypeHeader);
            }
        }
        return properties.getDefaultRevocationDataType();
    }

}
