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
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import java.util.ArrayList;
import java.util.List;
import javax.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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
     * Http Method for looking up the revocation state for a list of revocation check tokens.
     * @param revocationCheckTokenList The List of tokens to check the state for.
     * @return the revocation status of the given certificates.
     */
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
        summary = "Locks up the revocation state for a list of revocation check tokens.",
        description = "This method returns a list of hashes, which check got a positive result for revocation. "
            + "The certificates to be checked, must be provided as list of revocation check tokens (JWT tokens) in "
            + "the request boddy.",
        tags = {"Revocation Lookup"},
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = false,
            content = @Content(array = @ArraySchema(
                schema = @Schema(implementation = String.class, name = "JWT token")))
        ),
        responses = {
            @ApiResponse(
                responseCode = "200",
                description = "Returns all a list of all hashes from the request tokens, that are revoked.",
                content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    array = @ArraySchema(schema = @Schema(implementation = String.class)))),
            @ApiResponse(
                responseCode = "400",
                description = "Returned on wrong or missing request parameters.",
                content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = String.class))),
            @ApiResponse(
                responseCode = "404",
                description = "Returned if public key could not be found.",
                content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = String.class)))
        }
    )
    public ResponseEntity<List<String>> lookupRevocation(
        @Valid @RequestBody(required = false) List<String> revocationCheckTokenList
    ) {
        if (revocationCheckTokenList.isEmpty()) {
            return ResponseEntity.ok(new ArrayList<>());
        }

        List<RevocationCheckTokenPayload> tokenPayloads =
            lookupService.validateRevocationCheckTokens(revocationCheckTokenList);

        List<String> result = lookupService.checkForRevocation(tokenPayloads);
        return ResponseEntity.ok(result);
    }

}

