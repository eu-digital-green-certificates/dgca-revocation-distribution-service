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


import eu.europa.ec.dgc.gateway.connector.dto.RevocationBatchDto;
import eu.europa.ec.dgc.revocationdistribution.dto.PartitionResponseDto;
import eu.europa.ec.dgc.revocationdistribution.entity.BatchListEntity;
import eu.europa.ec.dgc.revocationdistribution.entity.HashesEntity;
import eu.europa.ec.dgc.revocationdistribution.entity.PartitionEntity;
import eu.europa.ec.dgc.revocationdistribution.entity.RevocationListJsonEntity;
import eu.europa.ec.dgc.revocationdistribution.entity.SliceEntity;
import eu.europa.ec.dgc.revocationdistribution.exception.DataNotChangedException;
import eu.europa.ec.dgc.revocationdistribution.exception.DataNotFoundException;
import eu.europa.ec.dgc.revocationdistribution.mapper.PartitionListMapper;
import eu.europa.ec.dgc.revocationdistribution.repository.BatchListRepository;
import eu.europa.ec.dgc.revocationdistribution.repository.HashesRepository;
import eu.europa.ec.dgc.revocationdistribution.repository.PartitionRepository;
import eu.europa.ec.dgc.revocationdistribution.repository.RevocationListJsonRepository;
import eu.europa.ec.dgc.revocationdistribution.repository.SliceRepository;
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


    /**
     * Updates the hashes of the batch in the database.
     *
     * @param batchId            The id of the batch processed.
     * @param revocationBatchDto The batch data of the processed batch.
     */
    @Transactional
    public void updateRevocationListBatch(String batchId, RevocationBatchDto revocationBatchDto) {

        BatchListEntity batchListEntity =  saveBatchList(batchId, revocationBatchDto);

        List<HashesEntity> hashes = new ArrayList<>();


        for (RevocationBatchDto.BatchEntryDto batchEntry : revocationBatchDto.getEntries()) {
            if (batchEntry.getHash() != null) {
                try {
                    hashes.add(getHashEntity(batchListEntity, batchEntry, revocationBatchDto.getKid()));
                } catch (IndexOutOfBoundsException e) {
                    log.error("Error calculating x,y,z. Hash value length is to short: {}",
                        batchEntry.getHash().getBytes(StandardCharsets.UTF_8).length);
                } catch (IllegalArgumentException e) {
                    log.error("Hash failed base64 decoding: {}", batchEntry.getHash());
                }
            } else {
                log.warn("Batch ({}) includes hash with null value, hash is ignored", batchId);
            }
        }
        hashesRepository.saveAll(hashes);
    }


    /**
     * Deletes alle batch entities in the given list from the DB.
     *
     * @param batchIds Ids of the Batch list items to be deleted
     */
    @Transactional
    public void deleteBatchListItemsByIds(List<String> batchIds) {
        batchListRepository.deleteByBatchIdIn(batchIds);
    }

    /**
     * Saves a  revocation list entity in the db.
     *
     * @param revocationListJsonEntity the entity to be saved
     */
    public void saveRevocationListJson(RevocationListJsonEntity revocationListJsonEntity) {
        revocationListJsonRepository.save(revocationListJsonEntity);
    }

    /**
     * Gets the revocation list meta data for an etag.
     *
     * @param currentEtag Etag for which the revocation list meta data should be returned
     * @return revocation list meta data if present
     */
    public Optional<RevocationListJsonEntity> getRevocationListJsonData(String currentEtag) {
        return revocationListJsonRepository.findById(currentEtag);
    }

    /**
     * Sets all hashes updated states to false.
     */
    @Transactional
    public void setAllHashesUpdatedStatesToFalse() {
        hashesRepository.setAllUpdatedStatesToFalse();
    }

    /**
     * Deletes all hashes without a batch id.
     */
    @Transactional
    public void deleteAllOrphanedHashes() {
        hashesRepository.deleteAllOrphanedHashes();
    }

    /**
     * Deletes all revocation lists meta data, except the one belonging to the given etag.
     *
     * @param currendEtag the etag of the data that should not be deleted.
     */
    @Transactional
    public void deleteAllOutdatedJsonLists(String currendEtag) {
        revocationListJsonRepository.deleteAllOutdatedEntries(currendEtag);
    }

    /**
     * Gets all partition metadata of a kid filtered by date.
     *
     * @param kid             the kid of the partition.
     * @param etag            the etag of the data set.
     * @param ifModifiedSince Only newer data should be returned.
     * @return the partition meta data
     * @throws DataNotFoundException thrown if no data was found
     * @throws DataNotChangedException thrown if no data changed after date
     */
    public List<PartitionResponseDto> getPartitionsByKidAndDate(
        String etag, String kid, ZonedDateTime ifModifiedSince) throws DataNotFoundException, DataNotChangedException {

        List<PartitionResponseDto> partitions =  partitionRepository.findAllByEtagAndKidAndLastUpdatedAfter(
            etag, kid, ifModifiedSince).stream().map(partitionListMapper::map).collect(Collectors.toList());

        if (partitions.isEmpty()) {
            //check if there is data at all. -> throws exception if not
            getPartitionsByKid(etag, kid);
            throw new DataNotChangedException();
        }

        return partitions;
    }

    /**
     * Gets all partition metadata of a kid.
     *
     * @param kid  the kid of the partition.
     * @param etag the etag of the data set.
     * @return the partition meta data
     * @throws DataNotFoundException thrown if no data was found
     */
    public List<PartitionResponseDto> getPartitionsByKid(String etag, String kid) throws DataNotFoundException {

        List<PartitionResponseDto> partitions = partitionRepository.findAllByEtagAndKid(etag, kid).stream()
            .map(partitionListMapper::map).collect(Collectors.toList());

        if (partitions.isEmpty()) {
            throw new DataNotFoundException();
        }

        return partitions;
    }

    /**
     * Gets a partition meta data.
     *
     * @param etag the etag of the data set.
     * @param kid  the kid of the partition.
     * @param id   the id of the partition
     * @return the partition meta data
     * @throws DataNotFoundException thrown if no data was found
     */
    public PartitionResponseDto getPartitionsByKidAndId(String etag, String kid, String id)
        throws DataNotFoundException {

        Optional<PartitionEntity> partition;

        if (id.equalsIgnoreCase("null")) {
            log.info("id is null");
            partition = partitionRepository.findOneByEtagAndKidAndIdIsNull(etag, kid);
        } else {
            partition = partitionRepository.findOneByEtagAndKidAndId(etag, kid, id);
        }

        if (!partition.isPresent()) {
            throw new DataNotFoundException();
        }
        return partitionListMapper.map(partition.get());
    }


    /**
     * Gets a partition meta data.
     *
     * @param etag the etag of the data set.
     * @param kid  the kid of the partition.
     * @param id   the id of the partition
     * @param ifModifiedSince only data after this dae are returned
     * @return the partition meta data
     * @throws DataNotFoundException thrown if no data was found
     * @throws DataNotChangedException thrown if no data changed after date
     */
    public PartitionResponseDto getPartitionsByKidAndIdAndDate(
        String etag, String kid, String id, ZonedDateTime ifModifiedSince)
        throws DataNotFoundException {

        Optional<PartitionEntity> partition;

        if (id.equalsIgnoreCase("null")) {
            log.info("id is null");
            partition =
                partitionRepository.findOneByEtagAndKidAndIdIsNullAndLastUpdatedAfter(etag, kid, ifModifiedSince);
        } else {
            partition =
                partitionRepository.findOneByEtagAndKidAndIdAndLastUpdatedAfter(etag, kid, id, ifModifiedSince);
        }

        if (partition.isEmpty()) {
            Long count;

            if (id.equalsIgnoreCase("null")) {
                count = partitionRepository.countByEtagAndKidAndIdIsNull(etag, kid);
            } else {
                count = partitionRepository.countByEtagAndKidAndId(etag, kid, id);
            }

            if (count == 0) {
                throw new DataNotFoundException();
            }
            throw new DataNotChangedException();
        }
        return partitionListMapper.map(partition.get());
    }



    /**
     * Gets all slice binary data of a partition.
     *
     * @param etag the etag of the data set.
     * @param kid  the kid of the partition.
     * @param id   the id of the partition
     * @return the partition binary slice data
     * @throws DataNotFoundException thrown if no data was found
     */
    public byte[] getAllChunkDataFromPartition(String etag, String kid, String id) throws DataNotFoundException {
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


    /**
     * Gets all slice binary data of a partition since a date.
     *
     * @param etag the etag of the data set.
     * @param kid  the kid of the partition.
     * @param id   the id of the partition
     * @param ifModifiedDateTime only data after this date are returned
     * @return the partition binary slice data
     * @throws DataNotFoundException thrown if no data was found
     */
    public byte[] getAllChunkDataFromPartitionSinceDate(
        String etag,
        String kid,
        String id,
        ZonedDateTime ifModifiedDateTime) throws DataNotFoundException, DataNotChangedException {

        List<SliceEntity> sliceEntityList;

        if (id.equalsIgnoreCase("null")) {
            log.info("id is null");
            sliceEntityList = sliceRepository.findAllByEtagAndKidAndIdIsNullAndLastUpdatedAfter(
                etag, kid, ifModifiedDateTime);

        } else {
            sliceEntityList = sliceRepository.findAllByEtagAndKidAndIdAndLastUpdatedAfter(
                etag, kid, id, ifModifiedDateTime);
        }

        if (sliceEntityList.isEmpty()) {
            Long count;

            if (id.equalsIgnoreCase("null")) {
                count = sliceRepository.countByEtagAndKidAndIdIsNull(etag, kid);
            } else {
                count = sliceRepository.countByEtagAndKidAndId(etag, kid, id);
            }

            if (count == 0) {
                throw new DataNotFoundException();
            }

            throw new DataNotChangedException();
        }

        return createTarForSlices(sliceEntityList);

    }


    /**
     * Gets all slice binary data of a partition with filter. Only the binary slice data of the slices,
     * which ids are part of the filter are returned.
     *
     * @param etag   the etag of the data set.
     * @param kid    the kid of the partition.
     * @param id     the id of the partition
     * @param filter only the slices, which ids are part of the filter are returned.
     * @return the partition binary slice data
     * @throws DataNotFoundException thrown if no data was found
     */
    public byte[] getAllChunkDataFromPartitionWithFilter(
        String etag,
        String kid,
        String id,
        List<String> filter) throws DataNotFoundException {

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

    /**
     * Gets all slice binary data of a partition with filter. Only the binary slice data of the slices,
     * which ids are part of the filter  and are newer then the modified since date are returned.
     *
     * @param etag   the etag of the data set.
     * @param kid    the kid of the partition.
     * @param id     the id of the partition
     * @param filter only the slices, which ids are part of the filter are returned.
     * @param ifModifiedDateTime only data after this date are returned
     * @return the partition binary slice data
     * @throws DataNotFoundException thrown if no data was found
     */

    public byte[] getAllChunkDataFromPartitionWithFilterSinceDate(
        String etag,
        String kid,
        String id,
        List<String> filter,
        ZonedDateTime ifModifiedDateTime) throws DataNotFoundException, DataNotChangedException {

        List<SliceEntity> sliceEntityList;
        if (id.equalsIgnoreCase("null")) {
            sliceEntityList = sliceRepository.findAllByEtagAndKidAndIdIsNullAndChunkInAndLastUpdatedAfter(
                etag, kid, filter, ifModifiedDateTime);

        } else {
            sliceEntityList = sliceRepository.findAllByEtagAndKidAndIdAndChunkInAndLastUpdatedAfter(
                etag, kid, id, filter, ifModifiedDateTime);
        }

        if (sliceEntityList.isEmpty()) {
            Long count;

            if (id.equalsIgnoreCase("null")) {
                count = sliceRepository.countByEtagAndKidAndIdIsNullAndChunkIn(etag, kid, filter);
            } else {
                count = sliceRepository.countByEtagAndKidAndIdAndChunkIn(etag, kid, id, filter);
            }

            if (count == 0) {
                throw new DataNotFoundException();
            }
            throw new DataNotChangedException();
        }

        return createTarForSlices(sliceEntityList);

    }

    /**
     * Gets all slice binary data of a chunk.
     *
     * @param etag the etag of the data set.
     * @param kid  the kid of the partition.
     * @param id   the id of the partition
     * @param cid  the id of the chunk
     * @return the chunk binary slice data
     * @throws DataNotFoundException thrown if no data was found
     */
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

    /**
     * Gets all slice binary data of a chunk. Only newer date since the modified since date are returned.
     *
     * @param etag the etag of the data set.
     * @param kid  the kid of the partition.
     * @param id   the id of the partition
     * @param cid  the id of the chunk
     * @param ifModifiedDateTime only data after this dae are returned
     * @return the chunk binary slice data
     * @throws DataNotFoundException thrown if no data was found
     */

    public byte[] getChunkDataSinceDate(
        String etag,
        String kid,
        String id,
        String cid,
        ZonedDateTime ifModifiedDateTime) throws DataNotFoundException, DataNotChangedException {

        List<SliceEntity> sliceEntityList;
        if (id.equalsIgnoreCase("null")) {
            log.info("id is null");
            sliceEntityList = sliceRepository.findAllByEtagAndKidAndIdIsNullAndChunkAndLastUpdatedAfter(
                etag, kid, cid, ifModifiedDateTime);

        } else {
            sliceEntityList = sliceRepository.findAllByEtagAndKidAndIdAndChunkAndLastUpdatedAfter(
                etag, kid, id, cid, ifModifiedDateTime);
        }

        if (sliceEntityList.isEmpty()) {
            Long count;

            if (id.equalsIgnoreCase("null")) {
                count = sliceRepository.countByEtagAndKidAndIdIsNullAndChunk(etag, kid, cid);
            } else {
                count = sliceRepository.countByEtagAndKidAndIdAndChunk(etag, kid, id, cid);
            }

            if (count == 0) {
                throw new DataNotFoundException();
            }
            throw new DataNotChangedException();
        }

        return createTarForSlices(sliceEntityList);

    }

    /**
     * Gets all slice binary data of a chunk with filter. Only the binary slice data of the slices,
     * which ids are part of the filter are returned.
     *
     * @param etag   the etag of the data set.
     * @param kid    the kid of the partition.
     * @param id     the id of the partition
     * @param cid    the id of the chunk
     * @param filter only the slices, which ids are part of the filter are returned.
     * @return the chunk binary slice data
     * @throws DataNotFoundException thrown if no data was found
     */
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

    /**
     * Gets all slice binary data of a chunk with filter. Only the binary slice data of the slices,
     * which ids are part of the filter are returned.
     *
     * @param etag   the etag of the data set.
     * @param kid    the kid of the partition.
     * @param id     the id of the partition
     * @param cid    the id of the chunk
     * @param filter only the slices, which ids are part of the filter are returned.
     * @param ifModifiedDateTime only data after this dae are returned
     * @return the chunk binary slice data
     * @throws DataNotFoundException thrown if no data was found
     */
    public byte[] getAllSliceDataForChunkWithFilterSinceDate(
        String etag,
        String kid,
        String id,
        String cid,
        List<String> filter,
        ZonedDateTime ifModifiedDateTime) throws DataNotFoundException, DataNotChangedException {

        List<SliceEntity> sliceEntityList;

        if (id.equalsIgnoreCase("null")) {
            log.info("id is null");
            sliceEntityList = sliceRepository.findAllByEtagAndKidAndIdIsNullAndChunkAndHashInAndLastUpdatedAfter(
                etag, kid, cid, filter, ifModifiedDateTime);

        } else {
            sliceEntityList = sliceRepository.findAllByEtagAndKidAndIdAndChunkAndHashInAndLastUpdatedAfter(
                etag, kid, id, cid, filter, ifModifiedDateTime);
        }

        if (sliceEntityList.isEmpty()) {
            Long count;

            if (id.equalsIgnoreCase("null")) {
                count = sliceRepository.countByEtagAndKidAndIdIsNullAndChunkAndHashIn(etag, kid, cid, filter);
            } else {
                count = sliceRepository.countByEtagAndKidAndIdAndChunkAndHashIn(etag, kid, id, cid, filter);
            }

            if (count == 0) {
                throw new DataNotFoundException();
            }

            throw new DataNotChangedException();
        }

        return createTarForSlices(sliceEntityList);
    }


    /**
     * Gets the slice binary data for a specific slice.
     *
     * @param etag the etag of the data set.
     * @param kid  the kid of the partition.
     * @param id   the id of the partition
     * @param cid  the id of the chunk
     * @param sid  the id of the slice
     * @return the chunk binary slice data
     * @throws DataNotFoundException thrown if no data was found
     */
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

    /**
     * Gets the slice binary data for a specific slice if the data is newer than the modified since date.
     *
     * @param etag the etag of the data set.
     * @param kid  the kid of the partition.
     * @param id   the id of the partition
     * @param cid  the id of the chunk
     * @param sid  the id of the slice
     * @param ifModifiedDateTime only data after this dae are returned
     * @return the chunk binary slice data
     * @throws DataNotFoundException thrown if no data was found
     */
    public byte[] getSliceDataSinceDate(
        String etag,
        String kid,
        String id,
        String cid,
        String sid,
        ZonedDateTime ifModifiedDateTime) throws DataNotFoundException, DataNotChangedException {

        Optional<SliceEntity> sliceEntity;
        if (id.equalsIgnoreCase("null")) {
            log.info("id is null");
            sliceEntity = sliceRepository.findOneByEtagAndKidAndIdIsNullAndChunkAndHashAndLastUpdatedAfter(
                etag, kid, cid, sid, ifModifiedDateTime);

        } else {
            sliceEntity = sliceRepository.findOneByEtagAndKidAndIdAndChunkAndHashAndLastUpdatedAfter(
                etag, kid, id, cid, sid, ifModifiedDateTime);
        }

        if (!sliceEntity.isPresent()) {
            Long count;

            if (id.equalsIgnoreCase("null")) {
                count = sliceRepository.countByEtagAndKidAndIdIsNullAndChunkAndHash(etag, kid, cid, sid);
            } else {
                count = sliceRepository.countByEtagAndKidAndIdAndChunkAndHash(etag, kid, id, cid, sid);
            }

            if (count == 0) {
                throw new DataNotFoundException();
            }
            throw new DataNotChangedException();
        }

        List<SliceEntity> sliceEntityList = new ArrayList<>();
        sliceEntityList.add(sliceEntity.get());

        return createTarForSlices(sliceEntityList);


    }


    /**
     * Gets all Batch ids that are expired from the DB.
     *
     * @return List of expired batches
     */
    public List<String> getExpiredBatchIds() {
        return batchListRepository.findAllByExpiresBefore(ZonedDateTime.now());
    }


    private String decodeBase64Hash(String b64Hash) {
        byte[] decodedBytes = Base64.getDecoder().decode(b64Hash);

        return Hex.toHexString(decodedBytes);
    }


    private BatchListEntity saveBatchList(String batchId, RevocationBatchDto revocationBatchDto) {
        BatchListEntity batchListEntity = new BatchListEntity();

        batchListEntity.setBatchId(batchId);
        batchListEntity.setExpires(revocationBatchDto.getExpires());
        batchListEntity.setCountry(revocationBatchDto.getCountry());
        batchListEntity.setType(BatchListEntity.RevocationHashType.valueOf(revocationBatchDto.getHashType().name()));
        batchListEntity.setKid(revocationBatchDto.getKid());

        return batchListRepository.save(batchListEntity);
    }


    private HashesEntity getHashEntity(BatchListEntity batch, RevocationBatchDto.BatchEntryDto hash, String kid)
        throws IndexOutOfBoundsException {

        String hexHash = decodeBase64Hash(hash.getHash());
        HashesEntity hashesEntity = new HashesEntity();
        hashesEntity.setHash(hexHash);
        hashesEntity.setX(hexHash.charAt(0));
        hashesEntity.setY(hexHash.charAt(1));
        hashesEntity.setZ(hexHash.charAt(2));
        hashesEntity.setKid(kid);
        hashesEntity.setBatch(batch);
        hashesEntity.setUpdated(true);

        return hashesEntity;
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
