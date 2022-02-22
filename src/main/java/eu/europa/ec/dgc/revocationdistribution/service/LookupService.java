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


import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.util.Base64URL;
import eu.europa.ec.dgc.revocationdistribution.client.IssuanceDgciRestClient;
import eu.europa.ec.dgc.revocationdistribution.dto.DidAuthentication;
import eu.europa.ec.dgc.revocationdistribution.dto.DidDocument;
import eu.europa.ec.dgc.revocationdistribution.dto.RevocationCheckTokenPayload;
import eu.europa.ec.dgc.revocationdistribution.exception.TokenValidationException;
import eu.europa.ec.dgc.revocationdistribution.repository.HashesRepository;
import feign.FeignException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwt;
import java.security.PublicKey;
import java.text.ParseException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.util.encoders.Hex;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Slf4j
@RequiredArgsConstructor
@Service
public class LookupService {

    private final RevocationCheckTokenParser revocationCheckTokenParser;

    private final IssuanceDgciRestClient issuanceDgciRestClient;

    private final HashesRepository hashesRepository;

    /**
     * Validates the given recocationCheckTokes. Therefore it checks the format and signature. For the signature check
     * the public key of the certificate is downloaded.
     * @param revocationCheckTokens list of revocation check tokens to be validated
     * @return List of token payload
     * @throws TokenValidationException is thrown when one of the tokens could not be validated
     */
    public List<RevocationCheckTokenPayload> validateRevocationCheckTokens(List<String> revocationCheckTokens)
        throws TokenValidationException {
        List<RevocationCheckTokenPayload> tokenPayloads = new ArrayList<>();

        for (String token : revocationCheckTokens) {
            Jwt jwt = revocationCheckTokenParser.extractPayload(token);

            Claims claims = (Claims) jwt.getBody();

            if (claims.containsKey("exp")
                && claims.getExpiration().toInstant().getEpochSecond() < Instant.now().getEpochSecond()) {
                log.warn("Invalid revocation check token: expired");
                throw new TokenValidationException("Invalid revocation check token: expired",
                    HttpStatus.BAD_REQUEST.value());
            }

            if (!claims.containsKey("sub") || !claims.containsKey("payload")) {
                log.warn("Invalid revocation check token: sub not found in token");
                throw new TokenValidationException("Required Fields Missing in token", HttpStatus.BAD_REQUEST.value());
            }

            PublicKey publicKey = downloadPublicKey(claims.getSubject());
            RevocationCheckTokenPayload payload = revocationCheckTokenParser.parseToken(token, publicKey);

            tokenPayloads.add(payload);
        }

        return tokenPayloads;
    }

    /**
     * Checks if the revocation status of the given tokens.
     * @param tokenPayloads tokens to be checked
     * @return list of revoked tokens an empty list meens none of the provided certificates / token are revoked.
     */

    public List<String> checkForRevocation(List<RevocationCheckTokenPayload> tokenPayloads) {

        List<String> hashes = tokenPayloads.stream().map(RevocationCheckTokenPayload::getPayload)
            .flatMap(List::stream).collect(Collectors.toList());

        return hashesRepository.getHashesPresentInListAndDb(hashes);

    }

    /**
     * Downloads the public key of a dcc from the issuance service.
     * @param hash of the cert, for which the public key is downloaded
     * @return Public key of the certificate
     * @throws TokenValidationException is thrown if the public key could not be determined
     */
    private PublicKey downloadPublicKey(String hash) throws TokenValidationException {
        ResponseEntity<DidDocument> responseEntity;
        DidDocument didDocument;

        try {
            String urlEncodedHash = Base64URL.encode(Hex.decode(hash)).toString();
            responseEntity = issuanceDgciRestClient.getDgciByHash(urlEncodedHash);
        } catch (IllegalArgumentException e) {
            log.error("Encoding of dgci hash for public key request failed.");
            throw new TokenValidationException("Token verification failed: Wrong format of DGCI hash for public key",
                HttpStatus.BAD_REQUEST.value());
        } catch (FeignException e) {
            if (e.status() == HttpStatus.NOT_FOUND.value()) {
                log.error("Download of dgci failed. {}",
                    e.status());
                throw new TokenValidationException("Token verification failed: Public key not found.",
                    HttpStatus.BAD_REQUEST.value());
            }

            log.error("Download of dgci failed. {}",
                e.status());
            throw new TokenValidationException("Token verification failed due to Server Error",
                HttpStatus.INTERNAL_SERVER_ERROR.value());
        }

        if (responseEntity.getStatusCode() != HttpStatus.OK) {
            throw new TokenValidationException("Token verification failed: No Public key found.",
                HttpStatus.BAD_REQUEST.value());
        }

        didDocument = responseEntity.getBody();

        if (didDocument.getAuthentication().isEmpty()) {
            throw new TokenValidationException("Token verification failed: No Public key found.",
                HttpStatus.BAD_REQUEST.value());
        }

        DidAuthentication didAuth = didDocument.getAuthentication().get(0);

        try {
            return ECKey.parse(didAuth.getPublicKeyJsw().toString()).toPublicKey();
        } catch (ParseException | JOSEException e) {
            log.error("Parsing of publicKey failed. {}", e);
            throw new TokenValidationException("Token verification failed: Public key could not be parsed",
                HttpStatus.BAD_REQUEST.value());
        }
    }
}
