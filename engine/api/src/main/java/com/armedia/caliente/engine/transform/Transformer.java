package com.armedia.caliente.engine.transform;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.xml.bind.JAXBException;

import org.apache.commons.io.input.XmlStreamReader;
import org.apache.commons.lang3.concurrent.ConcurrentException;
import org.apache.commons.lang3.concurrent.ConcurrentInitializer;
import org.apache.commons.lang3.concurrent.ConcurrentUtils;

import com.armedia.caliente.engine.transform.xml.Transformations;
import com.armedia.caliente.store.CmfObject;
import com.armedia.caliente.store.CmfStorageException;
import com.armedia.caliente.store.CmfTransformer;
import com.armedia.caliente.store.CmfValue;
import com.armedia.caliente.store.CmfValueMapper;
import com.armedia.commons.utilities.Tools;

public class Transformer implements CmfTransformer {
	private static ConcurrentMap<URL, Transformer> INSTANCES = new ConcurrentHashMap<>();

	private static final String DEFAULT_TRANSFORMATION_NAME = "transform.xml";

	public static Transformer getInstance() throws TransformationException {
		return Transformer.getInstance((String) null);
	}

	private static URL getFileURL(String filePath, boolean required)
		throws FileNotFoundException, MalformedURLException {
		File f = Tools.canonicalize(new File(filePath));
		if (!f.exists()) {
			if (!required) { return null; }
			throw new FileNotFoundException(
				String.format("Transformation file [%s] does not exist", f.getAbsolutePath()));
		}

		if (!f.isFile()) {
			if (!required) { return null; }
			throw new FileNotFoundException(String
				.format("The path at [%s] is not a valid transformation file (not a file!)", f.getAbsolutePath()));
		}

		if (!f.canRead()) { throw new FileNotFoundException(
			String.format("Transformation file [%s] is not readable", f.getAbsolutePath())); }

		// It exists, it's a file, and can be read!! Move forward!
		return f.toURI().toURL();
	}

	public static Transformer getInstance(String location) throws TransformationException {
		URL url = null;
		if (location == null) {
			try {
				url = Transformer.getFileURL(Transformer.DEFAULT_TRANSFORMATION_NAME, false);
				// If nothing was returned, then we return no transformer...
				if (url == null) { return null; }
			} catch (FileNotFoundException | MalformedURLException e) {
				throw new TransformationException(String.format("Failed to load the default transformation file [%s]",
					Transformer.DEFAULT_TRANSFORMATION_NAME), e);
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
						throw new TransformationException(
							String.format("Failed to locate the specified transformation [%s] in the classpath",
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
				url = Transformer.getFileURL(location, true);
			} catch (FileNotFoundException | MalformedURLException e) {
				throw new TransformationException(String.format("Failed to get the file URL from [%s]", location), e);
			}
		}

		return Transformer.getInstance(url);
	}

	private static URL normalize(final URL url) {
		try {
			return url.toURI().normalize().toURL();
		} catch (MalformedURLException | URISyntaxException e) {
			return url;
		}
	}

	public static Transformer getInstance(final URL resource) throws TransformationException {
		Objects.requireNonNull(resource, "Must provide a non-null resource URL");
		final URL key = Transformer.normalize(resource);
		try {
			return ConcurrentUtils.createIfAbsent(Transformer.INSTANCES, key, new ConcurrentInitializer<Transformer>() {
				@Override
				public Transformer get() throws ConcurrentException {
					try (InputStream in = key.openStream()) {
						return new Transformer(in);
					} catch (JAXBException e) {
						throw new ConcurrentException(
							String.format("Failed to parse out the XML resource at [%s]", key), e);
					} catch (IOException e) {
						throw new ConcurrentException(
							String.format("Failed to retrieve the contents of the URL [%s]", key), e);
					}
				}
			});
		} catch (ConcurrentException e) {
			throw new TransformationException(e.getMessage(), e.getCause());
		}
	}

	public static Transformer getNewInstance(final URL resource) throws Exception {
		Objects.requireNonNull(resource, "Must provide a non-null resource URL");
		Transformer.INSTANCES.remove(resource.toURI());
		return Transformer.getInstance(resource);
	}

	private final Transformations transformations;

	public Transformer(InputStream in) throws JAXBException {
		this.transformations = Transformations.loadFromXML(in);
	}

	public Transformer(Reader in) throws JAXBException {
		this.transformations = Transformations.loadFromXML(in);
	}

	public Transformer(XmlStreamReader in) throws JAXBException {
		this.transformations = Transformations.loadFromXML(in);
	}

	private TransformationContext createContext(CmfValueMapper mapper, CmfObject<CmfValue> object) {
		return new TransformationContext(new DefaultTransformableObjectFacade(object), mapper);
	}

	@Override
	public CmfObject<CmfValue> transform(CmfValueMapper mapper, CmfObject<CmfValue> object) throws CmfStorageException {
		TransformationContext ctx = createContext(mapper, object);
		try {
			try {
				this.transformations.apply(ctx);
			} catch (TransformationCompletedException e) {
				// Do nothing - this is simply our shortcut for stopping the transformation work in
				// its tracks
			}

			return ctx.getObject().applyChanges(object);
		} catch (TransformationException e) {
			throw new CmfStorageException(
				String.format("Exception caught while performing the transformation for %s (%s)[%s]", object.getType(),
					object.getLabel(), object.getId()),
				e);
		} finally {
			destroyContext(ctx);
		}
	}

	private void destroyContext(TransformationContext ctx) {
		// Clean things out... to help the GC...
		ctx.getObject().getAtt().clear();
		ctx.getObject().getPriv().clear();
		ctx.getVariables().clear();
	}

	@Override
	public void close() {
		// Don't really need to do anything here...
	}

}