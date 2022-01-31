package europa.ec.dgc.revocationdistribution.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SliceDataDto {
    private PartitionChunksJsonItemDto metaData = new PartitionChunksJsonItemDto();
    private byte[] binaryData;

}
