package europa.ec.dgc.revocationdistribution.dto;

import java.time.ZonedDateTime;
import lombok.AllArgsConstructor;
import lombok.Value;


@Value
@AllArgsConstructor
public  class ChunkMetaViewDto {

    String rowId;

    String kid;

    String[] hashes;

    String id;

    String x;

    String y;

    String chunk;

    ZonedDateTime lastUpdated;

    ZonedDateTime expired;

}
