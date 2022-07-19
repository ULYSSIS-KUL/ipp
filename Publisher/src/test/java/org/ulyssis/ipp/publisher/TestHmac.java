package org.ulyssis.ipp.publisher;

import org.junit.Test;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

public class TestHmac {
    @Test
    public void testGenerateAndVerify() throws NoSuchAlgorithmException, InvalidKeyException {
        byte[] data = "HMAC test string".getBytes();
        byte[] key = Base64.getDecoder().decode("jiHZ3n57BRk0PfjQgaIBo9As4F7SuXOgQRuNaddD23F22rQViEH2nEbKpO8XNko+Gc3jx9i1LdwKw3FtWgc/nQ==");
        String hmac = Hmac.generateHmac(data, key);
        assertThat(Hmac.verifyHmac(hmac, data, key), equalTo(true));
    }

    @Test
    public void testGenerateAndVerify_WrongData() throws NoSuchAlgorithmException, InvalidKeyException {
        byte[] data = "HMAC test string".getBytes();
        byte[] key = Base64.getDecoder().decode("jiHZ3n57BRk0PfjQgaIBo9As4F7SuXOgQRuNaddD23F22rQViEH2nEbKpO8XNko+Gc3jx9i1LdwKw3FtWgc/nQ==");
        String hmac = Hmac.generateHmac(data, key);
        byte[] data2 = "HMAC test String".getBytes();
        assertThat(Hmac.verifyHmac(hmac, data2, key), equalTo(false));
    }
}
