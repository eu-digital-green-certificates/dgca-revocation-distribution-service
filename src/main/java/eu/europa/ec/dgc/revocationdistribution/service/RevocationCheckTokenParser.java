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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.europa.ec.dgc.revocationdistribution.dto.RevocationCheckTokenPayload;
import eu.europa.ec.dgc.revocationdistribution.exception.TokenValidationException;
import io.jsonwebtoken.Jwt;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.SignatureException;
import java.security.PublicKey;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class RevocationCheckTokenParser {
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * parse Token.
     *
     * @param jwtCompact jwtCompact
     * @param publicKey  publicKey
     * @return RevocationCheckTokenPayload
     */
    public RevocationCheckTokenPayload parseToken(String jwtCompact, PublicKey publicKey) {



        try {


            Jwt token = Jwts.parserBuilder().setSigningKey(publicKey).build().parseClaimsJws(jwtCompact);


            String payloadJson = objectMapper.writeValueAsString(token.getBody());
            return objectMapper.readValue(payloadJson, RevocationCheckTokenPayload.class);
        } catch (JsonProcessingException e) {
            throw new TokenValidationException("Failed to parse revocation check token",
                HttpStatus.BAD_REQUEST.value());
        } catch (SignatureException e) {
            log.warn("Signature check failed for revocation check token: {}", e.getMessage());
            throw new TokenValidationException("Signature check failed for revocation check token",
                HttpStatus.BAD_REQUEST.value());
        }
    }

    /**
     * extract payload.
     *
     * @param jwtCompact jwtCompact
     * @return JWT
     */
    public Jwt extractPayload(String jwtCompact) {
        String[] splitToken = jwtCompact.split("\\.");

        if (splitToken.length < 2) {
            throw new TokenValidationException("Failed to parse revocation check token. Wrong format.",
                HttpStatus.BAD_REQUEST.value());
        }

        String unsignedToken = splitToken[0] + "." + splitToken[1] + ".";
        return Jwts.parser().parse(unsignedToken);
    }
}
