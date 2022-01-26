package europa.ec.dgc.revocationdistribution.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.vladmihalcea.hibernate.type.json.JsonBinaryType;
import java.time.ZonedDateTime;
import java.util.Map;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.Value;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;

@Value
@AllArgsConstructor
public class PartitionResponseDto {

    String kid;

    String id;

    String x;

    String y;

    String z;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ssXXX", timezone = "UTC")
    ZonedDateTime lastUpdated;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ssXXX", timezone = "UTC")
     ZonedDateTime expired;

     Map<String, Map<String,PartitionChunksJsonItemDto>> chunks;

}
