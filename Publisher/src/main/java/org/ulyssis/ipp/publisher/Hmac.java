package org.ulyssis.ipp.publisher;

import org.apache.commons.codec.binary.Hex;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

public class Hmac {
    static final String ALGORITHM = "HmacSHA256";

    public static String generateHmac(byte[] data, byte[] key) throws NoSuchAlgorithmException, InvalidKeyException {
        SecretKeySpec secretKeySpec = new SecretKeySpec(key, ALGORITHM);
        Mac mac = Mac.getInstance(ALGORITHM);
        mac.init(secretKeySpec);
        return Hex.encodeHexString(mac.doFinal(data));
    }

    public static boolean verifyHmac(String mac, byte[] data, byte[] key) throws NoSuchAlgorithmException, InvalidKeyException {
        if (mac == null) {
            return false;
        }

        String expected = generateHmac(data, key);

        if (expected.length() != mac.length()) {
            return false;
        }

        int result = 0;
        for (int i = 0; i < mac.length(); i++) {
            result |= mac.charAt(i) ^ expected.charAt(i);
        }
        return result == 0;
    }
}
