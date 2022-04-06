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
import eu.europa.ec.dgc.revocationdistribution.dto.RevocationListJsonResponseDto.RevocationListJsonResponseItemDto;
import eu.europa.ec.dgc.revocationdistribution.entity.KidViewEntity;
import eu.europa.ec.dgc.revocationdistribution.entity.RevocationListJsonEntity;
import eu.europa.ec.dgc.revocationdistribution.mapper.CoordinateViewMapper;
import eu.europa.ec.dgc.revocationdistribution.mapper.PointViewMapper;
import eu.europa.ec.dgc.revocationdistribution.mapper.VectorViewMapper;
import eu.europa.ec.dgc.revocationdistribution.model.ChangeList;
import eu.europa.ec.dgc.revocationdistribution.model.ChangeListItem;
import eu.europa.ec.dgc.revocationdistribution.repository.CoordinateViewRepository;
import eu.europa.ec.dgc.revocationdistribution.repository.KidViewRepository;
import eu.europa.ec.dgc.revocationdistribution.repository.PartitionRepository;
import eu.europa.ec.dgc.revocationdistribution.repository.PointViewRepository;
import eu.europa.ec.dgc.revocationdistribution.repository.SliceRepository;
import eu.europa.ec.dgc.revocationdistribution.repository.VectorViewRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@RequiredArgsConstructor
@Service
@Transactional
public class GeneratorService {

    private final KidViewRepository kidViewRepository;

    private final RevocationListService revocationListService;

    private final InfoService infoService;

    private final PointViewRepository pointViewRepository;

    private final VectorViewRepository vectorViewRepository;

    private final CoordinateViewRepository coordinateViewRepository;

    private final PartitionGeneratorService partitionGeneratorService;

    private final PartitionRepository partitionRepository;

    private final SliceRepository sliceRepository;

    private final PointViewMapper pointViewMapper;

    private final VectorViewMapper vectorViewMapper;

    private final CoordinateViewMapper coordinateViewMapper;

    private String etag;
    private String oldEtag;


    @PostConstruct
    private void postConstruct() {
        etag = infoService.getValueForKey(InfoService.CURRENT_ETAG);
        if (etag == null) {
            etag = "";
        }
    }

    /**
     *  The function generates a new dataset for the revocation service.
     */
    public void generateNewDataSet() {
        log.info("Started generation of new data set.");

        oldEtag = etag;
        etag = UUID.randomUUID().toString();

        log.info("Generate new List");
        ChangeList changeList = generateList();

        log.info("Handle Changes");
        handleChangeList(changeList);

        log.info("Update Etag");
        infoService.setNewEtag(etag);

        log.info("Cleanup Data");
        cleanupData();


        log.info("Finished generation of new data set.");
    }



    private ChangeList generateList() {
        ChangeList changeList = new ChangeList();

        List<KidViewEntity> kidViewEntityList = kidViewRepository.findAll();

        List<RevocationListJsonResponseItemDto> items = getRevocationListData(oldEtag);

        //Create items Map
        Map<String, RevocationListJsonResponseItemDto> itemsMap =
            items.stream().collect(Collectors.toMap(i -> i.getKid(), i -> i));

        List<String> goneKids = new ArrayList<>(itemsMap.keySet());
        goneKids.removeAll(kidViewEntityList.stream().map(KidViewEntity::getKid).collect(Collectors.toList()));
        changeList.getDeletedKids().addAll(goneKids);
        log.debug("Gone kid entries: {}", goneKids);


        log.trace("Update items start");
        //Update Items
        kidViewEntityList.stream().forEach(kve -> {

            if (kve.getTypes().isEmpty() && kve.getExpired() == null) {
                log.debug("Delete kid entry : {} ", kve.getKid());
                if (itemsMap.remove(kve.getKid()) != null) {
                    changeList.getDeletedKids().add(kve.getKid());
                }
            } else {

                RevocationListJsonResponseItemDto item = getRevocationListJsonItem(kve);

                if (kve.isUpdated() || (!itemsMap.containsKey(kve.getKid()))) {

                    itemsMap.put(item.getKid(), item);
                    changeList.getUpdated().add(new ChangeListItem(kve));

                } else {
                    RevocationListJsonResponseItemDto oldItem;
                    oldItem = itemsMap.get(kve.getKid());

                    if (!oldItem.equals(item)) {
                        itemsMap.put(item.getKid(), item);
                        changeList.getUpdated().add(new ChangeListItem(kve));
                    }
                }
            }
        });
        log.trace("update items stop");
        RevocationListJsonEntity revocationListJsonEntity = new RevocationListJsonEntity();
        revocationListJsonEntity.setEtag(etag);
        revocationListJsonEntity.setJsonData(new ArrayList<>(itemsMap.values()));
        log.trace("before save");
        revocationListService.saveRevocationListJson(revocationListJsonEntity);
        log.trace("create list finished");
        //log.trace(itemsMap.values().toString());
        return changeList;
    }

    private RevocationListJsonResponseItemDto getRevocationListJsonItem(KidViewEntity kve) {
        RevocationListJsonResponseItemDto item = new RevocationListJsonResponseItemDto();

        item.setKid(kve.getKid());
        item.setLastUpdated(kve.getLastUpdated());
        item.setHashTypes(kve.getTypes());
        item.setMode(kve.getStorageMode());
        item.setExpires(kve.getExpired());

        return item;
    }


    private void handleChangeList(ChangeList changeList) {
        //handle deleted kIds
        markDataForRemoval(changeList.getDeletedKids());

        //handle updated kIds
        List<String> updatedKids =
            changeList.getUpdated().stream().map(ChangeListItem::getKidId).collect(Collectors.toList());

        markDataForRemoval(updatedKids);

        generatePattern(changeList.getUpdated());

        //handle created kIds
        generatePattern(changeList.getCreated());
    }

    private void markDataForRemoval(List<String> kids) {
        if (!kids.isEmpty()) {
            partitionRepository.setToBeDeletedForKids(kids);
            sliceRepository.setToBeDeletedForKids(kids);
        }
    }


    private void generatePattern(List<ChangeListItem> changeListItems) {

        for (ChangeListItem changeItem : changeListItems) {
            switch (changeItem.getNewStorageMode()) {
                case "POINT": {
                    log.debug("Create pattern for kid {} in POINT mode.", changeItem.getKidId());
                    generatePartitionsForKidInPointMode(changeItem);
                    break;
                }
                case "VECTOR": {
                    log.debug("Create pattern for kid {} in VECTOR mode.", changeItem.getKidId());
                    generatePartitionsForKidInVectorMode(changeItem);
                    break;
                }
                case "COORDINATE": {
                    log.debug("Create pattern for kid {} in COORDINATE mode.", changeItem.getKidId());
                    generatePartitionsForKidInCoordinateMode(changeItem);
                    break;
                }
                default: {
                    log.warn("Unrecognised storage mode ({}) for kid: {}",
                        changeItem.getNewStorageMode(), changeItem.getKidId());
                }
            }
        }
    }

    private void generatePartitionsForKidInPointMode(ChangeListItem changeItem) {

        List<ChunkMetaViewDto> entities = pointViewRepository.findAllByKid(changeItem.getKidId()).stream()
            .map(pointViewMapper::map).collect(Collectors.toList());

        partitionGeneratorService.generatePartition(
            etag, entities, changeItem.getKidId(), null, changeItem.getNewStorageMode());


    }

    private void generatePartitionsForKidInVectorMode(ChangeListItem changeItem) {

        //get all ids for kId
        List<String> partitionIds = vectorViewRepository.findDistinctIdsByKid(changeItem.getKidId());

        log.debug("PartionIds {}", partitionIds);

        for (String partitionId : partitionIds) {
            List<ChunkMetaViewDto> entities =
                vectorViewRepository.findAllByKidAndId(changeItem.getKidId(), partitionId).stream()
                    .map(vectorViewMapper::map).collect(Collectors.toList());

            partitionGeneratorService.generatePartition(
                etag, entities, changeItem.getKidId(), partitionId, changeItem.getNewStorageMode());

        }

    }

    private void generatePartitionsForKidInCoordinateMode(ChangeListItem changeItem) {

        //get all ids for kId
        List<String> partitionIds = coordinateViewRepository.findDistinctIdsByKid(changeItem.getKidId());

        log.debug("PartionIds {}", partitionIds);

        for (String partitionId : partitionIds) {
            List<ChunkMetaViewDto> entities =
                coordinateViewRepository.findAllByKidAndId(changeItem.getKidId(), partitionId).stream()
                    .map(coordinateViewMapper::map).collect(Collectors.toList());

            partitionGeneratorService.generatePartition(
                etag, entities, changeItem.getKidId(), partitionId, changeItem.getNewStorageMode());

        }

    }


    private List<RevocationListJsonResponseItemDto> getRevocationListData(String etag) {
        Optional<RevocationListJsonEntity> optionalData = revocationListService.getRevocationListJsonData(etag);
        if (optionalData.isPresent()) {
            return optionalData.get().getJsonData();
        }

        return new ArrayList<>();
    }

    @Transactional
    private void cleanupData() {
        // set all entries in hashes table to updated false
        revocationListService.setAllHashesUpdatedStatesToFalse();

        // remove all orphaned entries in hashes table
        revocationListService.deleteAllOrphanedHashes();

        //delete all older calculations of the revocation list json
        revocationListService.deleteAllOutdatedJsonLists(etag);

    }


}
