package europa.ec.dgc.revocationdistribution.service;


import europa.ec.dgc.revocationdistribution.dto.PartitionChunksJsonItemDto;
import europa.ec.dgc.revocationdistribution.dto.RevocationListJsonResponseDto.RevocationListJsonResponseItemDto;
import europa.ec.dgc.revocationdistribution.entity.KidViewEntity;
import europa.ec.dgc.revocationdistribution.entity.PartitionEntity;
import europa.ec.dgc.revocationdistribution.entity.PointViewEntity;
import europa.ec.dgc.revocationdistribution.entity.RevocationListJsonEntity;
import europa.ec.dgc.revocationdistribution.model.ChangeList;
import europa.ec.dgc.revocationdistribution.model.ChangeListItem;
import europa.ec.dgc.revocationdistribution.repository.KidViewRepository;
import europa.ec.dgc.revocationdistribution.repository.PartitionRepository;
import europa.ec.dgc.revocationdistribution.repository.PointViewRepository;
import europa.ec.dgc.revocationdistribution.utils.HelperFunctions;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@RequiredArgsConstructor
@Service
public class GeneratorService {

    private final KidViewRepository kidViewRepository;
    private final RevocationListService revocationListService;
    private final InfoService infoService;

    private final PointViewRepository pointViewRepository;

    private final PartitionRepository partitionRepository;

    private final HelperFunctions helperFunctions;

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

        for (KidViewEntity kidViewEntity : kidViewEntityList) {

            log.info("KidView({},{},{},{}, {})",
                kidViewEntity.getKid(),
                kidViewEntity.getStorageMode(),
                kidViewEntity.getTypesString(),
                kidViewEntity.getLastUpdated(),
                kidViewEntity.getExpired());
        }
        ChangeList changeList = generateList();

        generatePattern(changeList);

        infoService.setValueForKey(InfoService.CURRENT_ETAG, etag);
        cleanupData();

        log.info("Finished generation of new data set.");
    }


    private void generatePattern(ChangeList changeList) {

        for(ChangeListItem changeItem : changeList.getUpdated()) {
            switch(changeItem.getNewStorageMode()){
                case "POINT": {
                    log.info("Create pattern for kid {} in POINT mode.", changeItem.getKidId());

                    generatePatternForKidInPointMode(changeItem);

                }
                case "VECTOR":{
                    log.info("Create pattern for kid {} in VECTOR mode.", changeItem.getKidId());
                }
                case "COORDINATE": {
                    log.info("Create pattern for kid {} in COORDINATE mode.", changeItem.getKidId());
                }
            }


        }

    }

    private void generatePatternForKidInPointMode(ChangeListItem changeItem){
        List<PointViewEntity> pointViewEntities = pointViewRepository.findAllByKid(changeItem.getKidId());
        log.info("ReadViewData");

        if (pointViewEntities.isEmpty()){
            log.info("No Entries found in Point View for kid: {}", changeItem.getKidId());
            return;
        }
        PartitionEntity partitionEntity = new PartitionEntity();
        partitionEntity.setKid(changeItem.getKidId());
        partitionEntity.setLastUpdated(changeItem.getLastUpdated());
        partitionEntity.setExpired(changeItem.getExpired());

        Map<String, Map<String,PartitionChunksJsonItemDto>> chunksJson = new HashMap<>();

        for (PointViewEntity pve : pointViewEntities) {
            PartitionChunksJsonItemDto item = new PartitionChunksJsonItemDto();

            // TODO: calculate binary data
            item.setType("Bloom");
            item.setVersion("1.0");
            item.setHash(pve.getHashes().toString());

            Map<String,PartitionChunksJsonItemDto> chunkItemsMap;

            if (chunksJson.containsKey(pve.getChunk())) {
                chunkItemsMap = chunksJson.get(pve.getChunk());
            } else {
                chunkItemsMap = new HashMap<>();
            }

            chunkItemsMap.put(helperFunctions.getDateTimeString(pve.getExpired()), item);
            chunksJson.put(pve.getChunk(), chunkItemsMap);
        }

        partitionEntity.setChunks(chunksJson);
        partitionRepository.save(partitionEntity);
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
                log.info("Delete kid entry : {} ",kve.getKid());
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

    private List<RevocationListJsonResponseItemDto> getRevocationListData(String etag) {
        Optional<RevocationListJsonEntity> optionalData =  revocationListService.getRevocationListJsonData(etag);
        if(optionalData.isPresent()) {
            return optionalData.get().getJsonData();
        }

        return new ArrayList<>();
    }

    private void cleanupData() {
        // set all entries in hashes table to updated false
        //revocationListService.setAllHashesUpdatedStatesToFalse();

        // remove all orphaned entries in hashes table
        revocationListService.deleteAllOrphanedHashes();

    }



}
