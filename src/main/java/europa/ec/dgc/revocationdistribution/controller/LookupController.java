package europa.ec.dgc.revocationdistribution.controller;

import europa.ec.dgc.revocationdistribution.dto.RevocationCheckTokenPayload;
import europa.ec.dgc.revocationdistribution.dto.RevocationListJsonResponseDto;
import europa.ec.dgc.revocationdistribution.service.LookupService;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.List;
import javax.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/revocation/lookup")
@Slf4j
@RequiredArgsConstructor
public class LookupController {

    private final LookupService lookupService;

    /**
     * Http Method for getting the revocation list.
     * @return
     */
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<String>> lockupRevocation(
        @Valid @RequestBody(required = false)  List<String> revocationCheckTokenList
    ){
        if (revocationCheckTokenList.isEmpty()) {
            return ResponseEntity.ok(new ArrayList<>());
        }

        List<RevocationCheckTokenPayload> tokenPayloads =
            lookupService.validateRevocationCheckTokens(revocationCheckTokenList);

        List<String> result = lookupService.checkForRevocation(tokenPayloads);
        return ResponseEntity.ok(result);
    }

    @GetMapping(path= "/key", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> getKey() {
        PublicKey result;
        String hash = "Pu3LWoDPQv3lH53fcYmCOb12mHPd354tAXdWJDQns1U%3d";
        result = lookupService.downloadPublicKey(hash);
        return ResponseEntity.ok(result.toString());
    }


}

