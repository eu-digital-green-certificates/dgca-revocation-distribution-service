package europa.ec.dgc.revocationdistribution.service;


import eu.europa.ec.dgc.gateway.connector.dto.RevocationBatchDto;
import europa.ec.dgc.revocationdistribution.entity.BatchListEntity;
import europa.ec.dgc.revocationdistribution.entity.HashesEntity;
import europa.ec.dgc.revocationdistribution.entity.PartitionEntity;
import europa.ec.dgc.revocationdistribution.entity.RevocationListJsonEntity;
import europa.ec.dgc.revocationdistribution.repository.BatchListRepository;
import europa.ec.dgc.revocationdistribution.repository.HashesRepository;
import europa.ec.dgc.revocationdistribution.repository.PartitionRepository;
import europa.ec.dgc.revocationdistribution.repository.RevocationListJsonRepository;
import java.nio.charset.StandardCharsets;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.util.encoders.Hex;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@RequiredArgsConstructor
@Service
public class RevocationListService {

    private final BatchListRepository batchListRepository;
    private final HashesRepository hashesRepository;
    private final RevocationListJsonRepository revocationListJsonRepository;
    private final PartitionRepository partitionRepository;

    @Transactional
    public void updateRevocationListBatch(String batchId, RevocationBatchDto revocationBatchDto) {

        saveBatchList(batchId, revocationBatchDto);

        for(RevocationBatchDto.BatchEntryDto hash : revocationBatchDto.getEntries()) {
            saveHash(batchId, hash, revocationBatchDto.getKid());
        }

    }

    @Transactional
    private void saveBatchList(String batchId, RevocationBatchDto revocationBatchDto) {
        BatchListEntity batchListEntity = new BatchListEntity();

        batchListEntity.setBatchId(batchId);
        batchListEntity.setExpires(revocationBatchDto.getExpires());
        batchListEntity.setCountry(revocationBatchDto.getCountry());
        batchListEntity.setType(BatchListEntity.RevocationHashType.valueOf(revocationBatchDto.getHashType().name()));
        batchListEntity.setKid(revocationBatchDto.getKid());

        batchListRepository.save(batchListEntity);
    }

    @Transactional
    private void saveHash(String batchId, RevocationBatchDto.BatchEntryDto hash, String kid){
        try {
            String hexHash = decodeBase64Hash(hash.getHash());
            HashesEntity hashesEntity = new HashesEntity();
            hashesEntity.setHash(hexHash);
            hashesEntity.setX(hexHash.charAt(0));
            hashesEntity.setY(hexHash.charAt(1));
            hashesEntity.setZ(hexHash.charAt(2));
            hashesEntity.setKid(kid);
            hashesEntity.setBatchId(batchId);
            hashesEntity.setUpdated(true);

            hashesRepository.save(hashesEntity);

        } catch (IndexOutOfBoundsException e) {
            log.error("Error calculating x,y,z. Hash value length is to short: {}",
                hash.getHash().getBytes(StandardCharsets.UTF_8).length);
        }
    }

    @Transactional
    public void deleteBatchListItemsByIds(List<String> batchIds) {
        batchListRepository.deleteByBatchIdIn(batchIds);
    }

    private String decodeBase64Hash(String b64Hash) {
        byte[] decodedBytes = Base64.getDecoder().decode(b64Hash);

        return Hex.toHexString(decodedBytes);
    }

    public void saveRevocationListJson(RevocationListJsonEntity revocationListJsonEntity) {
        revocationListJsonRepository.save(revocationListJsonEntity);
    }

    public Optional<RevocationListJsonEntity> getRevocationListJsonData(String currentEtag) {
        return revocationListJsonRepository.findById(currentEtag);
    }

    @Transactional
    public void setAllHashesUpdatedStatesToFalse() {
        hashesRepository.setAllUpdatedStatesToFalse();
    }

    @Transactional
    public void deleteAllOrphanedHashes(){
        hashesRepository.deleteAllOrphanedHashes();
    }


    public List<PartitionEntity> getPartitionsByKidAndDate(String kidId, ZonedDateTime ifModifiedSince){
        return new ArrayList<>();
    }


    public List<PartitionEntity> getPartitionsByKid(String kid) {
      return  partitionRepository.findAllByKid(kid);
    }
}
