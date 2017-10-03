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
import com.armedia.caliente.store.CmfAttributeMapper;
import com.armedia.caliente.store.CmfObject;
import com.armedia.caliente.store.CmfStorageException;
import com.armedia.caliente.store.CmfTransformer;
import com.armedia.caliente.store.CmfValue;
import com.armedia.commons.utilities.Tools;

public class Transformer implements CmfTransformer {
	private static ConcurrentMap<URI, Transformer> INSTANCES = new ConcurrentHashMap<>();

	private static final String DEFAULT_TRANSFORMATION_NAME = "transform.xml";

	public static Transformer getInstance() throws Exception {
		return Transformer.getInstance((String) null);
	}

	public static Transformer getInstance(String xmlData) throws Exception {
		URL url = null;
		if (xmlData != null) {
			try {
				URI uri = new URI(xmlData);
				// TODO: Support commons-VFS URLs here?
				if ("resource".equalsIgnoreCase(uri.getScheme()) || "res".equalsIgnoreCase(uri.getScheme())
					|| "classpath".equalsIgnoreCase(uri.getScheme()) || "cp".equalsIgnoreCase(uri.getScheme())) {
					// It's a classpath reference, so let's just find the first resource that
					// matches the SSP
					url = Thread.currentThread().getContextClassLoader().getResource(uri.getSchemeSpecificPart());
					if (url == null) { throw new Exception(
						String.format("Failed to locate the specified transformation [%s] in the classpath",
							uri.getSchemeSpecificPart())); }
					return Transformer.getInstance(url);
				} else {
					try {
						url = uri.toURL();
					} catch (MalformedURLException e) {
						throw new Exception(String.format("The given URI [%s] is not a valid URL", xmlData), e);
					}
				}
			} catch (URISyntaxException e) {
				// Not a URI, so it must be an absolute path
			}
		} else {
			// No data given, so use the default
			xmlData = Transformer.DEFAULT_TRANSFORMATION_NAME;
		}

		if (url == null) {
			File f = Tools.canonicalize(new File(xmlData));
			if (!f.exists()) { throw new FileNotFoundException(
				String.format("Transformation file [%s] does not exist", f.getAbsolutePath())); }
			if (!f.isFile()) { throw new FileNotFoundException(String
				.format("The path at [%s] is not a valid transformation file (not a file!)", f.getAbsolutePath())); }
			if (!f.canRead()) { throw new FileNotFoundException(
				String.format("Transformation file [%s] is not readable", f.getAbsolutePath())); }
			// It exists, it's a file, and can be read!! Move forward!
			url = f.toURI().toURL();
		}

		return Transformer.getInstance(url);
	}

	public static Transformer getInstance(final URL resource) throws Exception {
		Objects.requireNonNull(resource, "Must provide a non-null resource URL");
		return ConcurrentUtils.createIfAbsent(Transformer.INSTANCES, resource.toURI(),
			new ConcurrentInitializer<Transformer>() {
				@Override
				public Transformer get() throws ConcurrentException {
					try (InputStream in = resource.openStream()) {
						return new Transformer(in);
					} catch (JAXBException e) {
						throw new ConcurrentException(
							String.format("Failed to parse out the XML resource at [%s]", resource), e);
					} catch (IOException e) {
						throw new ConcurrentException(
							String.format("Failed to retrieve the contents of the URL [%s]", resource), e);
					}
				}
			});
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

	private TransformationContext createContext(CmfAttributeMapper mapper, CmfObject<CmfValue> object) {
		return new TransformationContext(new DefaultTransformableObjectFacade(object), mapper);
	}

	@Override
	public CmfObject<CmfValue> transform(CmfAttributeMapper mapper, CmfObject<CmfValue> object)
		throws CmfStorageException {
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