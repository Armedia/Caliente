package com.armedia.caliente.store.local;

import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.armedia.caliente.store.CmfStorageException;

public class LocalContentLocatorTest {

	@Test
	public void testConstructor() {
		Assertions.assertThrows(IllegalArgumentException.class, () -> new LocalContentLocator(null, null));
		for (String scheme : LocalContentLocator.SCHEMES) {
			Assertions.assertThrows(IllegalArgumentException.class, () -> new LocalContentLocator(scheme, null));
			Assertions.assertThrows(IllegalArgumentException.class, () -> new LocalContentLocator(scheme, ""));
		}

		String path = "/a/b/c/d";
		Assertions.assertThrows(IllegalArgumentException.class, () -> new LocalContentLocator(null, path));
		for (String scheme : LocalContentLocator.SCHEMES) {
			new LocalContentLocator(scheme, path);
		}
	}

	@Test
	public void testIsSupported() {
		Assertions.assertFalse(LocalContentLocator.isSupported(null));
		Assertions.assertFalse(LocalContentLocator.isSupported(""));
		Assertions.assertFalse(LocalContentLocator.isSupported("abcde"));
		Assertions.assertFalse(LocalContentLocator.isSupported(UUID.randomUUID().toString()));
		for (String scheme : LocalContentLocator.SCHEMES) {
			Assertions.assertTrue(LocalContentLocator.isSupported(scheme));
		}
	}

	@Test
	public void testEncodeDecode() throws CmfStorageException {
		// Create a string containing the first 255 characters from UTF-8
		StringBuilder sb = new StringBuilder();
		for (int i = 1; i < 255; i++) {
			sb.append((char) i);
		}
		String raw = sb.toString();
		for (String scheme : LocalContentLocator.SCHEMES) {
			LocalContentLocator l = new LocalContentLocator(scheme, raw);
			String encoded = l.encode();
			LocalContentLocator decoded = LocalContentLocator.decode(encoded);
			Assertions.assertEquals(l, decoded);
		}

		Assertions.assertThrows(CmfStorageException.class, () -> LocalContentLocator.decode("abcde"));
		Assertions.assertThrows(CmfStorageException.class, () -> LocalContentLocator.decode("badScheme:abcde"));
		Assertions.assertThrows(CmfStorageException.class, () -> LocalContentLocator.decode("raw:"));
	}

	@Test
	public void testGetScheme() {
		String path = "/a/bc//d/e";
		for (String scheme : LocalContentLocator.SCHEMES) {
			Assertions.assertEquals(scheme, new LocalContentLocator(scheme, path).getScheme());
		}
	}

	@Test
	public void testGetPath() {
		String path = "/a/bc//d/e";
		for (String scheme : LocalContentLocator.SCHEMES) {
			Assertions.assertEquals(path, new LocalContentLocator(scheme, path).getPath());
		}
	}

	@Test
	public void testEqualsObject() {
		List<String> paths = new LinkedList<>();
		for (int i = 1; i <= 10; i++) {
			paths.add(String.format("/path/number/%02d", i));
		}

		List<LocalContentLocator> locators = new LinkedList<>();
		for (String scheme : LocalContentLocator.SCHEMES) {
			for (String path : paths) {
				locators.add(new LocalContentLocator(scheme, path));
			}
		}

		for (LocalContentLocator a : locators) {
			Assertions.assertNotEquals(a, null);
			Assertions.assertNotEquals(a, this);

			for (LocalContentLocator b : locators) {
				if (a == b) {
					Assertions.assertEquals(a, b);
					b = new LocalContentLocator(b.getScheme(), b.getPath());
					Assertions.assertEquals(a, b);
					Assertions.assertEquals(a.hashCode(), b.hashCode());
					Assertions.assertEquals(a.encode(), b.encode());
				} else {
					Assertions.assertNotEquals(a, b);
					b = new LocalContentLocator(b.getScheme(), b.getPath());
					Assertions.assertNotEquals(a, b);
					Assertions.assertNotEquals(a.hashCode(), b.hashCode());
					Assertions.assertNotEquals(a.encode(), b.encode());
				}
			}
		}
	}

	@Test
	public void testHashCode() {
	}
}