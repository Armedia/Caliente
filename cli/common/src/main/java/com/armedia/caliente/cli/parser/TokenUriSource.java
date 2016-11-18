package com.armedia.caliente.cli.parser;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;

public class TokenUriSource extends TokenStreamSource {

	private static final String CLASSPATH = "classpath";

	private static boolean supportsClasspath() {
		try {
			new URI(TokenUriSource.CLASSPATH, "", "").toURL();
			return true;
		} catch (URISyntaxException e) {
			return false;
		} catch (MalformedURLException e) {
			return false;
		}
	}

	private final URI sourceUri;

	public TokenUriSource(String sourceUri) throws URISyntaxException {
		if (sourceUri == null) { throw new IllegalArgumentException("Must provide a non-null String object"); }
		this.sourceUri = new URI(sourceUri);
	}

	public TokenUriSource(URI sourceUri) {
		if (sourceUri == null) { throw new IllegalArgumentException("Must provide a non-null URL object"); }
		this.sourceUri = sourceUri;
	}

	@Override
	public String getKey() {
		return this.sourceUri.toString();
	}

	@Override
	protected InputStream openStream() throws IOException {
		final String scheme = this.sourceUri.getScheme();
		if (TokenUriSource.CLASSPATH.equalsIgnoreCase(scheme) && !TokenUriSource.supportsClasspath()) {
			// It's a classpath URL... let's use the rest of it as the classpath resource to get
			String resource = this.sourceUri.getSchemeSpecificPart();
			// Eliminate all leading slashes
			while (resource.startsWith("/")) {
				resource = resource.substring(1);
			}
			return Thread.currentThread().getContextClassLoader().getResourceAsStream(resource);
		}
		return this.sourceUri.toURL().openStream();
	}
}