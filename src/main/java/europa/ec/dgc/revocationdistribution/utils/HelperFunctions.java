package europa.ec.dgc.revocationdistribution.utils;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.util.encoders.Hex;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class HelperFunctions {

    private final DateTimeFormatter dateTimeFormatter =
        DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX");


    public String getDateTimeString(ZonedDateTime dateTime){
        return dateTimeFormatter.withZone(ZoneId.of("UTC")).format(dateTime);
    }

    /**
     * Calculates SHA-256 hash of a given Byte-Array.
     *
     * @param data data to hash.
     * @return HEX-String with the hash of the data.
     */
    public String calculateHash(byte[] data) throws NoSuchAlgorithmException {
        byte[] certHashBytes = MessageDigest.getInstance("SHA-256").digest(data);
        return Hex.toHexString(certHashBytes);
    }

    /**
     * Gets the byte array from a hex representation string.
     * @param hex
     * @return the byte data.
     */
    public byte[] getBytesFromHexString(String hex) {
       return Hex.decode(hex);
    }

}
