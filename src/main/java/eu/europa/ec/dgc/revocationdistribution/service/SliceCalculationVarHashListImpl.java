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

import eu.europa.ec.dgc.partialvariablehashfilter.PartialVariableHashFilter;
import eu.europa.ec.dgc.partialvariablehashfilter.PartitionOffset;
import eu.europa.ec.dgc.revocationdistribution.config.DgcConfigProperties;
import eu.europa.ec.dgc.revocationdistribution.dto.SliceDataDto;
import eu.europa.ec.dgc.revocationdistribution.model.SliceType;
import eu.europa.ec.dgc.revocationdistribution.utils.HelperFunctions;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.util.encoders.DecoderException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;


@Slf4j
@RequiredArgsConstructor
@Service
@ConditionalOnProperty("dgc.varHashList.enabled")
public class SliceCalculationVarHashListImpl implements SliceCalculation {

    private final DgcConfigProperties properties;
    private final HelperFunctions helperFunctions;

    @Override
    public SliceType getSliceType() {
        return SliceType.VARHASHLIST;
    }

    @Override
    public SliceDataDto calculateSlice(String[] hashes, String storageMode) {
        if (hashes.length <= 0) {
            return null;
        }

        byte minByteCount = properties.getVarHashList().getMinByteCount();

        SliceDataDto sliceDataDto = new SliceDataDto();

        sliceDataDto.getMetaData().setType(SliceType.VARHASHLIST.name());
        sliceDataDto.getMetaData().setVersion(properties.getVarHashList().getVersion());

        PartialVariableHashFilter filter =
            new PartialVariableHashFilter(minByteCount, getPartitionOffset(storageMode), hashes.length,
                properties.getVarHashList().getProbRate());


        for (String hash : hashes) {
            try {
                byte[] hashBytes = helperFunctions.getBytesFromHexString(hash);
                filter.add(hashBytes);
            } catch (DecoderException e) {
                log.error("Could not add hash to hash list: {} , {}", hash, e.getMessage());
            }
        }

        try {
            sliceDataDto.setBinaryData(filter.writeTo());
            sliceDataDto.getMetaData().setHash(helperFunctions.calculateHash(sliceDataDto.getBinaryData()));
        } catch (IOException e) {
            log.error("Could not set binary data.");
            return null;
        } catch (NoSuchAlgorithmException e) {
            log.error("Could not calculate hash for binary data.");
            return null;
        }

        return sliceDataDto;
    }

    private PartitionOffset getPartitionOffset(String storageMode) {
        switch (storageMode) {
            case "POINT": {
                return PartitionOffset.POINT;
            }
            case "VECTOR": {
                return PartitionOffset.VECTOR;
            }
            case "COORDINATE":
            default: {
                return PartitionOffset.COORDINATE;
            }
        }
    }

}
