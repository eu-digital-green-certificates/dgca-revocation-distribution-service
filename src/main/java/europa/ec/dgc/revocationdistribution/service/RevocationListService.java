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

package europa.ec.dgc.revocationdistribution.service;


import eu.europa.ec.dgc.gateway.connector.dto.RevocationBatchDto;
import europa.ec.dgc.revocationdistribution.dto.PartitionResponseDto;
import europa.ec.dgc.revocationdistribution.entity.BatchListEntity;
import europa.ec.dgc.revocationdistribution.entity.HashesEntity;
import europa.ec.dgc.revocationdistribution.entity.PartitionEntity;
import europa.ec.dgc.revocationdistribution.entity.RevocationListJsonEntity;
import europa.ec.dgc.revocationdistribution.entity.SliceEntity;
import europa.ec.dgc.revocationdistribution.exception.DataNotFoundException;
import europa.ec.dgc.revocationdistribution.mapper.PartitionListMapper;
import europa.ec.dgc.revocationdistribution.repository.BatchListRepository;
import europa.ec.dgc.revocationdistribution.repository.HashesRepository;
import europa.ec.dgc.revocationdistribution.repository.PartitionRepository;
import europa.ec.dgc.revocationdistribution.repository.RevocationListJsonRepository;
import europa.ec.dgc.revocationdistribution.repository.SliceRepository;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.zip.GZIPOutputStream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
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
    private final PartitionListMapper partitionListMapper;
    private final SliceRepository sliceRepository;

    @Transactional
    public void updateRevocationListBatch(String batchId, RevocationBatchDto revocationBatchDto) {

        saveBatchList(batchId, revocationBatchDto);

        for (RevocationBatchDto.BatchEntryDto hash : revocationBatchDto.getEntries()) {
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
    private void saveHash(String batchId, RevocationBatchDto.BatchEntryDto hash, String kid) {
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
    public void deleteAllOrphanedHashes() {
        hashesRepository.deleteAllOrphanedHashes();
    }

    @Transactional
    public void deleteAllOutdatedJsonLists(String currendEtag) {
        revocationListJsonRepository.deleteAllOutdatedEntries(currendEtag);
    }


    public List<PartitionResponseDto> getPartitionsByKidAndDate(String kidId, ZonedDateTime ifModifiedSince) {
        return new ArrayList<>();
    }


    public List<PartitionResponseDto> getPartitionsByKid(String etag, String kid) {

        return partitionRepository.findAllByEtagAndKid(etag, kid).stream().map(partitionListMapper::map).collect(Collectors.toList());
    }



    public PartitionResponseDto getPartitionsByKidAndId(String etag, String kid, String id) throws DataNotFoundException {

        Optional<PartitionEntity> partition;

        if (id.equalsIgnoreCase("null")) {
            log.info("id is null");
            partition = partitionRepository.findOneByEtagAndKidAndIdIsNull(etag, kid);
        } else {
            partition = partitionRepository.findOneByEtagAndKidAndId(etag, kid, id);
        }

        if(!partition.isPresent()) {
            throw new DataNotFoundException();
        }
        return partitionListMapper.map(partition.get());
    }


    public byte[] getAllChunkDataFromPartition(String etag, String kid, String id) {
        List<SliceEntity> sliceEntityList;
        if (id.equalsIgnoreCase("null")) {
            log.info("id is null");
            sliceEntityList = sliceRepository.findAllByEtagAndKidAndIdIsNull(etag, kid);

        } else {
            sliceEntityList = sliceRepository.findAllByEtagAndKidAndId(etag, kid, id);
        }

        if (sliceEntityList.isEmpty()) {
            throw new DataNotFoundException();
        }

        return createTarForSlices(sliceEntityList);
    }

    public byte[] getAllChunkDataFromPartitionWithFilter(
        String etag,
        String kid,
        String id,
        List<String> filter) {

        List<SliceEntity> sliceEntityList;
        if (id.equalsIgnoreCase("null")) {
            sliceEntityList = sliceRepository.findAllByEtagAndKidAndIdIsNullAndChunkIn(etag, kid, filter);

        } else {
            sliceEntityList = sliceRepository.findAllByEtagAndKidAndIdAndChunkIn(etag, kid, id, filter);
        }

        if (sliceEntityList.isEmpty()) {
            throw new DataNotFoundException();
        }

        return createTarForSlices(sliceEntityList);

    }


    public byte[] getChunkData(String etag, String kid, String id, String cid) throws DataNotFoundException {
        List<SliceEntity> sliceEntityList;
        if (id.equalsIgnoreCase("null")) {
            log.info("id is null");
            sliceEntityList = sliceRepository.findAllByEtagAndKidAndIdIsNullAndChunk(etag, kid, cid);

        } else {
            sliceEntityList = sliceRepository.findAllByEtagAndKidAndIdAndChunk(etag, kid, id, cid);
        }

        if (sliceEntityList.isEmpty()) {
            throw new DataNotFoundException();
        }

        return createTarForSlices(sliceEntityList);
    }


    public byte[] getAllSliceDataForChunkWithFilter(
        String etag,
        String kid,
        String id,
        String cid,
        List<String> filter) throws DataNotFoundException {

        List<SliceEntity> sliceEntityList;
        if (id.equalsIgnoreCase("null")) {
            log.info("id is null");
            sliceEntityList = sliceRepository.findAllByEtagAndKidAndIdIsNullAndChunkAndHashIn(etag, kid, cid, filter);

        } else {
            sliceEntityList = sliceRepository.findAllByEtagAndKidAndIdAndChunkAndHashIn(etag, kid, id, cid, filter);
        }

        if (sliceEntityList.isEmpty()) {
            throw new DataNotFoundException();
        }

        return createTarForSlices(sliceEntityList);
    }


    public byte[] getSliceData(String etag, String kid, String id, String cid, String sid)
        throws DataNotFoundException {

        Optional<SliceEntity> sliceEntity;
        if (id.equalsIgnoreCase("null")) {
            log.info("id is null");
            sliceEntity = sliceRepository.findOneByEtagAndKidAndIdIsNullAndChunkAndHash(etag, kid, cid, sid);

        } else {
            sliceEntity = sliceRepository.findOneByEtagAndKidAndIdAndChunkAndHash(etag, kid, id, cid, sid);
        }

        if (!sliceEntity.isPresent()) {
            throw new DataNotFoundException();
        }

        List<SliceEntity> sliceEntityList = new ArrayList<>();
        sliceEntityList.add(sliceEntity.get());

        return createTarForSlices(sliceEntityList);
    }


    private byte[] createTarForSlices(List<SliceEntity> sliceEntityList) {


        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        try {
            GZIPOutputStream gzipOutputStream = new GZIPOutputStream(byteArrayOutputStream);
            TarArchiveOutputStream outTar = new TarArchiveOutputStream(gzipOutputStream);

            for (SliceEntity sliceEntity : sliceEntityList) {
                String archiveEntryName = String.format("%s/%s/%s/%s",
                    sliceEntity.getKid(),
                    sliceEntity.getId(),
                    sliceEntity.getChunk(),
                    sliceEntity.getHash());

                TarArchiveEntry tarArchiveEntry = new TarArchiveEntry(archiveEntryName);
                tarArchiveEntry.setSize(sliceEntity.getBinaryData().length);

                outTar.putArchiveEntry(tarArchiveEntry);
                outTar.write(sliceEntity.getBinaryData());
                outTar.closeArchiveEntry();
            }

            gzipOutputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return byteArrayOutputStream.toByteArray();

    }



}
