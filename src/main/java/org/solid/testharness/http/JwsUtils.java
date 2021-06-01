package org.solid.testharness.http;

import org.jose4j.jwk.RsaJsonWebKey;
import org.jose4j.jws.AlgorithmIdentifiers;
import org.jose4j.jws.JsonWebSignature;
import org.jose4j.jwt.JwtClaims;
import org.jose4j.lang.JoseException;
import org.jose4j.lang.UncheckedJoseException;

import static org.jose4j.jwx.HeaderParameterNames.TYPE;

public final class JwsUtils {
    // TODO: Switch to elliptical curve as it is faster
    public static String generateDpopToken(final RsaJsonWebKey clientKey, final JwtClaims claims) {
        final JsonWebSignature jws = new JsonWebSignature();
//            jws.setAlgorithmHeaderValue(AlgorithmIdentifiers.ECDSA_USING_P256_CURVE_AND_SHA256);
        jws.setAlgorithmHeaderValue(AlgorithmIdentifiers.RSA_USING_SHA256);
        jws.setHeader(TYPE, "dpop+jwt");
        jws.setJwkHeader(clientKey);
        jws.setKey(clientKey.getPrivateKey());
        jws.setPayload(claims.toJson());
        try {
            return jws.getCompactSerialization();
        } catch (final JoseException ex) {
            throw new UncheckedJoseException("Unable to generate DPoP token", ex);
        }
    }

    private JwsUtils() { }
}
