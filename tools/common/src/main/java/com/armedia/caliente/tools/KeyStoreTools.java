package com.armedia.caliente.tools;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.AccessDeniedException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.Provider;
import java.security.Security;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.commons.io.FileExistsException;

public class KeyStoreTools {

	public static final Map<String, Set<String>> KEYSTORE_PROVIDERS;
	public static final Map<String, Set<String>> KEYFACTORY_PROVIDERS;

	static {
		Map<String, Set<String>> keystoreProviders = new TreeMap<>();
		Map<String, Set<String>> keyfactoryProviders = new TreeMap<>();
		for (Provider p : Security.getProviders()) {
			for (Provider.Service s : p.getServices()) {
				Map<String, Set<String>> m = null;
				Set<String> c = null;
				if ("KeyStore".equals(s.getType())) {
					m = keystoreProviders;
				} else if ("KeyFactory".equals(s.getType())) {
					m = keyfactoryProviders;
				}
				if (m == null) {
					continue;
				}
				c = m.get(s.getAlgorithm().toUpperCase());
				if (c == null) {
					c = new TreeSet<>();
					m.put(s.getAlgorithm(), c);
				}
				c.add(p.getName());
			}
		}

		// Now, freeze everything
		Map<String, Set<String>> PROVIDERS = new LinkedHashMap<>(keystoreProviders.size());
		for (String k : keystoreProviders.keySet()) {
			PROVIDERS.put(k, Collections.unmodifiableSet(new LinkedHashSet<>(keystoreProviders.get(k))));
		}
		KEYSTORE_PROVIDERS = Collections.unmodifiableMap(PROVIDERS);

		PROVIDERS = new LinkedHashMap<>(keyfactoryProviders.size());
		for (String k : keyfactoryProviders.keySet()) {
			PROVIDERS.put(k, Collections.unmodifiableSet(new LinkedHashSet<>(keyfactoryProviders.get(k))));
		}
		KEYFACTORY_PROVIDERS = Collections.unmodifiableMap(PROVIDERS);
	}

	public static KeyStore loadKeyStore(InputStream in, String pass) throws IOException, KeyStoreException {
		return KeyStoreTools.loadKeyStore(in, pass, null);
	}

	/**
	 * Load the keystore from the given stream, using the given password, and the given type. If the
	 * type is not given, then each supported KeyStore type in the system will be tried in turn,
	 * until one works or all fail.
	 *
	 * @param in
	 * @param pass
	 * @param type
	 * @return an open keystore loaded from the given path
	 * @throws IOException
	 * @throws KeyStoreException
	 */
	public static KeyStore loadKeyStore(InputStream in, String pass, String type)
		throws IOException, KeyStoreException {
		final Set<String> types;
		if (type == null) {
			types = KeyStoreTools.KEYSTORE_PROVIDERS.keySet();
		} else {
			types = Collections.singleton(type);
		}

		final char[] passChars = (pass != null ? pass.toCharArray() : null);
		// Use a 1MB buffer...just in case...
		final int bufSize = 1024 * 1024;
		in = new BufferedInputStream(in, bufSize);
		for (String t : types) {
			final KeyStore ks = KeyStore.getInstance(t);
			in.mark(bufSize);
			try {
				ks.load(in, passChars);
				return ks;
			} catch (Exception e) {
				in.reset();
				// This type failed... try another?
				// TODO: Should I analyze this exception more in depth to identify if this is a
				// finalizing error?
			}
		}
		throw new KeyStoreException(String.format("Failed to load the keystore using types %s and %s password", types,
			pass != null ? "a" : "no"));
	}

	public static KeyStore loadKeyStore(String path, String pass) throws IOException, KeyStoreException {
		return KeyStoreTools.loadKeyStore(path, pass, null);
	}

	/**
	 * Load the keystore from the given filesystem path, using the given password, and the given
	 * type. If the type is not given, then each supported KeyStore type in the system will be tried
	 * in turn, until one works or all fail.
	 *
	 * @param path
	 * @param pass
	 * @param type
	 * @return an open keystore loaded from the given path
	 * @throws FileExistsException
	 *             if the file exists, but is not a regular file
	 * @throws FileNotFoundException
	 *             if the file doesn't exist
	 * @throws AccessDeniedException
	 *             if the file exists, is a regular file, but can't be read
	 */
	public static KeyStore loadKeyStore(String path, String pass, String type) throws IOException, KeyStoreException {
		File f = new File(path);
		if (f.exists() && f.isFile() && f.canRead()) {
			try (InputStream in = new FileInputStream(f)) {
				return KeyStoreTools.loadKeyStore(in, pass, type);
			}
		}
		if (!f.exists()) { throw new FileNotFoundException(path); }
		if (!f.isFile()) { throw new FileExistsException(path); }
		throw new AccessDeniedException(path);
	}

	public static KeyStore loadKeyStore(URL url, String pass) throws IOException, KeyStoreException {
		return KeyStoreTools.loadKeyStore(url, pass, null);
	}

	/**
	 * Load the keystore from the given filesystem path, using the given password, and the given
	 * type. If the type is not given, then each supported KeyStore type in the system will be tried
	 * in turn, until one works or all fail.
	 *
	 * @param url
	 * @param pass
	 * @param type
	 * @return an open keystore loaded from the given path
	 * @throws KeyStoreException
	 */
	public static KeyStore loadKeyStore(URL url, String pass, String type) throws IOException, KeyStoreException {
		try (InputStream in = url.openStream()) {
			return KeyStoreTools.loadKeyStore(in, pass, type);
		}
	}
}