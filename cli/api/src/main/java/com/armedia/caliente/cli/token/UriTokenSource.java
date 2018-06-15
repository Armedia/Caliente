package com.armedia.caliente.cli.token;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;

public class UriTokenSource extends ReaderTokenSource {

	private static final String CLASSPATH = "classpath";

	private static boolean supportsClasspath() {
		try {
			new URI(UriTokenSource.CLASSPATH, "", "").toURL();
			return true;
		} catch (URISyntaxException e) {
			return false;
		} catch (MalformedURLException e) {
			return false;
		}
	}

	private final URI sourceUri;

	public UriTokenSource(URI sourceUri) {
		if (sourceUri == null) { throw new IllegalArgumentException("Must provide a non-null URI object"); }
		this.sourceUri = sourceUri;
	}

	@Override
	public String getKey() {
		return this.sourceUri.toString();
	}

	@Override
	protected Reader openReader() throws IOException {
		final String scheme = this.sourceUri.getScheme();
		InputStream in = null;
		if (UriTokenSource.CLASSPATH.equalsIgnoreCase(scheme) && !UriTokenSource.supportsClasspath()) {
			// It's a classpath URL... let's use the rest of it as the classpath resource to get
			String resource = this.sourceUri.getSchemeSpecificPart();
			// Eliminate all leading slashes
			while (resource.startsWith("/")) {
				resource = resource.substring(1);
			}
			in = Thread.currentThread().getContextClassLoader().getResourceAsStream(resource);
		}
		if (in == null) {
			in = this.sourceUri.toURL().openStream();
		}
		return new InputStreamReader(in, getCharset());
	}

	@Override
	public String toString() {
		return String.format("UriTokenSource [sourceUri=%s]", this.sourceUri);
	}
}