package at.sleazlee.bmvelocity.crypto;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.util.Base64;

public class AESEncryptor {
    private static final String CIPHER = "AES/GCM/NoPadding";
    private static final int IV_LENGTH = 12;         // 96‑bit IV
    private static final int TAG_LENGTH = 128;       // 128‑bit auth tag

    private final SecretKeySpec keySpec;
    private final SecureRandom random = new SecureRandom();

    public AESEncryptor(byte[] key) {
        if (key.length != 32) throw new IllegalArgumentException("Key must be 256‑bit");
        this.keySpec = new SecretKeySpec(key, "AES");
    }

    /** Load Base64 key from a file */
    public static AESEncryptor fromKeyFile(Path keyFile) throws Exception {
        String b64 = Files.readString(keyFile).trim();
        byte[] key = Base64.getDecoder().decode(b64);
        return new AESEncryptor(key);
    }

    /** Encrypts and prepends IV */
    public byte[] encrypt(byte[] plain) throws Exception {
        byte[] iv = new byte[IV_LENGTH];
        random.nextBytes(iv);

        Cipher cipher = Cipher.getInstance(CIPHER);
        cipher.init(Cipher.ENCRYPT_MODE, keySpec, new GCMParameterSpec(TAG_LENGTH, iv));
        byte[] ct = cipher.doFinal(plain);

        ByteBuffer buf = ByteBuffer.allocate(iv.length + ct.length);
        buf.put(iv).put(ct);
        return buf.array();
    }

    /** Splits out IV, authenticates & decrypts */
    public byte[] decrypt(byte[] ivAndCt) throws Exception {
        ByteBuffer buf = ByteBuffer.wrap(ivAndCt);
        byte[] iv = new byte[IV_LENGTH];
        buf.get(iv);
        byte[] ct = new byte[buf.remaining()];
        buf.get(ct);

        Cipher cipher = Cipher.getInstance(CIPHER);
        cipher.init(Cipher.DECRYPT_MODE, keySpec, new GCMParameterSpec(TAG_LENGTH, iv));
        return cipher.doFinal(ct);
    }
}
