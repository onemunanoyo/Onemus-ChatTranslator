package onemunanoyo.translatormod.config.encryption;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.Arrays;

public final class AesGcmSecretCipher {
    private static final String KEY_DERIVATION_CONTEXT = "ChatTranslator API key v1";
    private static final String CIPHER_ALGORITHM = "AES/GCM/NoPadding";
    private static final int KEY_LENGTH_BITS = 256;
    private static final int TAG_LENGTH_BITS = 128;
    private static final int PBKDF2_ITERATIONS = 120_000;

    private AesGcmSecretCipher() {}

    public static byte[] encrypt(String secret, String deviceId, byte[] salt, byte[] iv)
            throws GeneralSecurityException {
        Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
        cipher.init(Cipher.ENCRYPT_MODE, deriveKey(deviceId, salt), new GCMParameterSpec(TAG_LENGTH_BITS, iv));
        cipher.updateAAD(KEY_DERIVATION_CONTEXT.getBytes(StandardCharsets.UTF_8));
        return cipher.doFinal(secret.getBytes(StandardCharsets.UTF_8));
    }

    public static String decrypt(byte[] encrypted, String deviceId, byte[] salt, byte[] iv)
            throws GeneralSecurityException {
        Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
        cipher.init(Cipher.DECRYPT_MODE, deriveKey(deviceId, salt), new GCMParameterSpec(TAG_LENGTH_BITS, iv));
        cipher.updateAAD(KEY_DERIVATION_CONTEXT.getBytes(StandardCharsets.UTF_8));
        return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
    }

    private static SecretKey deriveKey(String deviceId, byte[] salt) throws GeneralSecurityException {
        char[] password = (KEY_DERIVATION_CONTEXT + "\0" + deviceId).toCharArray();
        PBEKeySpec spec = new PBEKeySpec(password, salt, PBKDF2_ITERATIONS, KEY_LENGTH_BITS);
        Arrays.fill(password, '\0');
        try {
            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            byte[] encoded = factory.generateSecret(spec).getEncoded();
            return new SecretKeySpec(encoded, "AES");
        } finally {
            spec.clearPassword();
        }
    }
}

