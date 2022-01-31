package europa.ec.dgc.revocationdistribution.service;

import europa.ec.dgc.bloomfilter.BloomFilter;
import europa.ec.dgc.bloomfilter.BloomFilterImpl;
import europa.ec.dgc.revocationdistribution.config.DgcConfigProperties;
import europa.ec.dgc.revocationdistribution.dto.SliceDataDto;
import europa.ec.dgc.revocationdistribution.utils.HelperFunctions;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.util.encoders.DecoderException;
import org.springframework.stereotype.Service;


@Slf4j
@RequiredArgsConstructor
@Service
public class SliceCalculationServiceBloomFilterImpl implements SliceCalculationService {

    private final DgcConfigProperties properties;
    private final HelperFunctions helperFunctions;

    @Override
    public SliceDataDto calculateChunk(String[] hashes) {
        if (hashes.length <= 0)
            return null;

        BloomFilter bloomFilter = new BloomFilterImpl(hashes.length,properties.getBloomFilter().getProbRate());

        SliceDataDto sliceDataDto = new SliceDataDto();

        sliceDataDto.getMetaData().setType(properties.getBloomFilter().getType());
        sliceDataDto.getMetaData().setVersion(properties.getBloomFilter().getVersion());

        for (String hash : hashes) {
            try {
                byte[] hashBytes = helperFunctions.getBytesFromHexString(hash);
                bloomFilter.add(hashBytes);
            } catch (NoSuchAlgorithmException | IOException | DecoderException e) {
                log.error("Could not add hash to bloom filter: {}",hash);
            }
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        try {
            bloomFilter.writeTo(baos);
        } catch (IOException e){
            log.error("Could not get bloom filter binary data.");
            return null;
        }

        sliceDataDto.setBinaryData(baos.toByteArray());

        try {
            sliceDataDto.getMetaData().setHash(helperFunctions.calculateHash(sliceDataDto.getBinaryData()));
        } catch (NoSuchAlgorithmException e){
            log.error("Could calculate hash for binary data.");
            return null;
        }

        return sliceDataDto;
    }


}
