package com.armedia.caliente.tools;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class CmfCryptTest {
	@Test
	public void testEncryptDecrypt() throws Exception {
		CmfCrypt crypt = new CmfCrypt();
		for (int i = 0; i < 0xFF; i++) {
			String str = String.format("Password-%02x-%s", i, UUID.randomUUID());
			String encrypted = crypt.encrypt(str);
			Assertions.assertNotEquals(str, encrypted);
			String decrypted = crypt.decrypt(encrypted);
			Assertions.assertEquals(str, decrypted);
		}
	}

	@Test
	public void testEncodeDecode() throws Exception {
		CmfCrypt crypt = new CmfCrypt();
		for (int i = 0; i < 0xFF; i++) {
			String str = String.format("Password-%02x-%s", i, UUID.randomUUID());
			byte[] encrypted = crypt.encode(str);

			// Is it the raw bytes?
			String other = new String(encrypted, StandardCharsets.UTF_8);
			Assertions.assertNotEquals(str, other);

			// Is it the Base64-encoded value?
			other = Base64.encodeBase64String(encrypted);
			Assertions.assertNotEquals(Base64.encodeBase64String(str.getBytes(StandardCharsets.UTF_8)), other);

			// Make sure we get the original value when we decode (decrypt) it
			String decrypted = crypt.decode(encrypted);
			Assertions.assertEquals(str, decrypted);
		}

		byte[] nullData = crypt.encode(null);
		Assertions.assertNotNull(nullData);
		Assertions.assertNotEquals(0, nullData.length);
		byte[] emptyData = crypt.encode(StringUtils.EMPTY);
		Assertions.assertNotNull(emptyData);
		Assertions.assertNotEquals(0, emptyData.length);
		Assertions.assertArrayEquals(nullData, emptyData);

		String nullStr = crypt.decode(null);
		Assertions.assertNotNull(nullStr);
		String emptyStr = crypt.decode(new byte[0]);
		Assertions.assertNotNull(emptyStr);
		Assertions.assertEquals(nullStr, emptyStr);
	}
}