package com.armedia.caliente.engine.dynamic.xml;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.xml.bind.JAXBException;
import javax.xml.bind.annotation.XmlRootElement;

import org.apache.commons.lang3.concurrent.ConcurrentException;
import org.apache.commons.lang3.concurrent.ConcurrentInitializer;
import org.apache.commons.lang3.concurrent.ConcurrentUtils;

import com.armedia.commons.utilities.Tools;

public class XmlInstances<T extends XmlBase> {

	private final ConcurrentMap<URL, T> instances = new ConcurrentHashMap<>();
	private final String defaultFileName;
	private final String label;
	private final Class<T> objectClass;

	private static String getDefaultFileName(Class<?> klass) {
		String name = klass.getSimpleName();
		XmlRootElement element = klass.getAnnotation(XmlRootElement.class);
		if (element != null) {
			name = element.name();
		}
		return String.format("%s.xml", name.toLowerCase());
	}

	public XmlInstances(Class<T> objectClass) {
		this(objectClass, null);
	}

	public XmlInstances(Class<T> objectClass, String defaultFileName) {
		this.objectClass = objectClass;
		this.label = objectClass.getSimpleName();
		if (defaultFileName != null) {
			this.defaultFileName = defaultFileName;
		} else {
			this.defaultFileName = XmlInstances.getDefaultFileName(objectClass);
		}
	}

	public String getDefaultFileName() {
		return this.defaultFileName;
	}

	public T getInstance() throws Exception {
		return getInstance((String) null);
	}

	private URL getFileURL(String filePath, boolean required) throws FileNotFoundException, MalformedURLException {
		File f = Tools.canonicalize(new File(filePath));
		if (!f.exists()) {
			if (!required) { return null; }
			throw new FileNotFoundException(
				String.format("The %s file [%s] does not exist", this.label, f.getAbsolutePath()));
		}

		if (!f.isFile()) {
			if (!required) { return null; }
			throw new FileNotFoundException(String.format("The path at [%s] is not a valid %s file (not a file!)",
				this.label, f.getAbsolutePath()));
		}

		if (!f.canRead()) { throw new FileNotFoundException(
			String.format("The %s file [%s] is not readable", this.label, f.getAbsolutePath())); }

		// It exists, it's a file, and can be read!! Move forward!
		return f.toURI().toURL();
	}

	public T getInstance(String location) throws XmlInstanceException {
		URL url = null;
		if (location == null) {
			try {
				url = getFileURL(this.defaultFileName, false);
				// If nothing was returned, then we return no transformer...
				if (url == null) { return null; }
			} catch (FileNotFoundException | MalformedURLException e) {
				throw new XmlInstanceException(
					String.format("Failed to load the default %s file [%s]", this.label, this.defaultFileName), e);
			}
		}

		if (url == null) {
			// Try to see if it's a URI...
			try {
				URI uri = new URI(location);
				if ("resource".equalsIgnoreCase(uri.getScheme()) || "res".equalsIgnoreCase(uri.getScheme())
					|| "classpath".equalsIgnoreCase(uri.getScheme()) || "cp".equalsIgnoreCase(uri.getScheme())) {
					// It's a classpath reference, so let's just find the first resource that
					// matches the SSP
					url = Thread.currentThread().getContextClassLoader().getResource(uri.getSchemeSpecificPart());
					if (url == null) {
						// No match!! Explode!
						throw new XmlInstanceException(
							String.format("Failed to locate the specified %s file [%s] in the classpath", this.label,
								uri.getSchemeSpecificPart()));
					}
				} else {
					try {
						url = uri.normalize().toURL();
					} catch (MalformedURLException e) {
						// This isn't a valid URL...so it must be a file path
					}
				}
			} catch (URISyntaxException e) {
				// Not a URI...is it a URL? (URL syntax rules are looser than a URI's)
				try {
					url = new URL(location);
				} catch (MalformedURLException e2) {
					// Do nothing...it may still be a file path
				}
			}
		}

		if (url == null) {
			// If it wasn't a straight-up URL, or a classpath URI, then it must be a file path...
			try {
				url = getFileURL(location, true);
			} catch (FileNotFoundException | MalformedURLException e) {
				throw new XmlInstanceException(
					String.format("Failed to get the %s file URL from [%s]", this.label, location), e);
			}
		}

		return getInstance(url);
	}

	private URL normalize(final URL url) {
		try {
			return url.toURI().normalize().toURL();
		} catch (MalformedURLException | URISyntaxException e) {
			return url;
		}
	}

	protected T newInstance(InputStream in) throws JAXBException, IOException {
		return XmlBase.loadFromXML(this.objectClass, in);
	}

	public T getInstance(final URL resource) throws XmlInstanceException {
		Objects.requireNonNull(resource, "Must provide a non-null resource URL");
		final URL key = normalize(resource);
		try {
			return ConcurrentUtils.createIfAbsent(this.instances, key, new ConcurrentInitializer<T>() {
				@Override
				public T get() throws ConcurrentException {
					try (InputStream in = key.openStream()) {
						return newInstance(in);
					} catch (JAXBException e) {
						throw new ConcurrentException(String.format("Failed to parse out the XML %s resource at [%s]",
							XmlInstances.this.label, key), e);
					} catch (IOException e) {
						throw new ConcurrentException(String.format(
							"Failed to retrieve the contents of the %s URL [%s]", XmlInstances.this.label, key), e);
					}
				}
			});
		} catch (ConcurrentException e) {
			throw new XmlInstanceException(e.getMessage(), e.getCause());
		}
	}

	public T getNewInstance(final URL resource) throws Exception {
		Objects.requireNonNull(resource, "Must provide a non-null resource URL");
		this.instances.remove(resource.toURI());
		return getInstance(resource);
	}
}