package OfbizSpring.Util;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.stream.Collectors;

public class JwtHelper {
        private final String secret = "Some#8#Secret";

    public Map<String, String> getTokenUser(String token) {
        try {
            DecodedJWT decodedJWT = JWT.decode(token);
            Algorithm.HMAC256(secret).verify(decodedJWT);
            return decodedJWT.getClaims().entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().as(String.class)));
        } catch (Exception e) {
            return null;
        }
//        return JWT.require(Algorithm.HMAC256(secret)).build().verify(token).getClaims().toString();
    }

    public String getUserToken(Map<String, String> user) {
        return JWT.create()
                .withClaim("exp", LocalDateTime.now().plusDays(7).toEpochSecond(ZoneOffset.UTC))
                .withClaim("id", user.get("id"))
                .sign(Algorithm.HMAC256(secret));
    }
}
