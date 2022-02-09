package europa.ec.dgc.revocationdistribution.dto;

import java.util.List;
import lombok.Data;

@Data
public class RevocationCheckTokenPayload {
    private String sub;
    private List<String> payload;
    private long exp;
}
