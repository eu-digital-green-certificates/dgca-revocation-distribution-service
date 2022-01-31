package europa.ec.dgc.revocationdistribution.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.ZonedDateTime;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Value;

@Value
@AllArgsConstructor
public class PartitionResponseDto {

    String kid;

    String id;

    String x;

    String y;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ssXXX", timezone = "UTC")
    ZonedDateTime lastUpdated;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ssXXX", timezone = "UTC")
     ZonedDateTime expired;

     Map<String, Map<String,PartitionChunksJsonItemDto>> chunks;

}
