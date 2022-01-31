package europa.ec.dgc.revocationdistribution.service;

import europa.ec.dgc.revocationdistribution.dto.SliceDataDto;

public interface SliceCalculationService {

    public SliceDataDto calculateChunk(String[] hashes);

}
