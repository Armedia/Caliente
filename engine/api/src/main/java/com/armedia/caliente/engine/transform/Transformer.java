package com.armedia.caliente.engine.transform;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.net.URI;
import java.net.URL;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.xml.bind.JAXBException;

import org.apache.commons.io.input.XmlStreamReader;
import org.apache.commons.lang3.concurrent.ConcurrentException;
import org.apache.commons.lang3.concurrent.ConcurrentInitializer;
import org.apache.commons.lang3.concurrent.ConcurrentUtils;

import com.armedia.caliente.engine.transform.xml.RuntimeTransformationException;
import com.armedia.caliente.engine.transform.xml.Transformations;
import com.armedia.caliente.store.CmfObject;

public class Transformer {

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

	public <V> CmfObject<V> transform(CmfObject<V> object, TransformationContext<V> ctx)
		throws RuntimeTransformationException {
		// Do nothing, for now... but:
		// * create and initialize the context
		// * invoke the transformation
		this.transformations.apply(ctx);
		// * harvest the transformations, and turn them into a new object
		return object;
	}

	private static ConcurrentMap<URI, Transformer> INSTANCES = new ConcurrentHashMap<>();

	public static Transformer getCachedInstance(final URL resource) throws Exception {
		Objects.requireNonNull(resource, "Must provide a non-null resource URL");
		// TODO: Support classpath:/-style resources?
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
		return Transformer.getCachedInstance(resource);
	}

}