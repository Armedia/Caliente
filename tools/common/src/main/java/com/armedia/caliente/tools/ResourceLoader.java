package com.armedia.caliente.tools;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.lang3.StringUtils;

import com.armedia.commons.utilities.Tools;

public class ResourceLoader {

	private static final String[] CLASSPATH_SCHEME_STRINGS = {
		"classpath", "cp", "res", "resource"
	};
	private static final Set<String> CLASSPATH_SCHEMES;

	static {
		Set<String> set = new TreeSet<>();
		for (String s : ResourceLoader.CLASSPATH_SCHEME_STRINGS) {
			if (!StringUtils.isEmpty(s)) {
				set.add(StringUtils.lowerCase(s));
			}
		}
		CLASSPATH_SCHEMES = Tools.freezeSet(new LinkedHashSet<>(set));
	}

	private static boolean isClasspath(String scheme) {
		if (StringUtils.isEmpty(scheme)) { return false; }
		return ResourceLoader.CLASSPATH_SCHEMES.contains(StringUtils.lowerCase(scheme));
	}

	public static boolean isSupported(URI uri) {
		if (uri == null) { return false; }
		if (ResourceLoader.CLASSPATH_SCHEMES.contains(StringUtils.lowerCase(uri.getScheme()))) { return true; }
		try {
			uri.toURL();
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	public static URL getResource(URI uri) throws ResourceLoaderException {
		if (uri == null) { return null; }

		URI source = uri;
		if (!uri.isAbsolute()) {
			source = uri.normalize();
		}

		// First things first: is it a URL?
		try {
			return source.toURL();
		} catch (IllegalArgumentException e) {
			throw new ResourceLoaderException(String.format("The given URI [%s] is not absolute", uri), e);
		} catch (MalformedURLException e) {
			// Not supported.... maybe a classpath?
			if (!ResourceLoader.isClasspath(uri.getScheme())) {
				throw new ResourceLoaderException(
					String.format("The URI [%s] is not supported as a resource URI", uri));
			}
			// It's a classpath!! Resolve it...
		}

		String resource = source.getSchemeSpecificPart();
		if (StringUtils.isBlank(resource)) {
			throw new ResourceLoaderException(
				String.format("The URI [%s] is not a valid resource URI (no scheme-specific part!)", uri));
		}

		// Eliminate all leading slashes
		resource = resource.replaceAll("^/+", "");
		return Thread.currentThread().getContextClassLoader().getResource(resource);
	}

	public static URL getResource(String uriStr) throws ResourceLoaderException {
		if (StringUtils.isEmpty(uriStr)) { return null; }
		try {
			return ResourceLoader.getResource(new URI(uriStr));
		} catch (URISyntaxException e) {
			throw new ResourceLoaderException(String.format("The given URI [%s] is not in valid syntax", uriStr), e);
		}
	}

	public static InputStream getResourceAsStream(URI uri) throws ResourceLoaderException, IOException {
		return ResourceLoader.getResource(uri).openStream();
	}

	public static URL getResourceOrFile(String uriOrPath) throws ResourceLoaderException {
		final URI sourceUri;
		try {
			sourceUri = new URI(uriOrPath);
			try {
				URL resource = ResourceLoader.getResource(sourceUri);
				if (resource != null) {
					if (StringUtils.equals("file", resource.getProtocol())) {
						// Local file... treat it as such...
						return new File(resource.getPath()).toURI().toURL();
					} else {
						// Not a local file, use the URI
						return resource;
					}
				}
			} catch (MalformedURLException | ResourceLoaderException e) {
				// Not a valid resource syntax... must be a path!
			}
		} catch (URISyntaxException e) {
			// Not a URI... must be a path
		}

		// It's a local file... if the current source is another local file,
		// and the given path isn't absolute, take its path to be relative to that one
		try {
			Path p = Paths.get(uriOrPath).toAbsolutePath().normalize();
			File f = p.toFile();
			if (!f.exists() || !f.isFile()) { return null; }
			return f.toURI().toURL();
		} catch (Exception e) {
			// Not a URI nor a path!! KABOOM!
			throw new ResourceLoaderException(
				String.format("The string [%s] is neither a valid path nor a valid URI", uriOrPath), e);
		}
	}

	public static InputStream getResourceOrFileAsStream(String uriOrPath) throws ResourceLoaderException, IOException {
		return ResourceLoader.getResourceOrFile(uriOrPath).openStream();

	}
}