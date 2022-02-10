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

package eu.europa.ec.dgc.revocationdistribution.utils;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.util.encoders.Hex;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class HelperFunctions {

    private final DateTimeFormatter dateTimeFormatter =
        DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX");


    public String getDateTimeString(ZonedDateTime dateTime){
        return dateTimeFormatter.withZone(ZoneId.of("UTC")).format(dateTime);
    }

    /**
     * Calculates SHA-256 hash of a given Byte-Array.
     *
     * @param data data to hash.
     * @return HEX-String with the hash of the data.
     */
    public String calculateHash(byte[] data) throws NoSuchAlgorithmException {
        byte[] certHashBytes = MessageDigest.getInstance("SHA-256").digest(data);
        return Hex.toHexString(certHashBytes);
    }

    /**
     * Gets the byte array from a hex representation string.
     * @param hex
     * @return the byte data.
     */
    public byte[] getBytesFromHexString(String hex) {
       return Hex.decode(hex);
    }

}
