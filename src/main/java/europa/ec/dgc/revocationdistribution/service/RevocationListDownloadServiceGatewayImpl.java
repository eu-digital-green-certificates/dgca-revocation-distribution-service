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

package europa.ec.dgc.revocationdistribution.service;


import eu.europa.ec.dgc.gateway.connector.DgcGatewayRevocationListDownloadConnector;
import eu.europa.ec.dgc.gateway.connector.dto.RevocationBatchDto;
import eu.europa.ec.dgc.gateway.connector.dto.RevocationBatchListDto;
import eu.europa.ec.dgc.gateway.connector.exception.RevocationBatchDownloadException;
import eu.europa.ec.dgc.gateway.connector.exception.RevocationBatchGoneException;
import eu.europa.ec.dgc.gateway.connector.exception.RevocationBatchParseException;
import eu.europa.ec.dgc.gateway.connector.iterator.DgcGatewayRevocationListDownloadIterator;
import java.util.ArrayList;
import java.util.List;
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

    private final RevocationListService revocationListservice;


    /**
     * Synchronises the revocation list with the gateway.
     */
    @Scheduled(fixedDelayString = "${dgc.revocationListDownload.timeInterval}")
    @SchedulerLock(name = "RevocationListDownloadService_downloadRevocationList", lockAtLeastFor = "PT0S",
        lockAtMostFor = "${dgc.revocationListDownload.lockLimit}")
    public void downloadRevocationList() {
        log.info("Revocation list download started");
        DgcGatewayRevocationListDownloadIterator revocationListIterator =
            dgcGatewayRevocationListDownloadConnector.getRevocationListDownloadIterator();

        while(revocationListIterator.hasNext()) {
            List<RevocationBatchListDto.RevocationBatchListItemDto> batchListItems =  revocationListIterator.next();

            log.info(batchListItems.toString());

            List<String> deletedBatchIds = new ArrayList<>();
            List<String> goneBatchIds = new ArrayList<>();


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
            }
        }


        log.info("Revocation list download finished");
    }

}
