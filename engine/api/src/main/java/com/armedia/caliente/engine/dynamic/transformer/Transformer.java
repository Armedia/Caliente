package com.armedia.caliente.engine.dynamic.transformer;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.commons.lang3.StringUtils;

import com.armedia.caliente.engine.dynamic.ActionException;
import com.armedia.caliente.engine.dynamic.DefaultDynamicObject;
import com.armedia.caliente.engine.dynamic.DynamicElementContext;
import com.armedia.caliente.engine.dynamic.DynamicObject;
import com.armedia.caliente.engine.dynamic.ProcessingCompletedException;
import com.armedia.caliente.engine.dynamic.mapper.AttributeMapper;
import com.armedia.caliente.engine.dynamic.mapper.schema.SchemaService;
import com.armedia.caliente.engine.dynamic.mapper.schema.SchemaServiceException;
import com.armedia.caliente.engine.dynamic.metadata.ExternalMetadataLoader;
import com.armedia.caliente.engine.dynamic.xml.Transformations;
import com.armedia.caliente.engine.dynamic.xml.XmlInstances;
import com.armedia.caliente.engine.dynamic.xml.XmlNotFoundException;
import com.armedia.caliente.store.CmfObject;
import com.armedia.caliente.store.CmfValue;
import com.armedia.caliente.store.CmfValueMapper;

public class Transformer {

	private static final XmlInstances<Transformations> INSTANCES = new XmlInstances<>(Transformations.class);

	public static Transformer getTransformer(String location, ExternalMetadataLoader metadataLoader,
		AttributeMapper attributeMapper, boolean failIfMissing) throws TransformerException {
		try {
			try {
				Transformations transformations = Transformer.INSTANCES.getInstance(location);
				if (transformations == null) { return null; }
				return new Transformer(location, transformations, metadataLoader, attributeMapper);
			} catch (final XmlNotFoundException e) {
				if (!failIfMissing) { return null; }
				throw e;
			}
		} catch (Exception e) {
			String pre = "";
			String post = "";
			if (location == null) {
				pre = "default ";
			} else {
				post = String.format(" from [%s]", location);
			}
			throw new TransformerException(
				String.format("Failed to load the %stransformation configuration%s", pre, post), e);
		}
	}

	public static String getDefaultLocation() {
		return Transformer.INSTANCES.getDefaultFileName();
	}

	private final ReadWriteLock rwLock = new ReentrantReadWriteLock();
	private final Transformations transformations;
	private final ExternalMetadataLoader metadataLoader;
	private final AttributeMapper attributeMapper;
	private boolean closed = false;

	private Transformer(String location, Transformations transformations, ExternalMetadataLoader metadataLoader,
		AttributeMapper attributeMapper) throws TransformerException {
		this.transformations = transformations;
		this.metadataLoader = metadataLoader;
		this.attributeMapper = attributeMapper;
	}

	private DynamicElementContext createContext(CmfValueMapper mapper, CmfObject<CmfValue> object) {
		return new DynamicElementContext(object, new DefaultDynamicObject(object), mapper, this.metadataLoader);
	}

	public CmfObject<CmfValue> transform(CmfValueMapper mapper, SchemaService schemaService, CmfObject<CmfValue> object)
		throws TransformerException {
		Lock l = this.rwLock.readLock();
		l.lock();
		try {
			if (this.closed) { throw new TransformerException("This transformer instance is already closed"); }
			if (this.transformations == null) { return object; }
			DynamicElementContext ctx = createContext(mapper, object);
			try {
				try {
					this.transformations.apply(ctx);
				} catch (ProcessingCompletedException e) {
					// Do nothing - this is simply our shortcut for stopping the transformation work
					// in its tracks
				}

				final DynamicObject dynamic = ctx.getDynamicObject();
				if (StringUtils.equalsIgnoreCase(dynamic.getObjectId(), "09de75d1800295db")) {
					"".hashCode();
				}

				if (this.attributeMapper != null) {
					try {
						this.attributeMapper.renderMappedAttributes(schemaService, dynamic);
					} catch (SchemaServiceException e) {
						throw new TransformerException(
							String.format("Failed to apply the attribute mappings for %s", object.getDescription()), e);
					}
				}

				return dynamic.applyChanges(object);
			} catch (ActionException e) {
				throw new TransformerException(String
					.format("Exception caught while performing the transformation for %s", object.getDescription()), e);
			} finally {
				destroyContext(ctx);
			}
		} finally {
			l.unlock();
		}
	}

	private void destroyContext(DynamicElementContext ctx) {
		// Clean things out... to help the GC...
		ctx.getDynamicObject().getAtt().clear();
		ctx.getDynamicObject().getPriv().clear();
		ctx.getVariables().clear();
	}

	public void close() {
		Lock l = this.rwLock.writeLock();
		l.lock();
		try {
			if (this.closed) { return; }
			if (this.metadataLoader != null) {
				this.metadataLoader.close();
			}
		} finally {
			this.closed = true;
			l.unlock();
		}
	}

}