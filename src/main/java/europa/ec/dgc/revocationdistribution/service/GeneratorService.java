package europa.ec.dgc.revocationdistribution.service;


import europa.ec.dgc.revocationdistribution.dto.RevocationListJsonResponseDto.RevocationListJsonResponseItemDto;
import europa.ec.dgc.revocationdistribution.entity.KidViewEntity;
import europa.ec.dgc.revocationdistribution.entity.RevocationListJsonEntity;
import europa.ec.dgc.revocationdistribution.repository.KidViewRepository;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.bytebuddy.dynamic.DynamicType;
import org.springframework.stereotype.Service;

@Slf4j
@RequiredArgsConstructor
@Service
public class GeneratorService {

    private final KidViewRepository kidViewRepository;
    private final RevocationListService revocationListService;
    private final InfoService infoService;

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
        generateListJson();

        infoService.setValueForKey(InfoService.CURRENT_ETAG, etag);
        cleanupData();

        log.info("Finished generation of new data set.");
    }


    private void generateListJson() {
        List<KidViewEntity> kidViewEntityList = kidViewRepository.findAll();

        List<RevocationListJsonResponseItemDto> items = getRevocationListData(oldEtag);

        //Create items Map
        Map<String, RevocationListJsonResponseItemDto> itemsMap =
            items.stream().collect(Collectors.toMap(i -> i.getKid(), i -> i));

        //Update Items
        kidViewEntityList.stream().forEach(kve -> {

            if (kve.getTypes().isEmpty() && kve.getExpired() == null ) {
                log.info("Delete kid entry : {} ",kve.getKid());
                itemsMap.remove(kve.getKid());
            }else{
                if (kve.isUpdated()) {
                    RevocationListJsonResponseItemDto item;
                    item = new RevocationListJsonResponseItemDto();
                    item.setKid(kve.getKid());
                    item.setLastUpdated(kve.getLastUpdated());
                    item.setHashTypes(kve.getTypes());
                    item.setMode(kve.getStorageMode());
                    item.setExpires(kve.getExpired());
                    itemsMap.put(item.getKid(), item);
                }
            }
        });

        RevocationListJsonEntity revocationListJsonEntity = new RevocationListJsonEntity();
        revocationListJsonEntity.setEtag(etag);
        revocationListJsonEntity.setJsonData(new ArrayList<>(itemsMap.values()));

        revocationListService.saveRevocationListJson(revocationListJsonEntity);

        log.info(itemsMap.values().toString());
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

    }

}
