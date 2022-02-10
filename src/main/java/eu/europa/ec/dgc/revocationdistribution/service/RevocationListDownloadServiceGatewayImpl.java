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


import eu.europa.ec.dgc.gateway.connector.DgcGatewayRevocationListDownloadConnector;
import eu.europa.ec.dgc.gateway.connector.dto.RevocationBatchDto;
import eu.europa.ec.dgc.gateway.connector.dto.RevocationBatchListDto;
import eu.europa.ec.dgc.gateway.connector.exception.RevocationBatchDownloadException;
import eu.europa.ec.dgc.gateway.connector.exception.RevocationBatchGoneException;
import eu.europa.ec.dgc.gateway.connector.exception.RevocationBatchParseException;
import eu.europa.ec.dgc.gateway.connector.iterator.DgcGatewayRevocationListDownloadIterator;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;


/**
 * A service to download the revocation List from the gateway.
 */
@Slf4j
@RequiredArgsConstructor
@Component
@ConditionalOnProperty("dgc.gateway.connector.enabled")
public class RevocationListDownloadServiceGatewayImpl {

    private final  DgcGatewayRevocationListDownloadConnector dgcGatewayRevocationListDownloadConnector;

    private final InfoService infoService;

    private final RevocationListService revocationListservice;

    private final GeneratorService generatorService;



    private ZonedDateTime lastUpdatedBatchDate;


    @PostConstruct
    private void postConstruct() {

        String lastUpdatedString = infoService.getValueForKey(InfoService.LAST_UPDATED_KEY);

        if (lastUpdatedString != null)
            try {
                lastUpdatedBatchDate = ZonedDateTime.parse(lastUpdatedString);
            }catch(DateTimeParseException e) {
                log.error("Could not parse loaded last Updated timestamp: {}", lastUpdatedString);
            }
    }


    /**
     * Synchronises the revocation list with the gateway.
     */
    @Scheduled(fixedDelayString = "${dgc.revocationListDownload.timeInterval}")
    @SchedulerLock(name = "RevocationListDownloadService_downloadRevocationList", lockAtLeastFor = "PT0S",
        lockAtMostFor = "${dgc.revocationListDownload.lockLimit}")
    public void downloadRevocationList() {
        log.info("Revocation list download started");

        DgcGatewayRevocationListDownloadIterator revocationListIterator;

        if(lastUpdatedBatchDate != null) {
            revocationListIterator =
                dgcGatewayRevocationListDownloadConnector.getRevocationListDownloadIterator(lastUpdatedBatchDate);
        } else{
            revocationListIterator = dgcGatewayRevocationListDownloadConnector.getRevocationListDownloadIterator();
        }

        List<String> deletedBatchIds = new ArrayList<>();
        List<String> goneBatchIds = new ArrayList<>();

        while(revocationListIterator.hasNext()) {
            List<RevocationBatchListDto.RevocationBatchListItemDto> batchListItems =  revocationListIterator.next();

            log.info(batchListItems.toString());

            for(RevocationBatchListDto.RevocationBatchListItemDto batchListItem : batchListItems) {
                if (batchListItem.getDeleted()) {
                    deletedBatchIds.add(batchListItem.getBatchId());
                } else {
                    try {

                        RevocationBatchDto revocationBatchDto =
                            dgcGatewayRevocationListDownloadConnector.getRevocationListBatchById(batchListItem.getBatchId());
                        log.info(revocationBatchDto.toString());
                        revocationListservice.updateRevocationListBatch(batchListItem.getBatchId(), revocationBatchDto);

                    } catch(RevocationBatchGoneException e) {
                        goneBatchIds.add(batchListItem.getBatchId());
                    } catch (RevocationBatchDownloadException | RevocationBatchParseException e) {
                        log.error("Batch download failed");
                    }
                }
                lastUpdatedBatchDate = batchListItem.getDate();
            }
        }

        if (!deletedBatchIds.isEmpty()) {
            revocationListservice.deleteBatchListItemsByIds(deletedBatchIds);
        }

        if (!goneBatchIds.isEmpty()) {
            revocationListservice.deleteBatchListItemsByIds(goneBatchIds);
        }
        saveLastUpdated();
        generatorService.generateNewDataSet();

        log.info("Revocation list download finished");
    }

    private void saveLastUpdated(){
        log.info("Save last updated date: {}",lastUpdatedBatchDate);
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX");
        infoService.setValueForKey(InfoService.LAST_UPDATED_KEY, dateTimeFormatter.format(lastUpdatedBatchDate));
    }

}
