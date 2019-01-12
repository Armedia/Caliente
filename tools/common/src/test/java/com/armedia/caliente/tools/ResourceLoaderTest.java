package com.armedia.caliente.tools;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.Assert;
import org.junit.Test;

public class ResourceLoaderTest {

	@Test
	public void testIsSupported() {
		Collection<Pair<String, Boolean>> urlData = new ArrayList<>();
		urlData.add(Pair.of("http://www.google.com", null));
		urlData.add(Pair.of("https://www.google.com", null));
		urlData.add(Pair.of("file:///www.google.com", null));
		urlData.add(Pair.of("classpath://www.google.com", true));
		urlData.add(Pair.of("cp://www.google.com", true));
		urlData.add(Pair.of("resource://www.google.com", true));
		urlData.add(Pair.of("res://www.google.com", true));
		urlData.add(Pair.of("ssh://www.google.com", null));
		urlData.add(Pair.of("jdbc:some:crap", null));
		urlData.add(Pair.of("some-weird-uri-syntax", null));

		urlData.forEach((p) -> {
			final String str = p.getLeft();
			Boolean expect = p.getRight();

			final URI uri;
			try {
				uri = new URI(str);
			} catch (URISyntaxException e) {
				// If we can't build a URI, we can't test the string...
				return;
			}

			if (expect == null) {
				try {
					uri.toURL();
					expect = true;
				} catch (Exception e) {
					expect = false;
				}
			}
			Assert.assertEquals(expect, ResourceLoader.isSupported(uri));
		});
	}

	@Test
	public void testGetResourceURI() {
		Collection<Pair<String, Object>> urlData = new ArrayList<>();
		urlData.add(Pair.of("http://www.google.com", -1L));
		urlData.add(Pair.of("https://www.google.com", -1L));

		try {
			Assert.assertNull(ResourceLoader.getResource((URI) null));
		} catch (Throwable t) {
			Assert.fail("Failed to accept a null URI");
		}

		try {
			File f = File.createTempFile("testfile", "test");
			f.deleteOnExit();
			// Fill it with random data
			Random r = new Random(System.currentTimeMillis());
			byte[] data = new byte[r.nextInt(1024) + 1024];
			r.nextBytes(data);
			FileUtils.writeByteArrayToFile(f, data);
			urlData.add(Pair.of(f.toURI().toURL().toString(), f.length()));
		} catch (Exception e) {
			// Can't test this...
		}

		String[] cp = {
			"classpath", "cp", "resource", "res"
		};
		List<Pair<String, Long>> sums = new ArrayList<>();
		sums.add(Pair.of("1e69890c0a5acb92a7f21f707471505bbb32f646a408f4e3dae5a7949f62015d", 65536L));
		sums.add(Pair.of("ad77bcbc77546d3d882f6d1fd731970e2865cb07973b6067c416caa97a8ee544", 65536L));
		sums.add(Pair.of("4198124aeae024dc6b88a87627742c5bf0ac286f0389d0b8f4ec9ba8ed14ffb3", 65536L));
		sums.add(Pair.of("2866c88c068ccf395f4eb5584a1b623e82d06ac25a77a1b23c1ef79f069a3a5c", 65536L));
		sums.add(Pair.of("dc1a9c7ac5eabf3ea6c3b4b72396b94d6e71a97a42710b3cd9d07e0f0d953a4d", 65536L));
		Map<String, String> verifier = new HashMap<>();
		for (int i = 0; i <= (sums.size() + 1); i++) {
			for (int p = 0; p < cp.length; p++) {
				String url = String.format("%s:/resource-%d.dat", cp[p], i);
				if (i < sums.size()) {
					Pair<String, Long> checksum = sums.get(i);
					verifier.put(url, checksum.getLeft());
					urlData.add(Pair.of(url, checksum.getRight()));
				} else {
					urlData.add(Pair.of(url, null));
				}
			}
		}

		urlData.add(Pair.of("ssh://www.google.com", Throwable.class));
		urlData.add(Pair.of("jdbc:some:crap", Throwable.class));
		urlData.add(Pair.of("some-weird-uri-syntax", Throwable.class));
		urlData.add(Pair.of("!This is an illegal URI", Throwable.class));

		urlData.forEach((p) -> {
			final String str = p.getLeft();
			Object result = p.getRight();

			final URI uri;
			try {
				uri = new URI(str);
			} catch (URISyntaxException e) {
				// If we can't build a URI, we can't test the string...
				return;
			}

			URL url = null;
			Throwable raised = null;
			try {
				url = ResourceLoader.getResource(uri);
			} catch (Throwable t) {
				raised = t;
			}

			if (result == null) {
				// Supported, but should be missing
				Assert.assertNull(url);
				Assert.assertNull(raised);
				return;
			}

			if (Number.class.isInstance(result)) {
				// Supported, but may be unreachable (i.e. http:// or https:// on standalone
				// networkless systems)
				Number n = Number.class.cast(result);
				Assert.assertNotNull(url);
				Assert.assertNull(raised);

				byte[] data = null;
				String actualSum = null;

				try (InputStream in = url.openStream()) {
					data = IOUtils.toByteArray(in);
					actualSum = StringUtils.lowerCase(DigestUtils.sha256Hex(data));
				} catch (IOException e) {
					if (n.longValue() >= 0) {
						Assert.fail(String.format("Failed to read from the URL [%s]: %s", str, e.getMessage()));
						return;
					}
				}

				// Only check the size if we expect to find it and know the size beforehand
				// a negative value means we expect to find it, but won't know the size beforehand
				if (n.longValue() >= 0) {
					Assert.assertEquals(n.longValue(), data.length);
				}

				String expectedSum = StringUtils.lowerCase(verifier.get(str));
				if (expectedSum != null) {
					Assert.assertEquals(expectedSum, actualSum);
				}
				return;
			}

			if (Throwable.class == result) {
				// Not supported, should have raised an exception
				Assert.assertNotNull(String.format("Did not raise an exception for known-bad URL [%s]", str), raised);
			}

		});
	}

	@Test
	public void testGetResourceString() {
		Collection<Pair<String, Object>> urlData = new ArrayList<>();
		urlData.add(Pair.of("http://www.google.com", -1L));
		urlData.add(Pair.of("https://www.google.com", -1L));

		try {
			Assert.assertNull(ResourceLoader.getResource((URI) null));
		} catch (Throwable t) {
			Assert.fail("Failed to accept a null URI");
		}

		try {
			File f = File.createTempFile("testfile", "test");
			f.deleteOnExit();
			// Fill it with random data
			Random r = new Random(System.currentTimeMillis());
			byte[] data = new byte[r.nextInt(1024) + 1024];
			r.nextBytes(data);
			FileUtils.writeByteArrayToFile(f, data);
			urlData.add(Pair.of(f.toURI().toURL().toString(), f.length()));
		} catch (Exception e) {
			// Can't test this...
		}

		String[] cp = {
			"classpath", "cp", "resource", "res"
		};
		List<Pair<String, Long>> sums = new ArrayList<>();
		sums.add(Pair.of("1e69890c0a5acb92a7f21f707471505bbb32f646a408f4e3dae5a7949f62015d", 65536L));
		sums.add(Pair.of("ad77bcbc77546d3d882f6d1fd731970e2865cb07973b6067c416caa97a8ee544", 65536L));
		sums.add(Pair.of("4198124aeae024dc6b88a87627742c5bf0ac286f0389d0b8f4ec9ba8ed14ffb3", 65536L));
		sums.add(Pair.of("2866c88c068ccf395f4eb5584a1b623e82d06ac25a77a1b23c1ef79f069a3a5c", 65536L));
		sums.add(Pair.of("dc1a9c7ac5eabf3ea6c3b4b72396b94d6e71a97a42710b3cd9d07e0f0d953a4d", 65536L));
		Map<String, String> verifier = new HashMap<>();
		for (int i = 0; i <= (sums.size() + 1); i++) {
			for (int p = 0; p < cp.length; p++) {
				String url = String.format("%s:/resource-%d.dat", cp[p], i);
				if (i < sums.size()) {
					Pair<String, Long> checksum = sums.get(i);
					verifier.put(url, checksum.getLeft());
					urlData.add(Pair.of(url, checksum.getRight()));
				} else {
					urlData.add(Pair.of(url, null));
				}
			}
		}

		urlData.add(Pair.of("ssh://www.google.com", Throwable.class));
		urlData.add(Pair.of("jdbc:some:crap", Throwable.class));
		urlData.add(Pair.of("some-weird-uri-syntax", Throwable.class));
		urlData.add(Pair.of("!This is an illegal URI", Throwable.class));

		urlData.forEach((p) -> {
			final String uri = p.getLeft();
			Object result = p.getRight();

			try {
				new URI(uri);
			} catch (URISyntaxException e) {
				// If we can't build a URI, we shold expect the test to fail
				result = Throwable.class;
			}

			URL url = null;
			Throwable raised = null;
			try {
				url = ResourceLoader.getResource(uri);
			} catch (Throwable t) {
				raised = t;
			}

			if (result == null) {
				// Supported, but should be missing
				Assert.assertNull(url);
				Assert.assertNull(raised);
				return;
			}

			if (Number.class.isInstance(result)) {
				// Supported, but may be unreachable (i.e. http:// or https:// on standalone
				// networkless systems)
				Number n = Number.class.cast(result);
				Assert.assertNotNull(url);
				Assert.assertNull(raised);

				byte[] data = null;
				String actualSum = null;

				try (InputStream in = url.openStream()) {
					data = IOUtils.toByteArray(in);
					actualSum = StringUtils.lowerCase(DigestUtils.sha256Hex(data));
				} catch (IOException e) {
					if (n.longValue() >= 0) {
						Assert.fail(String.format("Failed to read from the URL [%s]: %s", uri, e.getMessage()));
						return;
					}
				}

				// Only check the size if we expect to find it and know the size beforehand
				// a negative value means we expect to find it, but won't know the size beforehand
				if (n.longValue() >= 0) {
					Assert.assertEquals(n.longValue(), data.length);
				}

				String expectedSum = StringUtils.lowerCase(verifier.get(uri));
				if (expectedSum != null) {
					Assert.assertEquals(expectedSum, actualSum);
				}
				return;
			}

			if (Throwable.class == result) {
				// Not supported, should have raised an exception
				Assert.assertNotNull(String.format("Did not raise an exception for known-bad URL [%s]", uri), raised);
			}

		});
	}

}
