/*
 * MIT License
 *
 * Copyright (c) 2019 - 2022 W3C Solid Community Group
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package org.solid.testharness.http;

import org.apache.commons.text.RandomStringGenerator;
import org.jose4j.jwk.RsaJsonWebKey;
import org.jose4j.jwk.RsaJwkGenerator;
import org.jose4j.jws.AlgorithmIdentifiers;
import org.jose4j.jws.JsonWebSignature;
import org.jose4j.jwt.JwtClaims;
import org.jose4j.lang.JoseException;
import org.jose4j.lang.UncheckedJoseException;
import org.solid.testharness.utils.TestHarnessInitializationException;

import static org.apache.commons.text.CharacterPredicates.DIGITS;
import static org.apache.commons.text.CharacterPredicates.LETTERS;
import static org.jose4j.jwx.HeaderParameterNames.TYPE;

public final class JwsUtils {
    private static final RandomStringGenerator GENERATOR = new RandomStringGenerator.Builder()
            .withinRange('0', 'z').filteredBy(LETTERS, DIGITS).get();

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

    public static RsaJsonWebKey createClientKey() {
        final RsaJsonWebKey clientKey;
        final String identifier = GENERATOR.generate(12);
        try {
            clientKey = RsaJwkGenerator.generateJwk(2048);
        } catch (JoseException e) {
            throw new TestHarnessInitializationException("Failed to set up DPoP support", e);
        }
        clientKey.setKeyId(identifier);
        clientKey.setUse("sig");
        clientKey.setAlgorithm("RS256");
        return clientKey;
    }

    private JwsUtils() { }
}
