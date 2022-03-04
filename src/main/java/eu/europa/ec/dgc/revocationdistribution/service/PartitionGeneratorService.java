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


import eu.europa.ec.dgc.revocationdistribution.dto.ChunkMetaViewDto;
import eu.europa.ec.dgc.revocationdistribution.dto.PartitionChunksJsonItemDto;
import eu.europa.ec.dgc.revocationdistribution.dto.SliceDataDto;
import eu.europa.ec.dgc.revocationdistribution.entity.PartitionEntity;
import eu.europa.ec.dgc.revocationdistribution.entity.SliceEntity;
import eu.europa.ec.dgc.revocationdistribution.model.SliceType;
import eu.europa.ec.dgc.revocationdistribution.repository.PartitionRepository;
import eu.europa.ec.dgc.revocationdistribution.repository.SliceRepository;
import eu.europa.ec.dgc.revocationdistribution.utils.HelperFunctions;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@RequiredArgsConstructor
@Service
public class PartitionGeneratorService {

    private final PartitionRepository partitionRepository;

    private final SliceRepository sliceRepository;

    private final HelperFunctions helperFunctions;

    private final Optional<SliceCalculationBloomFilterImpl> sliceCalculationBloomFilter;

    private final Optional<SliceCalculationVarHashListImpl> sliceCalculationHashList;

    /**
     * This function generates the partition and corresponding slices for the given data. The slice data is
     * stored as bloomfilter, varhashlist or as both. Corresponding to the application settings.
     * @param etag The etag value of the generated data set.
     * @param entities The entries, holding the hashes and metadata
     * @param kid The kid of the given data
     * @param id The partition id of the given data
     */

    public void generatePartition(String etag, List<ChunkMetaViewDto> entities,
                                   String kid, String id) {

        if (sliceCalculationBloomFilter.isPresent()) {
            generatePartition(etag, entities, kid, id, sliceCalculationBloomFilter.get());
        }

        if (sliceCalculationHashList.isPresent()) {
            generatePartition(etag, entities, kid, id, sliceCalculationHashList.get());
        }

    }


    private void generatePartition(String etag, List<ChunkMetaViewDto> entities,
                                   String kid, String id, SliceCalculation sliceCalculation) {

        String x = null;
        String y = null;
        ZonedDateTime lastUpdated = ZonedDateTime.parse("2021-06-01T00:00:00Z");
        ZonedDateTime expired = ZonedDateTime.parse("2021-06-01T00:00:00Z");

        if (entities.isEmpty()) {
            log.info("No Entries found in View for kid: {} id: {}", kid, id);
            return;
        }

        Map<String, Map<String, PartitionChunksJsonItemDto>> chunksJson = new HashMap<>();

        for (ChunkMetaViewDto mve : entities) {

            if (!Objects.equals(mve.getKid(), kid) || !Objects.equals(mve.getId(), id)) {
                log.error("Kid and/or id does not match: kid: {} , {} id {}, {}", kid, mve.getKid(), id, mve.getId());
            } else {

                SliceDataDto sliceDataDto = sliceCalculation.calculateSlice(mve.getHashes());
                if (sliceDataDto != null) {
                    Map<String, PartitionChunksJsonItemDto> chunkItemsMap;

                    if (chunksJson.containsKey(mve.getChunk())) {
                        chunkItemsMap = chunksJson.get(mve.getChunk());
                    } else {
                        chunkItemsMap = new HashMap<>();
                    }

                    chunkItemsMap.put(helperFunctions.getDateTimeString(mve.getExpired()), sliceDataDto.getMetaData());
                    chunksJson.put(mve.getChunk(), chunkItemsMap);

                    saveSlice(etag, mve.getKid(), id, mve.getChunk(), sliceDataDto.getMetaData().getHash(),
                        mve.getLastUpdated(), mve.getExpired(), sliceCalculation.getSliceType(),
                        sliceDataDto.getBinaryData());

                    x = mve.getX();
                    y = mve.getY();

                    lastUpdated = lastUpdated.isAfter(mve.getLastUpdated()) ? lastUpdated : mve.getLastUpdated();
                    expired = expired.isAfter(mve.getExpired()) ? expired : mve.getExpired();
                }
            }
        }
        if (!chunksJson.isEmpty()) {
            savePartition(etag, kid, id, x, y, null,
                lastUpdated, expired, sliceCalculation.getSliceType(), chunksJson);
        }

    }

    private void saveSlice(String etag, String kid, String id, String chunk, String hash,
                           ZonedDateTime lastUpdated, ZonedDateTime expired, SliceType dataType, byte[] binaryData) {

        SliceEntity sliceEntity = new SliceEntity();

        sliceEntity.setEtag(etag);
        sliceEntity.setKid(kid);
        sliceEntity.setId(id);
        sliceEntity.setChunk(chunk);
        sliceEntity.setHash(hash);
        sliceEntity.setLastUpdated(lastUpdated);
        sliceEntity.setExpired(expired);
        sliceEntity.setDataType(dataType);
        sliceEntity.setBinaryData(binaryData);
        sliceEntity.setToBeDeleted(false);

        sliceRepository.save(sliceEntity);

    }


    private void savePartition(String etag, String kid, String id, String x, String y, String z,
                               ZonedDateTime lastUpdated, ZonedDateTime expired, SliceType dataType,
                               Map<String, Map<String, PartitionChunksJsonItemDto>> chunksJson) {

        PartitionEntity partitionEntity = new PartitionEntity();

        partitionEntity.setEtag(etag);
        partitionEntity.setKid(kid);
        partitionEntity.setId(id);
        partitionEntity.setX(x);
        partitionEntity.setY(y);
        partitionEntity.setZ(z);
        partitionEntity.setLastUpdated(lastUpdated);
        partitionEntity.setExpired(expired);
        partitionEntity.setDataType(dataType);
        partitionEntity.setChunks(chunksJson);
        partitionEntity.setToBeDeleted(false);

        partitionRepository.save(partitionEntity);
    }


}
