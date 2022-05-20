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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonObject;
import eu.europa.ec.dgc.gateway.connector.DgcGatewayCountryListDownloadConnector;
import eu.europa.ec.dgc.gateway.connector.DgcGatewayValidationRuleDownloadConnector;
import eu.europa.ec.dgc.gateway.connector.DgcGatewayValueSetDownloadConnector;
import eu.europa.ec.dgc.gateway.connector.client.DgcGatewayConnectorRestClientConfig;
import eu.europa.ec.dgc.gateway.connector.dto.RevocationBatchDto;
import eu.europa.ec.dgc.gateway.connector.dto.RevocationHashTypeDto;
import eu.europa.ec.dgc.revocationdistribution.client.IssuanceDgciRestClient;
import eu.europa.ec.dgc.revocationdistribution.dto.ChunkMetaViewDto;
import eu.europa.ec.dgc.revocationdistribution.dto.DidAuthentication;
import eu.europa.ec.dgc.revocationdistribution.dto.DidDocument;
import eu.europa.ec.dgc.revocationdistribution.dto.RevocationCheckTokenPayload;
import eu.europa.ec.dgc.revocationdistribution.dto.RevocationListJsonResponseDto;
import eu.europa.ec.dgc.revocationdistribution.entity.BatchListEntity;
import eu.europa.ec.dgc.revocationdistribution.entity.HashesEntity;
import eu.europa.ec.dgc.revocationdistribution.entity.RevocationListJsonEntity;
import eu.europa.ec.dgc.revocationdistribution.exception.DataNotFoundException;
import eu.europa.ec.dgc.revocationdistribution.exception.TokenValidationException;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.util.ReflectionTestUtils;

@Slf4j
@SpringBootTest(
  properties = {
    "server.port=8080",
    "springdoc.api-docs.enabled=true",
    "springdoc.api-docs.path=/openapi",
    "dgc.gateway.connector.enabled=false"
  },
  webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT
)
@AutoConfigureEmbeddedDatabase(provider = ZONKY)
class LookupTest {
    @MockBean
    DgcGatewayConnectorRestClientConfig dgcGatewayConnectorRestClientConfig;

    @MockBean
    IssuanceDgciRestClient issuanceDgciRestClient;

    @MockBean
    DgcGatewayValidationRuleDownloadConnector dgcGatewayValidationRuleDownloadConnector;

    @MockBean
    DgcGatewayValueSetDownloadConnector dgcGatewayValueSetDownloadConnector;

    @MockBean
    DgcGatewayCountryListDownloadConnector dgcGatewayCountryListDownloadConnector;

    @Autowired
    LookupService classUnderTest;

    @Autowired
    RevocationListService revocationListService;

    final String TEST_HASH = "TESTHASH";
    final String SEARCH_HASH = "4c44931c0487";
    final String VALID_TOKEN =
      "eyJhbGciOiJFUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiI5Mjc3NDVlYzM0NTJiYTUzNzYzZjEyMGFhMGNjZWZlMDllMmY2MWI2NzlmODYxNDgwNDc1NTYwNTUyYTc5YjYwIiwicGF5bG9hZCI6WyI5Mjc3NDVlYzM0NTJiYTUzNzYzZjEyMGFhMGNjZWZlMCIsImVjZGNhMjAzMjlhZjUwZGIwN2VjNDM5YTQwM2E3YmYwIiwiYjRiYTFlOWFmNWQ5MTgzM2ZkY2VlODEzZWYyOTA1YmUiXSwiZXhwIjoxNjQ5NDI5NjQ4NTAxfQ.MEUCIH2s-iNykya_39yjisR_3KnpgsE0RHDMw61jLNzjhYrsAiEAsMbMwey8PyWvPVi-fJ7XSdHKhLcfh5kQWKwdvSTxZj0";


    @Test
    void validateRevocationCheckTokensRunThrough() throws JsonProcessingException {
        //ReflectionTestUtils.setField(classUnderTest, "issuanceDgciRestClient", issuanceDgciRestClient);
        DidDocument document = createDidDocument();
        ResponseEntity<DidDocument> response = new ResponseEntity<>(document,HttpStatus.OK);
        when(issuanceDgciRestClient.getDgciByHash(any())).thenReturn(response);
        List<String> tokens = new ArrayList<>();
        tokens.add(VALID_TOKEN);
        var result = classUnderTest.validateRevocationCheckTokens(tokens);
        assertFalse(result.isEmpty());
    }

    @Test
    void validateRevocationCheckTokensWrongTokenFormat() {
        List<String> tokens = new ArrayList<>();
        tokens.add("TESTTOKEN");
        assertThrows(TokenValidationException.class, () -> classUnderTest.validateRevocationCheckTokens(tokens));
    }

    @Test
    void checkForRevocationRunThrough() {
        String batchId = "TESTBATCHID";
        RevocationBatchDto dto = createTestRevocationBatchDto();
        revocationListService.updateRevocationListBatch(batchId, dto);

        List<RevocationCheckTokenPayload> payloads = new ArrayList<>();
        payloads.add(createRevocationCheckTokenPayload());
        var result = classUnderTest.checkForRevocation(payloads);
        assertFalse(result.isEmpty());
    }

    private RevocationCheckTokenPayload createRevocationCheckTokenPayload() {
        RevocationCheckTokenPayload payload = new RevocationCheckTokenPayload();
        List<String> payloadStrings = new ArrayList<>();
        payloadStrings.add(SEARCH_HASH);
        payload.setPayload(payloadStrings);
        payload.setExp(Long.MAX_VALUE);
        payload.setSub("TESTSUB");
        return payload;
    }

    private RevocationBatchDto createTestRevocationBatchDto() {
        RevocationBatchDto dto = new RevocationBatchDto();
        dto.setHashType(RevocationHashTypeDto.SIGNATURE);
        dto.setCountry("DE");
        dto.setExpires(ZonedDateTime.now().plusWeeks(2));
        RevocationBatchDto.BatchEntryDto entry = createBatchEntryDto();
        List<RevocationBatchDto.BatchEntryDto> entries = new ArrayList<>();
        entries.add(entry);
        dto.setEntries(entries);
        return dto;
    }

    private RevocationBatchDto.BatchEntryDto createBatchEntryDto() {
        RevocationBatchDto.BatchEntryDto entry = new RevocationBatchDto.BatchEntryDto();
        entry.setHash(TEST_HASH);
        return entry;
    }
    private DidDocument createDidDocument() throws JsonProcessingException {

        DidDocument document = new DidDocument();
        document.setId("URN:UVCI:V1:DE:2UETP55W3VVS7LF2MOB8W1998L");
        document.setContext("https://w3id.org/did/v1");
        document.setController("URN:UVCI:V1:DE:2UETP55W3VVS7LF2MOB8W1998L");
        DidAuthentication auth = new DidAuthentication();
        auth.setController("URN:UVCI:V1:DE:2UETP55W3VVS7LF2MOB8W1998L");
        auth.setExpires("2023-04-08T14:27:33Z");
        auth.setType("EcdsaSecp256r1VerificationKey2019");
        String json = "{\n" +
          "\"kty\": \"EC\",\n" +
          "\"crv\": \"P-256\",\n" +
          "\"x\": \"edmj8B6t1YF5yrDzihq6ezp9bZ-sYa92z84tGU9Xiwk\",\n" +
          "\"y\": \"4PAeyxdxh3hjsvR6F-V_wpWHCkM7mI9EAS47-FxZOxk\"\n" +
          "}";
        ObjectMapper mapper = new ObjectMapper();
        JsonNode publicKeyJSW = mapper.readTree(json);
        auth.setPublicKeyJsw(publicKeyJSW);
        List<DidAuthentication> auths = new ArrayList<>();
        auths.add(auth);
        document.setAuthentication(auths);
        return  document;
    }
}
