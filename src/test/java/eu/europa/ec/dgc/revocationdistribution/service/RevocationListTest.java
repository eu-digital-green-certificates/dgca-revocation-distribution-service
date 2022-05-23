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

package eu.europa.ec.dgc.revocationdistribution.service;


import static io.zonky.test.db.AutoConfigureEmbeddedDatabase.DatabaseProvider.ZONKY;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import eu.europa.ec.dgc.gateway.connector.DgcGatewayCountryListDownloadConnector;
import eu.europa.ec.dgc.gateway.connector.DgcGatewayValidationRuleDownloadConnector;
import eu.europa.ec.dgc.gateway.connector.DgcGatewayValueSetDownloadConnector;
import eu.europa.ec.dgc.gateway.connector.client.DgcGatewayConnectorRestClientConfig;
import eu.europa.ec.dgc.gateway.connector.dto.RevocationBatchDto;
import eu.europa.ec.dgc.gateway.connector.dto.RevocationHashTypeDto;
import eu.europa.ec.dgc.revocationdistribution.client.IssuanceDgciRestClient;
import eu.europa.ec.dgc.revocationdistribution.dto.ChunkMetaViewDto;
import eu.europa.ec.dgc.revocationdistribution.dto.RevocationListJsonResponseDto;
import eu.europa.ec.dgc.revocationdistribution.entity.HashesEntity;
import eu.europa.ec.dgc.revocationdistribution.entity.RevocationListJsonEntity;
import eu.europa.ec.dgc.revocationdistribution.exception.DataNotFoundException;
import eu.europa.ec.dgc.revocationdistribution.model.SliceType;
import eu.europa.ec.dgc.revocationdistribution.repository.BatchListRepository;
import eu.europa.ec.dgc.revocationdistribution.repository.HashesRepository;
import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.util.encoders.Hex;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.annotation.DirtiesContext;

@Slf4j
@SpringBootTest(
  properties = {
    "server.port=8080",
    "springdoc.api-docs.enabled=true",
    "springdoc.api-docs.path=/openapi",
    "dgc.gateway.connector.enabled=false"
  },
  webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@AutoConfigureEmbeddedDatabase(provider = ZONKY)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class RevocationListTest {

    @MockBean
    IssuanceDgciRestClient issuanceDgciRestClient;

    @Autowired
    RevocationListService classUnderTest;

    @Autowired
    PartitionGeneratorService partitionGeneratorService;

    @Autowired
    HashesRepository hashesRepository;

    @Autowired
    BatchListRepository batchListRepository;

    final String TESTHASH = "872e4e50ce9990d8b041330c47c9ddd11bec6b503ae9386a99da8584e9bb12c4";
    final String TESTETAG = "TESTETAG";
    final String TESTKID = "TESTKID";
    final String TESTMODE = "TESTMODE";
    final String TESTPARTITIONID = "TESTPARTITIONID";
    final String TESTCHUNK = "TESTCHUNK";
    final String TESTHASHTYPE = "TESTHASHTYPE";
    final String TESTSLICEID = "bf71fcf87814caa59c2bbf88df0b42b4f043aaa22fe4d7a05b3e38a292625638";
    private String b64Hash;

    @Test
    void RevocationListBatchRunThrough() {
        String batchId = "TESTBATCHID";
        List<String> hashList = new ArrayList<>();
        hashList.add(decodeBase64Hash());
        RevocationBatchDto dto = createTestRevocationBatchDto();
        var entity = batchListRepository.findById(batchId);
        assertTrue(entity.isEmpty());
        var entity2 = hashesRepository.getHashesPresentInListAndDb(hashList);
        assertTrue(entity2.isEmpty());

        classUnderTest.updateRevocationListBatch(batchId, dto);
        entity = batchListRepository.findById(batchId);
        entity2 = hashesRepository.getHashesPresentInListAndDb(hashList);
        assertFalse(entity.isEmpty());
        assertFalse(entity2.isEmpty());

        var hashes = hashesRepository.findAll();
        assertFalse(hashes.isEmpty());
        for (HashesEntity hashEntity : hashes) {
            assertTrue(hashEntity.isUpdated());
        }
        classUnderTest.setAllHashesUpdatedStatesToFalse();
        hashes = hashesRepository.findAll();
        assertFalse(hashes.isEmpty());
        for (HashesEntity hashEntity : hashes) {
            assertFalse(hashEntity.isUpdated());
        }
        var expired = classUnderTest.getExpiredBatchIds();
        assertFalse(expired.isEmpty());

        List<String> batchList = new ArrayList<>();
        batchList.add(batchId);
        classUnderTest.deleteBatchListItemsByIds(batchList);
        entity = batchListRepository.findById(batchId);
        assertTrue(entity.isEmpty());
        entity2 = hashesRepository.getHashesPresentInListAndDb(hashList);
        assertFalse(entity2.isEmpty());
        classUnderTest.deleteAllOrphanedHashes();
        entity2 = hashesRepository.getHashesPresentInListAndDb(hashList);
        assertTrue(entity2.isEmpty());
    }
    //TODO test error cases


    @Test
    void RevocationListJsonRunThrough() {
        var entity = classUnderTest.getRevocationListJsonData(TESTETAG);
        assertTrue(entity.isEmpty());
        RevocationListJsonEntity jsonEntity = createRevocationListJsonEntity();
        classUnderTest.saveRevocationListJson(jsonEntity);
        entity = classUnderTest.getRevocationListJsonData(TESTETAG);
        assertFalse(entity.isEmpty());
        classUnderTest.deleteAllOutdatedJsonLists(TESTETAG);
        entity = classUnderTest.getRevocationListJsonData(TESTETAG);
        assertFalse(entity.isEmpty());
        classUnderTest.deleteAllOutdatedJsonLists("NEWTESTETAG");
        entity = classUnderTest.getRevocationListJsonData(TESTETAG);
        assertTrue(entity.isEmpty());
    }

    @Test
    void PartitionRunThrough() {
        var chunkList = createChunkList();
        assertThrows(DataNotFoundException.class, () -> classUnderTest.getPartitionsByKidAndDate(TESTETAG, TESTKID, SliceType.VARHASHLIST,
          ZonedDateTime.now().minusMinutes(1)));
        assertThrows(DataNotFoundException.class, () -> classUnderTest.getPartitionsByKid(TESTETAG, TESTKID, SliceType.VARHASHLIST));
        assertThrows(DataNotFoundException.class, () -> classUnderTest.getPartitionsByKidAndId(TESTETAG, TESTKID, TESTPARTITIONID, SliceType.VARHASHLIST));
        assertThrows(DataNotFoundException.class, () -> classUnderTest.getPartitionsByKidAndIdAndDate(TESTETAG, TESTKID, TESTPARTITIONID, SliceType.VARHASHLIST,
          ZonedDateTime.now().minusMinutes(1)));
        assertThrows(DataNotFoundException.class, () -> classUnderTest.getPartitionsByKidAndId(TESTETAG, TESTKID, "null", SliceType.VARHASHLIST));
        assertThrows(DataNotFoundException.class, () -> classUnderTest.getPartitionsByKidAndIdAndDate(TESTETAG, TESTKID, "null", SliceType.VARHASHLIST,
          ZonedDateTime.now().minusMinutes(1)));
        partitionGeneratorService.generatePartition(TESTETAG, chunkList, TESTKID, TESTPARTITIONID, TESTMODE);
        var responseList = classUnderTest.getPartitionsByKid(TESTETAG, TESTKID, SliceType.VARHASHLIST);
        assertFalse(responseList.isEmpty());
        responseList = classUnderTest.getPartitionsByKidAndDate(TESTETAG, TESTKID, SliceType.VARHASHLIST,
          ZonedDateTime.now().minusMinutes(1));
        assertFalse(responseList.isEmpty());
        var response =
          classUnderTest.getPartitionsByKidAndId(TESTETAG, TESTKID, TESTPARTITIONID, SliceType.VARHASHLIST);
        Assertions.assertEquals(response.getId(), TESTPARTITIONID);
        response =
          classUnderTest.getPartitionsByKidAndIdAndDate(TESTETAG, TESTKID, TESTPARTITIONID, SliceType.VARHASHLIST,
            ZonedDateTime.now().minusMinutes(1));
        Assertions.assertEquals(response.getId(), TESTPARTITIONID);
    }

    @Test
    void SlicesRunThrough() {
        var chunkList = createChunkList();
        List<String> filter = new ArrayList<>();
        filter.add(TESTCHUNK);
        List<String> filter2 = new ArrayList<>();
        //TODO find out how this hashvalue endsup inside the slice
        filter2.add(TESTSLICEID);
        assertThrows(DataNotFoundException.class, () -> classUnderTest.getAllChunkDataFromPartition(TESTETAG, TESTKID, TESTPARTITIONID, SliceType.VARHASHLIST));
        assertThrows(DataNotFoundException.class, () -> classUnderTest.getAllChunkDataFromPartition(TESTETAG, TESTKID, "null", SliceType.VARHASHLIST));
        assertThrows(DataNotFoundException.class, () -> classUnderTest.getAllChunkDataFromPartitionSinceDate(TESTETAG, TESTKID, TESTPARTITIONID,
          SliceType.VARHASHLIST, ZonedDateTime.now().minusMinutes(1)));
        assertThrows(DataNotFoundException.class, () -> classUnderTest.getAllChunkDataFromPartitionSinceDate(TESTETAG, TESTKID, "null", SliceType.VARHASHLIST,
          ZonedDateTime.now().minusMinutes(1)));
        assertThrows(DataNotFoundException.class, () -> classUnderTest.getAllChunkDataFromPartitionWithFilter(TESTETAG, TESTKID, TESTPARTITIONID,
          SliceType.VARHASHLIST, filter));
        assertThrows(DataNotFoundException.class, () -> classUnderTest.getAllChunkDataFromPartitionWithFilter(TESTETAG, TESTKID, "null", SliceType.VARHASHLIST,
          filter));
        assertThrows(DataNotFoundException.class, () -> classUnderTest.getAllChunkDataFromPartitionWithFilterSinceDate(TESTETAG, TESTKID, TESTPARTITIONID,
          SliceType.VARHASHLIST, filter, ZonedDateTime.now().minusMinutes(1)));
        assertThrows(DataNotFoundException.class, () -> classUnderTest.getAllChunkDataFromPartitionWithFilterSinceDate(TESTETAG, TESTKID, "null",
          SliceType.VARHASHLIST, filter, ZonedDateTime.now().minusMinutes(1)));
        assertThrows(DataNotFoundException.class, () -> classUnderTest.getChunkData(TESTETAG, TESTKID, TESTPARTITIONID, TESTCHUNK, SliceType.VARHASHLIST));
        assertThrows(DataNotFoundException.class, () -> classUnderTest.getChunkData(TESTETAG, TESTKID, "null", TESTCHUNK, SliceType.VARHASHLIST));
        assertThrows(DataNotFoundException.class, () -> classUnderTest.getChunkDataSinceDate(TESTETAG, TESTKID, TESTPARTITIONID, TESTCHUNK, SliceType.VARHASHLIST,
          ZonedDateTime.now().minusMinutes(1)));
        assertThrows(DataNotFoundException.class, () -> classUnderTest.getChunkDataSinceDate(TESTETAG, TESTKID, "null", TESTCHUNK, SliceType.VARHASHLIST,
          ZonedDateTime.now().minusMinutes(1)));
        assertThrows(DataNotFoundException.class, () -> classUnderTest.getAllSliceDataForChunkWithFilter(TESTETAG, TESTKID, TESTPARTITIONID, TESTCHUNK,
          SliceType.VARHASHLIST, filter2));
        assertThrows(DataNotFoundException.class, () -> classUnderTest.getAllSliceDataForChunkWithFilter(TESTETAG, TESTKID, "null", TESTCHUNK,
          SliceType.VARHASHLIST, filter2));
        assertThrows(DataNotFoundException.class, () -> classUnderTest.getAllSliceDataForChunkWithFilterSinceDate(TESTETAG, TESTKID, TESTPARTITIONID, TESTCHUNK,
          SliceType.VARHASHLIST, filter2, ZonedDateTime.now().minusMinutes(1)));
        assertThrows(DataNotFoundException.class, () -> classUnderTest.getAllSliceDataForChunkWithFilterSinceDate(TESTETAG, TESTKID, "null", TESTCHUNK,
          SliceType.VARHASHLIST, filter2, ZonedDateTime.now().minusMinutes(1)));
        assertThrows(DataNotFoundException.class, () -> classUnderTest.getSliceData(TESTETAG, TESTKID, TESTPARTITIONID, TESTCHUNK, TESTSLICEID,
          SliceType.VARHASHLIST));
        assertThrows(DataNotFoundException.class, () -> classUnderTest.getSliceData(TESTETAG, TESTKID, "null", TESTCHUNK, TESTSLICEID,
          SliceType.VARHASHLIST));
        assertThrows(DataNotFoundException.class, () -> classUnderTest.getSliceDataSinceDate(TESTETAG, TESTKID, TESTPARTITIONID, TESTCHUNK, TESTSLICEID,
          SliceType.VARHASHLIST, ZonedDateTime.now().minusMinutes(1)));
        assertThrows(DataNotFoundException.class, () -> classUnderTest.getSliceDataSinceDate(TESTETAG, TESTKID, "null", TESTCHUNK, TESTSLICEID,
          SliceType.VARHASHLIST, ZonedDateTime.now().minusMinutes(1)));
        partitionGeneratorService.generatePartition(TESTETAG, chunkList, TESTKID, TESTPARTITIONID, TESTMODE);
        var result =
          classUnderTest.getAllChunkDataFromPartition(TESTETAG, TESTKID, TESTPARTITIONID, SliceType.VARHASHLIST);
        assertTrue(result.length > 0);
        result = classUnderTest.getAllChunkDataFromPartitionSinceDate(TESTETAG, TESTKID, TESTPARTITIONID,
          SliceType.VARHASHLIST, ZonedDateTime.now().minusMinutes(1));
        assertTrue(result.length > 0);
        result = classUnderTest.getAllChunkDataFromPartitionWithFilter(TESTETAG, TESTKID, TESTPARTITIONID,
          SliceType.VARHASHLIST, filter);
        assertTrue(result.length > 0);
        result = classUnderTest.getAllChunkDataFromPartitionWithFilterSinceDate(TESTETAG, TESTKID, TESTPARTITIONID,
          SliceType.VARHASHLIST, filter, ZonedDateTime.now().minusMinutes(1));
        assertTrue(result.length > 0);
        result = classUnderTest.getChunkData(TESTETAG, TESTKID, TESTPARTITIONID, TESTCHUNK, SliceType.VARHASHLIST);
        assertTrue(result.length > 0);
        result =
          classUnderTest.getChunkDataSinceDate(TESTETAG, TESTKID, TESTPARTITIONID, TESTCHUNK, SliceType.VARHASHLIST,
            ZonedDateTime.now().minusMinutes(1));
        assertTrue(result.length > 0);
        result = classUnderTest.getAllSliceDataForChunkWithFilter(TESTETAG, TESTKID, TESTPARTITIONID, TESTCHUNK,
          SliceType.VARHASHLIST, filter2);
        assertTrue(result.length > 0);
        result =
          classUnderTest.getAllSliceDataForChunkWithFilterSinceDate(TESTETAG, TESTKID, TESTPARTITIONID, TESTCHUNK,
            SliceType.VARHASHLIST, filter2, ZonedDateTime.now().minusMinutes(1));
        assertTrue(result.length > 0);
        result = classUnderTest.getSliceData(TESTETAG, TESTKID, TESTPARTITIONID, TESTCHUNK, TESTSLICEID,
          SliceType.VARHASHLIST);
        assertTrue(result.length > 0);
        result = classUnderTest.getSliceDataSinceDate(TESTETAG, TESTKID, TESTPARTITIONID, TESTCHUNK, TESTSLICEID,
          SliceType.VARHASHLIST, ZonedDateTime.now().minusMinutes(1));
        assertTrue(result.length > 0);


    }

    private RevocationBatchDto createTestRevocationBatchDto() {
        RevocationBatchDto dto = new RevocationBatchDto();
        dto.setHashType(RevocationHashTypeDto.SIGNATURE);
        dto.setCountry("DE");
        dto.setExpires(ZonedDateTime.now());
        RevocationBatchDto.BatchEntryDto entry = createBatchEntryDto();
        List<RevocationBatchDto.BatchEntryDto> entries = new ArrayList<>();
        entries.add(entry);
        dto.setEntries(entries);
        return dto;
    }

    private RevocationBatchDto.BatchEntryDto createBatchEntryDto() {
        RevocationBatchDto.BatchEntryDto entry = new RevocationBatchDto.BatchEntryDto();
        entry.setHash(TESTHASH);
        return entry;
    }

    private RevocationListJsonEntity createRevocationListJsonEntity() {
        RevocationListJsonEntity entity = new RevocationListJsonEntity();
        entity.setEtag(TESTETAG);
        entity.setCreatedAt(ZonedDateTime.now());
        RevocationListJsonResponseDto.RevocationListJsonResponseItemDto json =
          createRevocationListJsonResponseItemDto();
        List<RevocationListJsonResponseDto.RevocationListJsonResponseItemDto> jsonData = new ArrayList<>();
        jsonData.add(json);
        entity.setJsonData(jsonData);
        return entity;
    }

    private RevocationListJsonResponseDto.RevocationListJsonResponseItemDto createRevocationListJsonResponseItemDto() {
        RevocationListJsonResponseDto.RevocationListJsonResponseItemDto json =
          new RevocationListJsonResponseDto.RevocationListJsonResponseItemDto();
        List<String> hashTypes = new ArrayList<>();
        hashTypes.add(TESTHASHTYPE);
        json.setExpires(ZonedDateTime.now());
        json.setKid(TESTKID);
        json.setMode(TESTMODE);
        json.setHashTypes(hashTypes);
        json.setLastUpdated(ZonedDateTime.now());
        return json;
    }

    private List<ChunkMetaViewDto> createChunkList() {
        String[] hashList = {TESTHASH};
        ChunkMetaViewDto chunkMetaViewDto =
          new ChunkMetaViewDto("TESTROWID", TESTKID, hashList, TESTPARTITIONID, "TESTX", "TESTY", TESTCHUNK,
            ZonedDateTime.now(), ZonedDateTime.now());
        List<ChunkMetaViewDto> chunkList = new ArrayList<>();
        chunkList.add(chunkMetaViewDto);
        return chunkList;
    }

    private String decodeBase64Hash() {
        byte[] decodedBytes = Base64.getDecoder().decode(TESTHASH);
        return Hex.toHexString(decodedBytes);
    }
}
