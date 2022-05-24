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

package eu.europa.ec.dgc.revocationdistribution.controller;


import static eu.europa.ec.dgc.revocationdistribution.model.SliceType.VARHASHLIST;
import static io.zonky.test.db.AutoConfigureEmbeddedDatabase.DatabaseProvider.ZONKY;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import eu.europa.ec.dgc.revocationdistribution.client.IssuanceDgciRestClient;
import eu.europa.ec.dgc.revocationdistribution.dto.RevocationListJsonResponseDto;
import eu.europa.ec.dgc.revocationdistribution.entity.RevocationListJsonEntity;
import eu.europa.ec.dgc.revocationdistribution.service.InfoService;
import eu.europa.ec.dgc.revocationdistribution.service.RevocationListService;
import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

@Slf4j
@AutoConfigureEmbeddedDatabase(provider = ZONKY)
@WebMvcTest(RevocationListController.class)
class RevocationListControllerTest {

    @MockBean
    IssuanceDgciRestClient issuanceDgciRestClient;

    @MockBean
    InfoService infoService;

    @MockBean
    RevocationListService revocationListService;

    @Autowired
    MockMvc mvc;

    final String TEST_ETAG = "TESTETAG";
    final String TEST_NONE_MATCH = "NONEMATCH";
    final String TEST_HASHTYPE = "TESTHASHTYPE";
    final String TEST_KID = "TESTKID";
    final String TEST_MODE = "TESTMODE";
    final String TEST_PARTITION_ID = "TESTPARTITION";
    final String TEST_CID = "TESTCID";
    final String TEST_SID = "TESTSID";
    final String SLICE_DATA_TYPE_HEADER = "X-SLICE-FILTER-TYPE";

    @Test
    void getRevocationList_OK() throws Exception {
        when(infoService.getValueForKey(any())).thenReturn(TEST_ETAG);
        when(revocationListService.getRevocationListJsonData(TEST_ETAG)).thenReturn(
          createTestRevocationListJsonEntity());
        mvc.perform(MockMvcRequestBuilders.get("/lists").header(HttpHeaders.IF_NONE_MATCH, TEST_NONE_MATCH))
          .andExpect(status().isOk());
    }

    @Test
    void getRevocationList_NOTFOUND() throws Exception {
        when(infoService.getValueForKey(any())).thenReturn(TEST_ETAG);
        mvc.perform(MockMvcRequestBuilders.get("/lists").header(HttpHeaders.IF_NONE_MATCH, TEST_NONE_MATCH))
          .andExpect(status().isNotFound());
    }

    @Test
    void getRevocationList_NOTMODIFIED() throws Exception {
        when(infoService.getValueForKey(any())).thenReturn(TEST_ETAG);
        mvc.perform(MockMvcRequestBuilders.get("/lists").header(HttpHeaders.IF_NONE_MATCH, TEST_ETAG))
          .andExpect(status().isNotModified());
    }

    @Test
    void getPartitionListForKid_OK() throws Exception {
        when(infoService.getValueForKey(any())).thenReturn(TEST_ETAG);
        mvc.perform(
            MockMvcRequestBuilders.get("/lists/" + TEST_KID + "/partitions").header(HttpHeaders.IF_MATCH, TEST_ETAG))
          .andExpect(status().isOk());
    }

    @Test
    void getPartitionListForKid_PRECONDITIONFAILED() throws Exception {
        when(infoService.getValueForKey(any())).thenReturn(TEST_ETAG);
        mvc.perform(
            MockMvcRequestBuilders.get("/lists/" + TEST_KID + "/partitions").header(HttpHeaders.IF_MATCH, "I AM NOT THE CORRECT ETAG"))
          .andExpect(status().isPreconditionFailed());
    }

    @Test
    void getPartitionListForKid_sliceDataTypeHeader_OK() throws Exception {
        when(infoService.getValueForKey(any())).thenReturn(TEST_ETAG);
        mvc.perform(
            MockMvcRequestBuilders.get("/lists/" + TEST_KID + "/partitions").header(HttpHeaders.IF_MATCH, TEST_ETAG).header(SLICE_DATA_TYPE_HEADER,VARHASHLIST))
          .andExpect(status().isOk());
    }

    @Test
    void getPartitionListForKid_sliceDataTypeHeader_BADREQUEST() throws Exception {
        when(infoService.getValueForKey(any())).thenReturn(TEST_ETAG);
        mvc.perform(
            MockMvcRequestBuilders.get("/lists/" + TEST_KID + "/partitions").header(HttpHeaders.IF_MATCH, TEST_ETAG).header(SLICE_DATA_TYPE_HEADER,"NOT A VALID TYPE"))
          .andExpect(status().isBadRequest());
    }

    @Test
    void getPartitionListForKid_ModifiedSince_OK() throws Exception {
        when(infoService.getValueForKey(any())).thenReturn(TEST_ETAG);
        mvc.perform(
          MockMvcRequestBuilders.get("/lists/" + TEST_KID + "/partitions").header(HttpHeaders.IF_MATCH, TEST_ETAG)
            .header(HttpHeaders.IF_MODIFIED_SINCE, ZonedDateTime.now())).andExpect(status().isOk());
    }

    @Test
    void getPartitionListForKid_ModifiedSince_BADREQUEST() throws Exception {
        when(infoService.getValueForKey(any())).thenReturn(TEST_ETAG);
        mvc.perform(
          MockMvcRequestBuilders.get("/lists/" + TEST_KID + "/partitions").header(HttpHeaders.IF_MATCH, TEST_ETAG)
            .header(HttpHeaders.IF_MODIFIED_SINCE, "THIS IS NO VALID TIME")).andExpect(status().isBadRequest());
    }

    @Test
    void getPartitionForKid_OK() throws Exception {
        when(infoService.getValueForKey(any())).thenReturn(TEST_ETAG);
        mvc.perform(
          MockMvcRequestBuilders.get("/lists/" + TEST_KID + "/partitions/" + TEST_PARTITION_ID)
            .header(HttpHeaders.IF_MATCH, TEST_ETAG)).andExpect(status().isOk());
    }

    @Test
    void getPartitionForKid_ModifiedSince_OK() throws Exception {
        when(infoService.getValueForKey(any())).thenReturn(TEST_ETAG);
        mvc.perform(
            MockMvcRequestBuilders.get("/lists/" + TEST_KID + "/partitions/" + TEST_PARTITION_ID)
              .header(HttpHeaders.IF_MATCH, TEST_ETAG).header(HttpHeaders.IF_MODIFIED_SINCE, ZonedDateTime.now()))
          .andExpect(status().isOk());
    }

    @Test
    void getPartitionForKid_ModifiedSince_BADREQUEST() throws Exception {
        when(infoService.getValueForKey(any())).thenReturn(TEST_ETAG);
        mvc.perform(
            MockMvcRequestBuilders.get("/lists/" + TEST_KID + "/partitions/" + TEST_PARTITION_ID)
              .header(HttpHeaders.IF_MATCH, TEST_ETAG).header(HttpHeaders.IF_MODIFIED_SINCE, "THIS IS NO VALID TIME"))
          .andExpect(status().isBadRequest());
    }

    @Test
    void getPartitionChunksData_OK() throws Exception {
        when(infoService.getValueForKey(any())).thenReturn(TEST_ETAG);
        mvc.perform(
          MockMvcRequestBuilders.post("/lists/" + TEST_KID + "/partitions/" + TEST_PARTITION_ID + "/slices")
            .header(HttpHeaders.IF_MATCH, TEST_ETAG)).andExpect(status().isOk());
    }

    @Test
    void getPartitionChunksData_Body_OK() throws Exception {
        when(infoService.getValueForKey(any())).thenReturn(TEST_ETAG);
        mvc.perform(
          MockMvcRequestBuilders.post("/lists/" + TEST_KID + "/partitions/" + TEST_PARTITION_ID + "/slices")
            .header(HttpHeaders.IF_MATCH, TEST_ETAG).contentType(MediaType.APPLICATION_JSON_VALUE)
            .content("[\"filter1\", \"filter2\"]")).andExpect(status().isOk());
    }

    @Test
    void getPartitionChunksData_ModifiedSince_OK() throws Exception {
        when(infoService.getValueForKey(any())).thenReturn(TEST_ETAG);
        mvc.perform(
            MockMvcRequestBuilders.post("/lists/" + TEST_KID + "/partitions/" + TEST_PARTITION_ID + "/slices")
              .header(HttpHeaders.IF_MATCH, TEST_ETAG).header(HttpHeaders.IF_MODIFIED_SINCE, ZonedDateTime.now()))
          .andExpect(status().isOk());
    }

    @Test
    void getPartitionChunksData_ModifiedSince_Body_OK() throws Exception {
        when(infoService.getValueForKey(any())).thenReturn(TEST_ETAG);
        mvc.perform(
            MockMvcRequestBuilders.post("/lists/" + TEST_KID + "/partitions/" + TEST_PARTITION_ID + "/slices")
              .header(HttpHeaders.IF_MATCH, TEST_ETAG).header(HttpHeaders.IF_MODIFIED_SINCE, ZonedDateTime.now())
              .contentType(MediaType.APPLICATION_JSON_VALUE).content("[\"filter1\", \"filter2\"]"))
          .andExpect(status().isOk());
    }

    @Test
    void getPartitionChunksData_ModifiedSince_BADREQUEST() throws Exception {
        when(infoService.getValueForKey(any())).thenReturn(TEST_ETAG);
        mvc.perform(
            MockMvcRequestBuilders.post("/lists/" + TEST_KID + "/partitions/" + TEST_PARTITION_ID + "/slices")
              .header(HttpHeaders.IF_MATCH, TEST_ETAG).header(HttpHeaders.IF_MODIFIED_SINCE, "THIS IS NO VALID TIME"))
          .andExpect(status().isBadRequest());
    }

    @Test
    void getChunk_OK() throws Exception {
        when(infoService.getValueForKey(any())).thenReturn(TEST_ETAG);
        mvc.perform(
          MockMvcRequestBuilders.get(
              "/lists/" + TEST_KID + "/partitions/" + TEST_PARTITION_ID + "/chunks/" + TEST_CID + "/slices")
            .header(HttpHeaders.IF_MATCH, TEST_ETAG)).andExpect(status().isOk());
    }

    @Test
    void getChunk_ModifiedSince_OK() throws Exception {
        when(infoService.getValueForKey(any())).thenReturn(TEST_ETAG);
        mvc.perform(
            MockMvcRequestBuilders.get(
                "/lists/" + TEST_KID + "/partitions/" + TEST_PARTITION_ID + "/chunks/" + TEST_CID + "/slices")
              .header(HttpHeaders.IF_MATCH, TEST_ETAG).header(HttpHeaders.IF_MODIFIED_SINCE, ZonedDateTime.now()))
          .andExpect(status().isOk());
    }

    @Test
    void getChunk_ModifiedSince_BADREQUEST() throws Exception {
        when(infoService.getValueForKey(any())).thenReturn(TEST_ETAG);
        mvc.perform(
            MockMvcRequestBuilders.get(
                "/lists/" + TEST_KID + "/partitions/" + TEST_PARTITION_ID + "/chunks/" + TEST_CID + "/slices")
              .header(HttpHeaders.IF_MATCH, TEST_ETAG).header(HttpHeaders.IF_MODIFIED_SINCE, "THIS IS NO VALID TIME"))
          .andExpect(status().isBadRequest());
    }

    @Test
    void getPartitionChunks_OK() throws Exception {
        when(infoService.getValueForKey(any())).thenReturn(TEST_ETAG);
        mvc.perform(
          MockMvcRequestBuilders.post(
              "/lists/" + TEST_KID + "/partitions/" + TEST_PARTITION_ID + "/chunks/" + TEST_CID + "/slices")
            .header(HttpHeaders.IF_MATCH, TEST_ETAG)).andExpect(status().isOk());
    }

    @Test
    void getPartitionChunks_Body_OK() throws Exception {
        when(infoService.getValueForKey(any())).thenReturn(TEST_ETAG);
        mvc.perform(
          MockMvcRequestBuilders.post(
              "/lists/" + TEST_KID + "/partitions/" + TEST_PARTITION_ID + "/chunks/" + TEST_CID + "/slices")
            .header(HttpHeaders.IF_MATCH, TEST_ETAG).contentType(MediaType.APPLICATION_JSON_VALUE)
            .content("[\"filter1\", \"filter2\"]")).andExpect(status().isOk());
    }

    @Test
    void getPartitionChunks_ModifiedSince_OK() throws Exception {
        when(infoService.getValueForKey(any())).thenReturn(TEST_ETAG);
        mvc.perform(
            MockMvcRequestBuilders.post(
                "/lists/" + TEST_KID + "/partitions/" + TEST_PARTITION_ID + "/chunks/" + TEST_CID + "/slices")
              .header(HttpHeaders.IF_MATCH, TEST_ETAG).header(HttpHeaders.IF_MODIFIED_SINCE, ZonedDateTime.now()))
          .andExpect(status().isOk());
    }

    @Test
    void getPartitionChunks_ModifiedSince_BADREQUEST() throws Exception {
        when(infoService.getValueForKey(any())).thenReturn(TEST_ETAG);
        mvc.perform(
            MockMvcRequestBuilders.post(
                "/lists/" + TEST_KID + "/partitions/" + TEST_PARTITION_ID + "/chunks/" + TEST_CID + "/slices")
              .header(HttpHeaders.IF_MATCH, TEST_ETAG).header(HttpHeaders.IF_MODIFIED_SINCE, "THIS IS NO VALID TIME"))
          .andExpect(status().isBadRequest());
    }

    @Test
    void getPartitionChunks_ModifiedSince_Body_OK() throws Exception {
        when(infoService.getValueForKey(any())).thenReturn(TEST_ETAG);
        mvc.perform(
            MockMvcRequestBuilders.post(
                "/lists/" + TEST_KID + "/partitions/" + TEST_PARTITION_ID + "/chunks/" + TEST_CID + "/slices")
              .header(HttpHeaders.IF_MATCH, TEST_ETAG).header(HttpHeaders.IF_MODIFIED_SINCE, ZonedDateTime.now())
              .contentType(MediaType.APPLICATION_JSON_VALUE).content("[\"filter1\", \"filter2\"]"))
          .andExpect(status().isOk());
    }

    @Test
    void getSlice_OK() throws Exception {
        when(infoService.getValueForKey(any())).thenReturn(TEST_ETAG);
        mvc.perform(
          MockMvcRequestBuilders.get(
              "/lists/" + TEST_KID + "/partitions/" + TEST_PARTITION_ID + "/chunks/" + TEST_CID + "/slices/" + TEST_SID)
            .header(HttpHeaders.IF_MATCH, TEST_ETAG)).andExpect(status().isOk());
    }

    @Test
    void getSlice_ModifiedSince_OK() throws Exception {
        when(infoService.getValueForKey(any())).thenReturn(TEST_ETAG);
        mvc.perform(
            MockMvcRequestBuilders.get(
                "/lists/" + TEST_KID + "/partitions/" + TEST_PARTITION_ID + "/chunks/" + TEST_CID + "/slices/" + TEST_SID)
              .header(HttpHeaders.IF_MATCH, TEST_ETAG).header(HttpHeaders.IF_MODIFIED_SINCE, ZonedDateTime.now()))
          .andExpect(status().isOk());
    }

    @Test
    void getSlice_ModifiedSince_BADREQUEST() throws Exception {
        when(infoService.getValueForKey(any())).thenReturn(TEST_ETAG);
        mvc.perform(
            MockMvcRequestBuilders.get(
                "/lists/" + TEST_KID + "/partitions/" + TEST_PARTITION_ID + "/chunks/" + TEST_CID + "/slices/" + TEST_SID)
              .header(HttpHeaders.IF_MATCH, TEST_ETAG).header(HttpHeaders.IF_MODIFIED_SINCE, "THIS IS NO VALID TIME"))
          .andExpect(status().isBadRequest());
    }

    private Optional<RevocationListJsonEntity> createTestRevocationListJsonEntity() {
        RevocationListJsonEntity entity = new RevocationListJsonEntity();
        entity.setEtag(TEST_ETAG);
        entity.setCreatedAt(ZonedDateTime.now());
        RevocationListJsonResponseDto.RevocationListJsonResponseItemDto json =
          createRevocationListJsonResponseItemDto();
        List<RevocationListJsonResponseDto.RevocationListJsonResponseItemDto> jsonData = new ArrayList<>();
        jsonData.add(json);
        entity.setJsonData(jsonData);
        return Optional.of(entity);
    }

    private RevocationListJsonResponseDto.RevocationListJsonResponseItemDto createRevocationListJsonResponseItemDto() {
        RevocationListJsonResponseDto.RevocationListJsonResponseItemDto json =
          new RevocationListJsonResponseDto.RevocationListJsonResponseItemDto();
        List<String> hashTypes = new ArrayList<>();
        hashTypes.add(TEST_HASHTYPE);
        json.setExpires(ZonedDateTime.now());
        json.setKid(TEST_KID);
        json.setMode(TEST_MODE);
        json.setHashTypes(hashTypes);
        json.setLastUpdated(ZonedDateTime.now());
        return json;
    }
}
