package br.simplipark.util;

import lombok.extern.slf4j.Slf4j;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import java.util.Base64;

@Slf4j
public class Cryptographer {

    private static final String ALGORITHM = "AES";
    private static final SecretKey DEFAULT_SECRET_KEY;

    static {
        try {
            log.info("Initializing the default secret key.");
            DEFAULT_SECRET_KEY = generateSecretKey();
            log.info("Default secret key successfully initialized.");
        } catch (Exception e) {
            throw new RuntimeException("Error generating default secret key.", e);
        }
    }

    public static SecretKey generateSecretKey() throws Exception {
        log.info("Generating a new secret key using the algorithm: {}", ALGORITHM);
        KeyGenerator keyGen = KeyGenerator.getInstance(ALGORITHM);
        keyGen.init(128);
        SecretKey secretKey = keyGen.generateKey();
        log.debug("Secret key generated successfully.");
        return secretKey;
    }

    public static String encryptUrlSafe(String data) throws Exception {
        log.debug("Encrypting data using the default secret key.");
        return encryptUrlSafe(data, DEFAULT_SECRET_KEY);
    }

    public static String decrypt(String encryptedData) throws Exception {
        log.debug("Decrypting data using the default secret key.");
        return decrypt(encryptedData, DEFAULT_SECRET_KEY);
    }

    public static String encryptUrlSafe(String data, SecretKey secretKey) throws Exception {
        log.debug("Encrypting data: {}", data);
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.ENCRYPT_MODE, secretKey);
        byte[] encryptedData = cipher.doFinal(data.getBytes());
        String encodedData = Base64.getUrlEncoder().withoutPadding().encodeToString(encryptedData);
        log.info("Data encrypted and URL-safe encoded successfully.");
        log.debug("Encrypted and encoded data: {}", encodedData);
        return encodedData;
    }

    public static String decrypt(String encryptedData, SecretKey secretKey) throws Exception {
        log.debug("Decrypting encrypted data: {}", encryptedData);
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.DECRYPT_MODE, secretKey);
        byte[] decryptedData = cipher.doFinal(Base64.getUrlDecoder().decode(encryptedData));
        String decodedData = new String(decryptedData);
        log.info("Data decrypted successfully.");
        log.debug("Decrypted data: {}", decodedData);
        return decodedData;
    }

    public static void main(String[] args) throws Exception {
        String originalData = "This is a secret message";

        log.info("Starting encryption and decryption process in main.");

        SecretKey secretKey = generateSecretKey();

        String encryptedData = encryptUrlSafe(originalData, secretKey);
        log.info("Encrypted Data: {}", encryptedData);

        String decryptedData = decrypt(encryptedData, secretKey);
        log.info("Decrypted Data: {}", decryptedData);

        log.info("Encryption and decryption process completed.");
    }
}
