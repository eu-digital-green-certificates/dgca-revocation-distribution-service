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
import eu.europa.ec.dgc.revocationdistribution.dto.RevocationListJsonResponseDto.RevocationListJsonResponseItemDto;
import eu.europa.ec.dgc.revocationdistribution.dto.SliceDataDto;
import eu.europa.ec.dgc.revocationdistribution.entity.KidViewEntity;
import eu.europa.ec.dgc.revocationdistribution.entity.PartitionEntity;
import eu.europa.ec.dgc.revocationdistribution.entity.RevocationListJsonEntity;
import eu.europa.ec.dgc.revocationdistribution.entity.SliceEntity;
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
import eu.europa.ec.dgc.revocationdistribution.utils.HelperFunctions;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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

    private final SliceCalculationService sliceCalculationService;

    private final PointViewRepository pointViewRepository;

    private final VectorViewRepository vectorViewRepository;

    private final CoordinateViewRepository coordinateViewRepository;

    private final PartitionRepository partitionRepository;

    private final SliceRepository sliceRepository;

    private final HelperFunctions helperFunctions;

    private final PointViewMapper pointViewMapper;

    private final VectorViewMapper vectorViewMapper;

    private final CoordinateViewMapper coordinateViewMapper;

    private String etag;
    private String oldEtag;


    @PostConstruct
    private void postConstruct() {
        etag = infoService.getValueForKey(InfoService.CURRENT_ETAG);
        if (etag == null) { etag = ""; }
    }

    public void generateNewDataSet() {
        log.info("Started generation of new data set.");

        oldEtag = etag;
        etag = UUID.randomUUID().toString();

        List<KidViewEntity> kidViewEntityList = kidViewRepository.findAll();

        ChangeList changeList = generateList();

        handleChangeList(changeList);

        infoService.setNewEtag(etag);

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

        //Update Items
        kidViewEntityList.stream().forEach(kve -> {

            if (kve.getTypes().isEmpty() && kve.getExpired() == null ) {
                log.debug("Delete kid entry : {} ",kve.getKid());
                if ( itemsMap.remove(kve.getKid()) != null ) {
                    changeList.getDeleted().add( new ChangeListItem(kve, null));
                }
            }else{
                if (kve.isUpdated()) {
                    RevocationListJsonResponseItemDto oldItem;
                    RevocationListJsonResponseItemDto item;
                    item = new RevocationListJsonResponseItemDto();
                    item.setKid(kve.getKid());
                    item.setLastUpdated(kve.getLastUpdated());
                    item.setHashTypes(kve.getTypes());
                    item.setMode(kve.getStorageMode());
                    item.setExpires(kve.getExpired());
                    oldItem = itemsMap.put(item.getKid(), item);
                    if (oldItem != null) {
                        changeList.getUpdated().add( new ChangeListItem(kve, oldItem.getMode()));
                    } else {
                        changeList.getCreated().add( new ChangeListItem(kve,null));
                    }
                }
            }
        });

        RevocationListJsonEntity revocationListJsonEntity = new RevocationListJsonEntity();
        revocationListJsonEntity.setEtag(etag);
        revocationListJsonEntity.setJsonData(new ArrayList<>(itemsMap.values()));

        revocationListService.saveRevocationListJson(revocationListJsonEntity);

        log.info(itemsMap.values().toString());
        return changeList;
    }

    private void handleChangeList(ChangeList changeList) {
        //handle deleted kIds
        List<String>deletedKids =
            changeList.getDeleted().stream().map(ChangeListItem::getKidId).collect(Collectors.toList());

        markDataForRemoval(deletedKids);

        //handle updated kIds
        List<String>updatedKids =
            changeList.getUpdated().stream().map(ChangeListItem::getKidId).collect(Collectors.toList());

        markDataForRemoval(updatedKids);

        generatePattern(changeList.getUpdated());

        //handle created kIds
        generatePattern(changeList.getCreated());
    }


    private void markDataForRemoval(List<String> kIds){
        if(!kIds.isEmpty()) {
            partitionRepository.setToBeDeletedForKids(kIds);
            sliceRepository.setToBeDeletedForKids(kIds);
        }
    }


    private void generatePattern(List<ChangeListItem> changeListItems) {

        for(ChangeListItem changeItem : changeListItems) {
            switch(changeItem.getNewStorageMode()){
                case "POINT": {
                    log.debug("Create pattern for kid {} in POINT mode.", changeItem.getKidId());
                    generatePartitionsForKidInPointMode(changeItem);
                    break;
                }
                case "VECTOR":{
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

    private void generatePartitionsForKidInPointMode(ChangeListItem changeItem){

        List<ChunkMetaViewDto> entities = pointViewRepository.findAllByKid(changeItem.getKidId()).stream()
            .map(pointViewMapper::map).collect(Collectors.toList());

        generatePartition(entities, changeItem.getKidId(), null);


    }

    private void generatePartitionsForKidInVectorMode(ChangeListItem changeItem){

        //get all ids for kId
        List<String> partitionIds = vectorViewRepository.findDistinctIdsByKid(changeItem.getKidId());

        log.debug("PartionIds {}",partitionIds);

        for (String partitionId : partitionIds ){
            List<ChunkMetaViewDto> entities =
                vectorViewRepository.findAllByKidAndId(changeItem.getKidId(), partitionId).stream()
                .map(vectorViewMapper::map).collect(Collectors.toList());

            generatePartition(entities, changeItem.getKidId(), partitionId);

        }

    }

    private void generatePartitionsForKidInCoordinateMode(ChangeListItem changeItem){

        //get all ids for kId
        List<String> partitionIds = coordinateViewRepository.findDistinctIdsByKid(changeItem.getKidId());

        log.debug("PartionIds {}",partitionIds);

        for (String partitionId : partitionIds ){
            List<ChunkMetaViewDto> entities =
                coordinateViewRepository.findAllByKidAndId(changeItem.getKidId(), partitionId).stream()
                    .map(coordinateViewMapper::map).collect(Collectors.toList());

            generatePartition(entities, changeItem.getKidId(), partitionId);

        }

    }

    private void generatePartition(List<ChunkMetaViewDto> entities,
                                   String kid, String id) {

        String x = null;
        String y = null;
        ZonedDateTime lastUpdated = ZonedDateTime.parse("2021-06-01T00:00:00Z");
        ZonedDateTime expired = ZonedDateTime.parse("2021-06-01T00:00:00Z");

        if (entities.isEmpty()){
            log.info("No Entries found in Point View for kid: {} id: {} x: {} y: {}", kid, id);
            return;
        }

        Map<String, Map<String,PartitionChunksJsonItemDto>> chunksJson = new HashMap<>();

        for (ChunkMetaViewDto mve : entities) {

            if ( !Objects.equals(mve.getKid(), kid) || !Objects.equals(mve.getId(), id)) {
                log.error("Kid and/or id does not match: kid: {} , {} id {}, {}",kid , mve.getKid() , id , mve.getId());
            }
            else {

                SliceDataDto sliceDataDto = sliceCalculationService.calculateChunk(mve.getHashes());
                if (sliceDataDto != null) {
                    Map<String, PartitionChunksJsonItemDto> chunkItemsMap;

                    if (chunksJson.containsKey(mve.getChunk())) {
                        chunkItemsMap = chunksJson.get(mve.getChunk());
                    } else {
                        chunkItemsMap = new HashMap<>();
                    }

                    chunkItemsMap.put(helperFunctions.getDateTimeString(mve.getExpired()), sliceDataDto.getMetaData());
                    chunksJson.put(mve.getChunk(), chunkItemsMap);

                    saveSlice(mve.getKid(), id, mve.getChunk(), sliceDataDto.getMetaData().getHash(),
                        mve.getLastUpdated(), mve.getExpired(), sliceDataDto.getBinaryData());

                    x = mve.getX();
                    y = mve.getY();

                    lastUpdated = lastUpdated.isAfter(mve.getLastUpdated()) ? lastUpdated : mve.getLastUpdated();
                    expired = expired.isAfter(mve.getExpired()) ? expired : mve.getExpired();
                }
            }
        }
        if (!chunksJson.isEmpty()) {
            savePartition(kid, id, x, y, null,
                lastUpdated, expired, chunksJson);
        }

    }

    private void saveSlice(String kid, String id, String chunk, String hash,
                           ZonedDateTime lastUpdated, ZonedDateTime expired, byte[] binaryData) {

        SliceEntity sliceEntity = new SliceEntity();

        sliceEntity.setEtag(etag);
        sliceEntity.setKid(kid);
        sliceEntity.setId(id);
        sliceEntity.setChunk(chunk);
        sliceEntity.setHash(hash);
        sliceEntity.setLastUpdated(lastUpdated);
        sliceEntity.setExpired(expired);
        sliceEntity.setBinaryData(binaryData);
        sliceEntity.setToBeDeleted(false);

        sliceRepository.save(sliceEntity);

    }


        private void savePartition(String kid, String id, String x, String y, String z,
                               ZonedDateTime lastUpdated, ZonedDateTime expired,
                               Map<String, Map<String,PartitionChunksJsonItemDto>> chunksJson){

        PartitionEntity partitionEntity = new PartitionEntity();

        partitionEntity.setEtag(etag);
        partitionEntity.setKid(kid);
        partitionEntity.setId(id);
        partitionEntity.setX(x);
        partitionEntity.setY(y);
        partitionEntity.setZ(z);
        partitionEntity.setLastUpdated(lastUpdated);
        partitionEntity.setExpired(expired);
        partitionEntity.setChunks(chunksJson);
        partitionEntity.setToBeDeleted(false
        );
        partitionRepository.save(partitionEntity);

    }




    private List<RevocationListJsonResponseItemDto> getRevocationListData(String etag) {
        Optional<RevocationListJsonEntity> optionalData =  revocationListService.getRevocationListJsonData(etag);
        if(optionalData.isPresent()) {
            return optionalData.get().getJsonData();
        }

        return new ArrayList<>();
    }

    private void cleanupData() {
        // set all entries in hashes table to updated false
        revocationListService.setAllHashesUpdatedStatesToFalse();

        // remove all orphaned entries in hashes table
        revocationListService.deleteAllOrphanedHashes();

        //delete all older calculations of the revocation list json
        revocationListService.deleteAllOutdatedJsonLists(etag);

    }



}
