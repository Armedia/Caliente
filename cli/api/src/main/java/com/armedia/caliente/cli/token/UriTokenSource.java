/*******************************************************************************
 * #%L
 * Armedia Caliente
 * %%
 * Copyright (c) 2010 - 2019 Armedia LLC
 * %%
 * This file is part of the Caliente software. 
 *  
 * If the software was purchased under a paid Caliente license, the terms of 
 * the paid license agreement will prevail.  Otherwise, the software is 
 * provided under the following open source license terms:
 *
 * Caliente is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *   
 * Caliente is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Caliente. If not, see <http://www.gnu.org/licenses/>.
 * #L%
 *******************************************************************************/
package com.armedia.caliente.cli.token;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

import com.armedia.commons.utilities.Tools;

public class UriTokenSource extends ReaderTokenSource {

	private static final String[] CLASSPATH_SCHEME_STRINGS = {
		"classpath", "cp", "res", "resource"
	};
	private static final Set<String> CLASSPATH_SCHEMES;

	static {
		Set<String> set = Arrays.stream(UriTokenSource.CLASSPATH_SCHEME_STRINGS).filter(StringUtils::isNotEmpty)
			.collect(Collectors.toCollection(TreeSet::new));
		CLASSPATH_SCHEMES = Tools.freezeSet(new LinkedHashSet<>(set));
	}

	private static boolean isClasspathScheme(String scheme) {
		if (StringUtils.isEmpty(scheme)) { return false; }
		return UriTokenSource.CLASSPATH_SCHEMES.contains(StringUtils.lowerCase(scheme));
	}

	private static boolean supportsClasspath() {
		for (String s : UriTokenSource.CLASSPATH_SCHEMES) {
			try {
				new URI(s, "", "").toURL();
				return true;
			} catch (URISyntaxException e) {
				continue;
			} catch (MalformedURLException e) {
				continue;
			}
		}
		return false;
	}

	public static boolean isSupported(URI uri) {
		if (uri == null) { return false; }
		if (UriTokenSource.CLASSPATH_SCHEMES.contains(StringUtils.lowerCase(uri.getScheme()))) { return true; }
		try {
			uri.toURL();
			return true;
		} catch (Exception e) {
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
		if (UriTokenSource.isClasspathScheme(scheme) && !UriTokenSource.supportsClasspath()) {
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