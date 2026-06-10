/* Copyright (c) 2026 Matthias Bläsing, All Rights Reserved
 *
 * The contents of this file is dual-licensed under 2
 * alternative Open Source/Free licenses: LGPL 2.1 or later and
 * Apache License 2.0.
 *
 * You can freely decide which license you want to apply to
 * the project.
 *
 * You may obtain a copy of the LGPL License at:
 *
 * http://www.gnu.org/licenses/licenses.html
 *
 * A copy is also included in the downloadable source code package
 * containing JNA, in file "LGPL2.1".
 *
 * You may obtain a copy of the Apache License at:
 *
 * http://www.apache.org/licenses/
 */

package eu.doppelhelix.govee;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Objects;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;

/**
 * The encryption scheme mixed AES/ECB mode with RC4. The assumption is that
 * there are always 20 bytes to be encrypted/decrypted. The first 16 bytes are
 * processed by AES, the remaining 4 bytes are processed by RC4.
 *
 * @see <a href="https://github.com/NHaag87/govee-api/blob/main/API_documentation/H5105_protocol.md">H5105 Protocol</a>
 */
public class EncryptionHelper {

    private final byte[] key;

    public EncryptionHelper(byte[] key) {
        this.key = key;
    }

    public byte[] encrypt(byte[] packetData) {
        Objects.requireNonNull(packetData);
        if(packetData.length != 20) {
            throw new IllegalArgumentException("Data must be an array of 20 bytes (was: " + packetData.length + ")");
        }
        byte[] aesEncrypted = aesEncrypt(Arrays.copyOfRange(packetData, 0, 16));
        byte[] rc4Encrypted = rc4(Arrays.copyOfRange(packetData, 16, 20));
        byte[] result = new byte[20];
        System.arraycopy(aesEncrypted, 0, result, 0, aesEncrypted.length);
        System.arraycopy(rc4Encrypted, 0, result, aesEncrypted.length, 4);
        return result;
    }

    public byte[] decrypt(byte[] packetData) {
        Objects.requireNonNull(packetData);
        if(packetData.length != 20) {
            throw new IllegalArgumentException("Data must be an array of 20 bytes (was: " + packetData.length + ")");
        }
        byte[] aesDecrypted = aesDecrypt(Arrays.copyOfRange(packetData, 0, 16));
        byte[] rc4Decrypted = rc4(Arrays.copyOfRange(packetData, 16, 20));
        byte[] result = new byte[20];
        System.arraycopy(aesDecrypted, 0, result, 0, aesDecrypted.length);
        System.arraycopy(rc4Decrypted, 0, result, aesDecrypted.length, 4);
        return result;
    }

    private byte[] aesEncrypt(byte[] plaintext) {
        try {
            Cipher cipher = Cipher.getInstance("AES/ECB/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "AES"));
            return cipher.doFinal(plaintext);
        } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | IllegalBlockSizeException | BadPaddingException ex) {
            throw new IllegalStateException("AES or ECB mode not supported", ex);
        }
    }

    private byte[] aesDecrypt(byte[] encrypted) {
        try {
            Cipher cipher = Cipher.getInstance("AES/ECB/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "AES"));
            return cipher.doFinal(encrypted);
        } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | IllegalBlockSizeException | BadPaddingException ex) {
            throw new IllegalStateException("AES or ECB mode not supported", ex);
        }
    }

    byte[] rc4(byte[] plaintext) {
        int[] S = rc4SBox(key);
        byte[] result = new byte[plaintext.length];
        int i = 0;
        int j = 0;
        for (int n = 0; n < plaintext.length; n++) {
            i = (i + 1) % 256;
            j = (j + S[i]) % 256;
            swap(S, i, j);
            int r = S[(S[i] + S[j]) % 256];
            result[n] = (byte) (r ^ plaintext[n]);
        }
        return result;
    }

    static private int[] rc4SBox(byte[] key) {
        int[] s = new int[256];
        for (int i = 0; i < 256; i++) {
            s[i] = i;
        }
        int j = 0;
        for (int i = 0; i < 256; i++) {
            j = (j + s[i] + (0xFF & key[i % key.length])) % 256;
            swap(s, i, j);
        }
        return s;
    }

    static void swap(byte[] data, int pos1, int pos2) {
        byte intermediate = data[pos1];
        data[pos1] = data[pos2];
        data[pos2] = intermediate;
    }

    static void swap(int[] data, int pos1, int pos2) {
        int intermediate = data[pos1];
        data[pos1] = data[pos2];
        data[pos2] = intermediate;
    }
}
