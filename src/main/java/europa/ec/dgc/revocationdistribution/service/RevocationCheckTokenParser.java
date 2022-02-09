package europa.ec.dgc.revocationdistribution.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import europa.ec.dgc.revocationdistribution.dto.RevocationCheckTokenPayload;
import europa.ec.dgc.revocationdistribution.exception.TokenValidationException;
import io.jsonwebtoken.Jwt;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureException;
import java.security.PublicKey;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class RevocationCheckTokenParser {
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * parse Token.
     * @param jwtCompact jwtCompact
     * @param publicKey publicKey
     * @return RevocationCheckTokenPayload
     */
    public RevocationCheckTokenPayload parseToken(String jwtCompact, PublicKey publicKey) {
        try {
            Jwt token = Jwts.parser().setSigningKey(publicKey).parse(jwtCompact);

            String payloadJson = objectMapper.writeValueAsString(token.getBody());
            return objectMapper.readValue(payloadJson, RevocationCheckTokenPayload.class);
        } catch (JsonProcessingException e) {
            throw new TokenValidationException("Failed to parse revocation check token",
                HttpStatus.BAD_REQUEST.value());
        } catch (SignatureException e) {
            throw new TokenValidationException("Signature check failed for revocation check token",
                HttpStatus.BAD_REQUEST.value());
        }
    }

    /**
     * extract payload.
     * @param jwtCompact jwtCompact
     * @return JWT
     */
    public Jwt extractPayload(String jwtCompact) {
        String[] splitToken = jwtCompact.split("\\.");
        String unsignedToken = splitToken[0] + "." + splitToken[1] + ".";
        return Jwts.parser().parse(unsignedToken);
    }
}
