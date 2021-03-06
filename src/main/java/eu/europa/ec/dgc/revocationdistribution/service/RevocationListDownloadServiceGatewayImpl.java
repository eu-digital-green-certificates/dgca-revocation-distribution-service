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
import eu.europa.ec.dgc.revocationdistribution.config.DgcConfigProperties;
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

    private final DgcGatewayRevocationListDownloadConnector dgcGatewayDownloadConnector;

    private final DgcConfigProperties properties;

    private final InfoService infoService;

    private final RevocationListService revocationListservice;

    private final GeneratorService generatorService;


    private ZonedDateTime lastUpdatedBatchDate;


    @PostConstruct
    private void postConstruct() {

        String lastUpdatedString = infoService.getValueForKey(InfoService.LAST_UPDATED_KEY);

        if (lastUpdatedString != null) {
            try {
                lastUpdatedBatchDate = ZonedDateTime.parse(lastUpdatedString);
            } catch (DateTimeParseException e) {
                log.error("Could not parse loaded last Updated timestamp: {}", lastUpdatedString);
            }
        } else {
            // Load initial timestamp
            try {
                lastUpdatedBatchDate = ZonedDateTime.parse("2021-06-01T00:00:00Z");
            } catch (DateTimeParseException e) {
                log.error("Could not parse loaded last Updated timestamp: 2021-06-01T00:00:00Z");
            }
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

        boolean needsCalculation = getNeedsCalculation();

        int downloadLimit = properties.getRevocationListDownload().getDownloadLimit();

        ZonedDateTime abortTime =  ZonedDateTime.now().plusSeconds(downloadLimit / 1000);

        DgcGatewayRevocationListDownloadIterator revocationListIterator;

        if (lastUpdatedBatchDate != null) {
            revocationListIterator =
                dgcGatewayDownloadConnector.getRevocationListDownloadIterator(lastUpdatedBatchDate);
        } else {
            revocationListIterator = dgcGatewayDownloadConnector.getRevocationListDownloadIterator();
        }

        if (revocationListIterator.hasNext()) {
            needsCalculation = true;
        } else {
            log.info("There was no new data loaded from the Gateway.");
        }

        List<String> deletedBatchIds = new ArrayList<>();
        List<String> goneBatchIds = new ArrayList<>();

        while (revocationListIterator.hasNext() && abortTime.isAfter(ZonedDateTime.now())) {
            List<RevocationBatchListDto.RevocationBatchListItemDto> batchListItems = revocationListIterator.next();

            for (RevocationBatchListDto.RevocationBatchListItemDto batchListItem : batchListItems) {
                if (batchListItem.getDeleted()) {
                    deletedBatchIds.add(batchListItem.getBatchId());
                } else {
                    try {

                        RevocationBatchDto revocationBatchDto =
                            dgcGatewayDownloadConnector.getRevocationListBatchById(batchListItem.getBatchId());

                        //log.trace(revocationBatchDto.toString());

                        revocationListservice.updateRevocationListBatch(batchListItem.getBatchId(), revocationBatchDto);
                        log.info("Downloaded batch: {}", batchListItem.getBatchId());

                    } catch (RevocationBatchGoneException e) {
                        goneBatchIds.add(batchListItem.getBatchId());
                    } catch (RevocationBatchDownloadException | RevocationBatchParseException e) {
                        log.error("Batch download failed");
                    }
                }
                lastUpdatedBatchDate = batchListItem.getDate();

                if (abortTime.isBefore(ZonedDateTime.now())) {
                    break;
                }
            }
        }

        if (!deletedBatchIds.isEmpty()) {
            log.info("Deleted batches: {}", deletedBatchIds);
            revocationListservice.deleteBatchListItemsByIds(deletedBatchIds);
        }

        if (!goneBatchIds.isEmpty()) {
            log.info("Gone Batches: {}", goneBatchIds);
            revocationListservice.deleteBatchListItemsByIds(goneBatchIds);
        }

        List<String> expiredBatchIds = revocationListservice.getExpiredBatchIds();
        if (!expiredBatchIds.isEmpty()) {
            log.info("Delete expired batches: {}", expiredBatchIds);
            revocationListservice.deleteBatchListItemsByIds(expiredBatchIds);
            needsCalculation = true;
        }

        saveLastUpdated();

        saveNeedsCalculation(needsCalculation);

        if (needsCalculation) {
            generatorService.generateNewDataSet();
        } else {
            log.info("No recalculation of data needed.");
        }

        saveNeedsCalculation(false);

        log.info("Revocation list download finished");
    }

    private void saveLastUpdated() {
        log.info("Save last updated date: {}", lastUpdatedBatchDate);
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX");
        infoService.setValueForKey(InfoService.LAST_UPDATED_KEY, dateTimeFormatter.format(lastUpdatedBatchDate));
    }

    private boolean getNeedsCalculation() {
        String needsCalculationString = infoService.getValueForKey(InfoService.NEEDS_CALCULATION_KEY);
        return Boolean.parseBoolean(needsCalculationString);
    }

    private void saveNeedsCalculation(boolean needsCalculation) {
        infoService.setValueForKey(InfoService.NEEDS_CALCULATION_KEY, Boolean.toString(needsCalculation));
    }

}
