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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import eu.europa.ec.dgc.revocationdistribution.client.IssuanceDgciRestClient;
import eu.europa.ec.dgc.revocationdistribution.dto.RevocationListJsonResponseDto;
import eu.europa.ec.dgc.revocationdistribution.entity.CoordinateViewEntity;
import eu.europa.ec.dgc.revocationdistribution.entity.KidViewEntity;
import eu.europa.ec.dgc.revocationdistribution.entity.PointViewEntity;
import eu.europa.ec.dgc.revocationdistribution.entity.RevocationListJsonEntity;
import eu.europa.ec.dgc.revocationdistribution.entity.VectorViewEntity;
import eu.europa.ec.dgc.revocationdistribution.repository.CoordinateViewRepository;
import eu.europa.ec.dgc.revocationdistribution.repository.KidViewRepository;
import eu.europa.ec.dgc.revocationdistribution.repository.PartitionRepository;
import eu.europa.ec.dgc.revocationdistribution.repository.PointViewRepository;
import eu.europa.ec.dgc.revocationdistribution.repository.RevocationListJsonRepository;
import eu.europa.ec.dgc.revocationdistribution.repository.VectorViewRepository;
import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import java.lang.reflect.Field;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

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
class GeneratorServiceTest {

    @MockBean
    IssuanceDgciRestClient issuanceDgciRestClient;

    @Autowired
    GeneratorService classUnderTest;

    @MockBean
    KidViewRepository kidViewRepository;

    @MockBean
    CoordinateViewRepository coordinateViewRepository;

    @MockBean
    PointViewRepository pointViewRepository;

    @MockBean
    VectorViewRepository vectorViewRepository;

    @MockBean
    RevocationListJsonRepository revocationListJsonRepository;
    @Autowired
    PartitionRepository partitionRepository;

    final String TESTKID = "TESTKID";
    final String TESTPARTITION = "TESTPARTITION";
    final String TESTHASH = "TESTHASH";

    @Test
    void generateNewDataSetCOORDINATERunThrough() throws NoSuchFieldException, IllegalAccessException {
        when(kidViewRepository.findAll()).thenReturn(getTestCOORDINATEKids());
        List<String> partitions = new ArrayList<>();
        partitions.add(TESTPARTITION);
        when(coordinateViewRepository.findDistinctIdsByKid(TESTKID)).thenReturn(partitions);
        when(coordinateViewRepository.findAllByKidAndId(TESTKID, TESTPARTITION)).thenReturn(
          createCoordinateViewEntityList());
        classUnderTest.generateNewDataSet();
        var result = partitionRepository.findAll();
        assertFalse(result.isEmpty());
    }

    @Test
    void generateNewDataSetPOINTRunThrough() throws NoSuchFieldException, IllegalAccessException {
        when(kidViewRepository.findAll()).thenReturn(getTestPOINTKids());
        when(pointViewRepository.findAllByKid(TESTKID)).thenReturn(
          createPointViewEntityList());
        classUnderTest.generateNewDataSet();
        var result = partitionRepository.findAll();
        assertFalse(result.isEmpty());
    }

    @Test
    void generateNewDataSetVECTORRunThrough() throws NoSuchFieldException, IllegalAccessException {
        when(kidViewRepository.findAll()).thenReturn(getTestVECTORKids());
        List<String> partitions = new ArrayList<>();
        partitions.add(TESTPARTITION);
        when(vectorViewRepository.findDistinctIdsByKid(TESTKID)).thenReturn(partitions);
        when(vectorViewRepository.findAllByKidAndId(TESTKID, TESTPARTITION)).thenReturn(
          createVectorViewEntityList());
        classUnderTest.generateNewDataSet();
        var result = partitionRepository.findAll();
        assertFalse(result.isEmpty());
    }

    @Test
    void generateNewDataSetUnrecognisedStorageMode() throws NoSuchFieldException, IllegalAccessException {
        when(kidViewRepository.findAll()).thenReturn(getTestBADKids());
        classUnderTest.generateNewDataSet();
        var result = partitionRepository.findAll();
        assertTrue(result.isEmpty());
    }

    @Test
    void generateNewDataSetEmptyTypes() throws NoSuchFieldException, IllegalAccessException {
        when(kidViewRepository.findAll()).thenReturn(getTestEMPTYKids());
        when(revocationListJsonRepository.findById(any())).thenReturn(Optional.of(createRevocationListJsonEntity()));
        classUnderTest.generateNewDataSet();
        var result = partitionRepository.findAll();
        assertTrue(result.isEmpty());
    }

    @Test
    void generateNewDataSetOutdated() throws NoSuchFieldException, IllegalAccessException {
        when(kidViewRepository.findAll()).thenReturn(getTestOutdatedKids());
        when(revocationListJsonRepository.findById(any())).thenReturn(Optional.of(createRevocationListJsonEntity()));
        classUnderTest.generateNewDataSet();
        var result = partitionRepository.findAll();
        assertTrue(result.isEmpty());
    }

    private List<KidViewEntity> getTestCOORDINATEKids() throws NoSuchFieldException, IllegalAccessException {
        KidViewEntity kidViewEntity = new KidViewEntity();

        Field typesField = kidViewEntity.getClass().getDeclaredField("types");
        typesField.setAccessible(true);
        List<String> types = new ArrayList<>();
        types.add("TESTTYPE");
        typesField.set(kidViewEntity, types);

        Field storageModeField = kidViewEntity.getClass().getDeclaredField("storageMode");
        storageModeField.setAccessible(true);
        storageModeField.set(kidViewEntity, "COORDINATE");

        Field kidField = kidViewEntity.getClass().getDeclaredField("kid");
        kidField.setAccessible(true);
        kidField.set(kidViewEntity, TESTKID);

        List<KidViewEntity> kidViewEntityList = new ArrayList<>();
        kidViewEntityList.add(kidViewEntity);
        return kidViewEntityList;
    }

    private List<KidViewEntity> getTestPOINTKids() throws NoSuchFieldException, IllegalAccessException {
        KidViewEntity kidViewEntity = new KidViewEntity();

        Field typesField = kidViewEntity.getClass().getDeclaredField("types");
        typesField.setAccessible(true);
        List<String> types = new ArrayList<>();
        types.add("TESTTYPE");
        typesField.set(kidViewEntity, types);

        Field storageModeField = kidViewEntity.getClass().getDeclaredField("storageMode");
        storageModeField.setAccessible(true);
        storageModeField.set(kidViewEntity, "POINT");

        Field kidField = kidViewEntity.getClass().getDeclaredField("kid");
        kidField.setAccessible(true);
        kidField.set(kidViewEntity, TESTKID);

        List<KidViewEntity> kidViewEntityList = new ArrayList<>();
        kidViewEntityList.add(kidViewEntity);
        return kidViewEntityList;
    }

    private List<KidViewEntity> getTestVECTORKids() throws NoSuchFieldException, IllegalAccessException {
        KidViewEntity kidViewEntity = new KidViewEntity();

        Field typesField = kidViewEntity.getClass().getDeclaredField("types");
        typesField.setAccessible(true);
        List<String> types = new ArrayList<>();
        types.add("TESTTYPE");
        typesField.set(kidViewEntity, types);

        Field storageModeField = kidViewEntity.getClass().getDeclaredField("storageMode");
        storageModeField.setAccessible(true);
        storageModeField.set(kidViewEntity, "VECTOR");

        Field kidField = kidViewEntity.getClass().getDeclaredField("kid");
        kidField.setAccessible(true);
        kidField.set(kidViewEntity, TESTKID);

        List<KidViewEntity> kidViewEntityList = new ArrayList<>();
        kidViewEntityList.add(kidViewEntity);
        return kidViewEntityList;
    }

    private List<KidViewEntity> getTestBADKids() throws NoSuchFieldException, IllegalAccessException {
        KidViewEntity kidViewEntity = new KidViewEntity();

        Field typesField = kidViewEntity.getClass().getDeclaredField("types");
        typesField.setAccessible(true);
        List<String> types = new ArrayList<>();
        types.add("TESTTYPE");
        typesField.set(kidViewEntity, types);

        Field storageModeField = kidViewEntity.getClass().getDeclaredField("storageMode");
        storageModeField.setAccessible(true);
        storageModeField.set(kidViewEntity, "GSJGHFJHKSFJHKASJGHV");

        Field kidField = kidViewEntity.getClass().getDeclaredField("kid");
        kidField.setAccessible(true);
        kidField.set(kidViewEntity, TESTKID);

        List<KidViewEntity> kidViewEntityList = new ArrayList<>();
        kidViewEntityList.add(kidViewEntity);
        return kidViewEntityList;
    }

    private List<KidViewEntity> getTestEMPTYKids() throws NoSuchFieldException, IllegalAccessException {
        KidViewEntity kidViewEntity = new KidViewEntity();

        Field typesField = kidViewEntity.getClass().getDeclaredField("types");
        typesField.setAccessible(true);
        List<String> types = new ArrayList<>();
        typesField.set(kidViewEntity, types);

        Field storageModeField = kidViewEntity.getClass().getDeclaredField("storageMode");
        storageModeField.setAccessible(true);
        storageModeField.set(kidViewEntity, "GSJGHFJHKSFJHKASJGHV");

        Field kidField = kidViewEntity.getClass().getDeclaredField("kid");
        kidField.setAccessible(true);
        kidField.set(kidViewEntity, TESTKID);

        List<KidViewEntity> kidViewEntityList = new ArrayList<>();
        kidViewEntityList.add(kidViewEntity);
        return kidViewEntityList;
    }

    private List<KidViewEntity> getTestOutdatedKids() throws NoSuchFieldException, IllegalAccessException {
        KidViewEntity kidViewEntity = new KidViewEntity();

        Field typesField = kidViewEntity.getClass().getDeclaredField("types");
        typesField.setAccessible(true);
        List<String> types = new ArrayList<>();
        types.add("TESTTYPE");
        typesField.set(kidViewEntity, types);

        Field storageModeField = kidViewEntity.getClass().getDeclaredField("storageMode");
        storageModeField.setAccessible(true);
        storageModeField.set(kidViewEntity, "COORDINATE");

        Field kidField = kidViewEntity.getClass().getDeclaredField("kid");
        kidField.setAccessible(true);
        kidField.set(kidViewEntity, TESTKID);

        Field updatedField = kidViewEntity.getClass().getDeclaredField("updated");
        updatedField.setAccessible(true);
        updatedField.set(kidViewEntity, false);

        List<KidViewEntity> kidViewEntityList = new ArrayList<>();
        kidViewEntityList.add(kidViewEntity);
        return kidViewEntityList;
    }

    private List<CoordinateViewEntity> createCoordinateViewEntityList()
      throws NoSuchFieldException, IllegalAccessException {
        String[] hashList = {TESTHASH};
        CoordinateViewEntity coordinateViewEntity =
          new CoordinateViewEntity();
        Field rowIdField = coordinateViewEntity.getClass().getSuperclass().getDeclaredField("rowId");
        rowIdField.setAccessible(true);
        rowIdField.set(coordinateViewEntity, "TESTROWID");
        Field kidField = coordinateViewEntity.getClass().getSuperclass().getDeclaredField("kid");
        kidField.setAccessible(true);
        kidField.set(coordinateViewEntity, TESTKID);
        Field hashesField = coordinateViewEntity.getClass().getSuperclass().getDeclaredField("hashes");
        hashesField.setAccessible(true);
        hashesField.set(coordinateViewEntity, hashList);
        Field idField = coordinateViewEntity.getClass().getSuperclass().getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(coordinateViewEntity, TESTPARTITION);
        Field xField = coordinateViewEntity.getClass().getSuperclass().getDeclaredField("x");
        xField.setAccessible(true);
        xField.set(coordinateViewEntity, "TESTX");
        Field yField = coordinateViewEntity.getClass().getSuperclass().getDeclaredField("y");
        yField.setAccessible(true);
        yField.set(coordinateViewEntity, "TESTY");
        Field chunkField = coordinateViewEntity.getClass().getSuperclass().getDeclaredField("chunk");
        chunkField.setAccessible(true);
        chunkField.set(coordinateViewEntity, "TESTCHUNK");
        Field lastUpdatedField = coordinateViewEntity.getClass().getSuperclass().getDeclaredField("lastUpdated");
        lastUpdatedField.setAccessible(true);
        lastUpdatedField.set(coordinateViewEntity, ZonedDateTime.now());
        Field expiredField = coordinateViewEntity.getClass().getSuperclass().getDeclaredField("expired");
        expiredField.setAccessible(true);
        expiredField.set(coordinateViewEntity, ZonedDateTime.now());

        List<CoordinateViewEntity> coordinateViewEntityList = new ArrayList<>();
        coordinateViewEntityList.add(coordinateViewEntity);
        return coordinateViewEntityList;
    }

    private List<PointViewEntity> createPointViewEntityList()
      throws NoSuchFieldException, IllegalAccessException {
        String[] hashList = {TESTHASH};
        PointViewEntity pointViewEntity = new PointViewEntity();
        Field rowIdField = pointViewEntity.getClass().getSuperclass().getDeclaredField("rowId");
        rowIdField.setAccessible(true);
        rowIdField.set(pointViewEntity, "TESTROWID");
        Field kidField = pointViewEntity.getClass().getSuperclass().getDeclaredField("kid");
        kidField.setAccessible(true);
        kidField.set(pointViewEntity, TESTKID);
        Field hashesField = pointViewEntity.getClass().getSuperclass().getDeclaredField("hashes");
        hashesField.setAccessible(true);
        hashesField.set(pointViewEntity, hashList);
        Field xField = pointViewEntity.getClass().getSuperclass().getDeclaredField("x");
        xField.setAccessible(true);
        xField.set(pointViewEntity, "TESTX");
        Field yField = pointViewEntity.getClass().getSuperclass().getDeclaredField("y");
        yField.setAccessible(true);
        yField.set(pointViewEntity, "TESTY");
        Field chunkField = pointViewEntity.getClass().getSuperclass().getDeclaredField("chunk");
        chunkField.setAccessible(true);
        chunkField.set(pointViewEntity, "TESTCHUNK");
        Field lastUpdatedField = pointViewEntity.getClass().getSuperclass().getDeclaredField("lastUpdated");
        lastUpdatedField.setAccessible(true);
        lastUpdatedField.set(pointViewEntity, ZonedDateTime.now());
        Field expiredField = pointViewEntity.getClass().getSuperclass().getDeclaredField("expired");
        expiredField.setAccessible(true);
        expiredField.set(pointViewEntity, ZonedDateTime.now());

        List<PointViewEntity> pointViewEntityList = new ArrayList<>();
        pointViewEntityList.add(pointViewEntity);
        return pointViewEntityList;
    }

    private List<VectorViewEntity> createVectorViewEntityList()
      throws NoSuchFieldException, IllegalAccessException {
        String[] hashList = {TESTHASH};
        VectorViewEntity vectorViewEntity =
          new VectorViewEntity();
        Field rowIdField = vectorViewEntity.getClass().getSuperclass().getDeclaredField("rowId");
        rowIdField.setAccessible(true);
        rowIdField.set(vectorViewEntity, "TESTROWID");
        Field kidField = vectorViewEntity.getClass().getSuperclass().getDeclaredField("kid");
        kidField.setAccessible(true);
        kidField.set(vectorViewEntity, TESTKID);
        Field hashesField = vectorViewEntity.getClass().getSuperclass().getDeclaredField("hashes");
        hashesField.setAccessible(true);
        hashesField.set(vectorViewEntity, hashList);
        Field idField = vectorViewEntity.getClass().getSuperclass().getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(vectorViewEntity, TESTPARTITION);
        Field xField = vectorViewEntity.getClass().getSuperclass().getDeclaredField("x");
        xField.setAccessible(true);
        xField.set(vectorViewEntity, "TESTX");
        Field yField = vectorViewEntity.getClass().getSuperclass().getDeclaredField("y");
        yField.setAccessible(true);
        yField.set(vectorViewEntity, "TESTY");
        Field chunkField = vectorViewEntity.getClass().getSuperclass().getDeclaredField("chunk");
        chunkField.setAccessible(true);
        chunkField.set(vectorViewEntity, "TESTCHUNK");
        Field lastUpdatedField = vectorViewEntity.getClass().getSuperclass().getDeclaredField("lastUpdated");
        lastUpdatedField.setAccessible(true);
        lastUpdatedField.set(vectorViewEntity, ZonedDateTime.now());
        Field expiredField = vectorViewEntity.getClass().getSuperclass().getDeclaredField("expired");
        expiredField.setAccessible(true);
        expiredField.set(vectorViewEntity, ZonedDateTime.now());

        List<VectorViewEntity> vectorViewEntityList = new ArrayList<>();
        vectorViewEntityList.add(vectorViewEntity);
        return vectorViewEntityList;
    }

    private RevocationListJsonEntity createRevocationListJsonEntity() {
        RevocationListJsonEntity entity = new RevocationListJsonEntity();
        entity.setEtag("TESTETAG");
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
        hashTypes.add("TESTHASHTYPE");
        json.setExpires(ZonedDateTime.now());
        json.setKid(TESTKID);
        json.setMode("TESTMODE");
        json.setHashTypes(hashTypes);
        json.setLastUpdated(ZonedDateTime.now());
        return json;
    }

}
