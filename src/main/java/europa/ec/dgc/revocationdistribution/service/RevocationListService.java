package europa.ec.dgc.revocationdistribution.service;


import eu.europa.ec.dgc.gateway.connector.dto.RevocationBatchDto;
import europa.ec.dgc.revocationdistribution.entity.BatchListEntity;
import europa.ec.dgc.revocationdistribution.entity.HashesEntity;
import europa.ec.dgc.revocationdistribution.repository.BatchListRepository;
import europa.ec.dgc.revocationdistribution.repository.HashesRepository;
import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@RequiredArgsConstructor
@Service
public class RevocationListService {

    private final BatchListRepository batchListRepository;
    private final HashesRepository hashesRepository;

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
            HashesEntity hashesEntity = new HashesEntity();
            hashesEntity.setHash(hash.getHash());
            hashesEntity.setX(hash.getHash().charAt(0));
            hashesEntity.setY(hash.getHash().charAt(1));
            hashesEntity.setZ(hash.getHash().charAt(2));
            hashesEntity.setKid(kid);
            hashesEntity.setBatchId(batchId);
            hashesEntity.setUpdated(true);

            hashesRepository.save(hashesEntity);

        } catch (IndexOutOfBoundsException e) {
            log.error("Error calculating x,y,z. Hash value length is to short: {}",
                hash.getHash().getBytes(StandardCharsets.UTF_8).length);
        }
    }

}
