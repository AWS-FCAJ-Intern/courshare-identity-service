package com.courshare.identity.infrastructure;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

public class RsaKeyHelper {

    private static final Logger logger = LoggerFactory.getLogger(RsaKeyHelper.class);

    public static KeyPair generateRsaKeyPair() {
        try {
            logger.warn("Generating temporary RSA 2048-bit key pair for development. DO NOT use this in production!");
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            return generator.generateKeyPair();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Failed to generate RSA key pair", e);
        }
    }

    public static RSAPrivateKey parsePrivateKey(String pem) {
        try {
            String privateKeyPem = pem
                    .replace("-----BEGIN PRIVATE KEY-----", "")
                    .replace("-----END PRIVATE KEY-----", "")
                    .replaceAll("\\s", "");
            byte[] encoded = Base64.getDecoder().decode(privateKeyPem);
            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(encoded);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            return (RSAPrivateKey) keyFactory.generatePrivate(keySpec);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new IllegalArgumentException("Invalid Private Key PEM format", e);
        }
    }

    public static RSAPublicKey parsePublicKey(String pem) {
        try {
            String publicKeyPem = pem
                    .replace("-----BEGIN PUBLIC KEY-----", "")
                    .replace("-----END PUBLIC KEY-----", "")
                    .replaceAll("\\s", "");
            byte[] encoded = Base64.getDecoder().decode(publicKeyPem);
            X509EncodedKeySpec keySpec = new X509EncodedKeySpec(encoded);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            return (RSAPublicKey) keyFactory.generatePublic(keySpec);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new IllegalArgumentException("Invalid Public Key PEM format", e);
        }
    }

    public static String readKeyFromFile(String path) {
        try {
            return new String(Files.readAllBytes(Paths.get(path)));
        } catch (IOException e) {
            throw new RuntimeException("Failed to read key file from path: " + path, e);
        }
    }

    public static Map<String, Object> toJwk(String keyId, RSAPublicKey publicKey) {
        Map<String, Object> jwk = new HashMap<>();
        jwk.put("kty", "RSA");
        jwk.put("use", "sig");
        jwk.put("alg", "RS256");
        jwk.put("kid", keyId);

        // Encode modulus n
        byte[] modulusBytes = publicKey.getModulus().toByteArray();
        if (modulusBytes.length > 0 && modulusBytes[0] == 0) {
            byte[] strippedBytes = new byte[modulusBytes.length - 1];
            System.arraycopy(modulusBytes, 1, strippedBytes, 0, strippedBytes.length);
            modulusBytes = strippedBytes;
        }
        String n = Base64.getUrlEncoder().withoutPadding().encodeToString(modulusBytes);
        jwk.put("n", n);

        // Encode exponent e
        byte[] exponentBytes = publicKey.getPublicExponent().toByteArray();
        if (exponentBytes.length > 0 && exponentBytes[0] == 0) {
            byte[] strippedBytes = new byte[exponentBytes.length - 1];
            System.arraycopy(exponentBytes, 1, strippedBytes, 0, strippedBytes.length);
            exponentBytes = strippedBytes;
        }
        String e = Base64.getUrlEncoder().withoutPadding().encodeToString(exponentBytes);
        jwk.put("e", e);

        return jwk;
    }
}
