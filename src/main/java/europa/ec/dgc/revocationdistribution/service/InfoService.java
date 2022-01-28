package europa.ec.dgc.revocationdistribution.service;


import europa.ec.dgc.revocationdistribution.entity.InfoEntity;
import europa.ec.dgc.revocationdistribution.entity.KidViewEntity;
import europa.ec.dgc.revocationdistribution.repository.InfoRepository;
import europa.ec.dgc.revocationdistribution.repository.KidViewRepository;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@RequiredArgsConstructor
@Service
public class InfoService {

    public final static String LAST_UPDATED_KEY = "LASTUPDATED";
    public final static String CURRENT_ETAG = "CURRENTETAG";

    private final InfoRepository infoRepository;

    public String getValueForKey(String key) {
        Optional<InfoEntity> optionalValue = infoRepository.findById(key);

        if(optionalValue.isPresent() && !optionalValue.isEmpty()) {
            return optionalValue.get().getValue();
        } else {
            return null;
        }
    }

    public void setValueForKey(String key, String value) {
        InfoEntity infoEntity = new InfoEntity(key, value);
        infoRepository.save(infoEntity);
    }

    public void setNewEtag(String etag) {
        infoRepository.setNewEtag(etag);
    }
}
