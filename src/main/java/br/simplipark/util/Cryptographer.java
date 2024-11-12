package br.simplipark.util;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import lombok.extern.slf4j.Slf4j;

import java.util.Base64;

@Slf4j
public class Cryptographer {
    private static final String ALGORITHM = "AES";
    private static final SecretKey DEFAULT_SECRET_KEY;

    static {
        try {
            DEFAULT_SECRET_KEY = generateSecretKey();
        } catch (Exception e) {
            throw new RuntimeException("Error generating secret key.", e);
        }
    }

    public static String encryptUrlSafe(String data) throws Exception {
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.ENCRYPT_MODE, DEFAULT_SECRET_KEY);
        byte[] encryptedData = cipher.doFinal(data.getBytes());

        log.info("Data encrypted and URL-safe encoded successfully.");
        return Base64.getUrlEncoder().withoutPadding().encodeToString(encryptedData);
    }

    public static String decrypt(String encryptedData) throws Exception {
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.DECRYPT_MODE, DEFAULT_SECRET_KEY);
        byte[] decryptedData = cipher.doFinal(Base64.getUrlDecoder().decode(encryptedData));

        log.info("Data decrypted successfully.");

        return new String(decryptedData);
    }

    private static SecretKey generateSecretKey() {
        byte[] decodedKey = Base64.getDecoder().decode("JPR/5EQrwO9/p+73pD1IFpWlJdXvR+UpghGRk25W26g=");
        return new SecretKeySpec(decodedKey, ALGORITHM);
    }
}