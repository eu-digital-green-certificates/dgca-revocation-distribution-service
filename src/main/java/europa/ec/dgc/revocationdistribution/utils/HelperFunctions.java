package europa.ec.dgc.revocationdistribution.utils;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class HelperFunctions {

    private final DateTimeFormatter dateTimeFormatter =
        DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX");


    public String getDateTimeString(ZonedDateTime dateTime){
        return dateTimeFormatter.withZone(ZoneId.of("UTC")).format(dateTime);
    }
}
