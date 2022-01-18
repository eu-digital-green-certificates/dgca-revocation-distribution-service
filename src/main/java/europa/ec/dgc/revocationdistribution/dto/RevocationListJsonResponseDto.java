package europa.ec.dgc.revocationdistribution.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.io.Serializable;
import java.time.ZonedDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
public class RevocationListJsonResponseDto implements Serializable{

    List<RevocationListJsonResponseItemDto> items;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class RevocationListJsonResponseItemDto implements Serializable {

        private String kid;

        private String mode;

        private List<String> hashTypes;

        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ssXXX", timezone = "UTC")
        private ZonedDateTime expires;

        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ssXXX", timezone = "UTC")
        private ZonedDateTime lastUpdated;
    }

}
