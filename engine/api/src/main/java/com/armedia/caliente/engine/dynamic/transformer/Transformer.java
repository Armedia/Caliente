package com.armedia.caliente.engine.dynamic.transformer;

import com.armedia.caliente.engine.dynamic.ActionException;
import com.armedia.caliente.engine.dynamic.DefaultDynamicObject;
import com.armedia.caliente.engine.dynamic.DynamicElementContext;
import com.armedia.caliente.engine.dynamic.ProcessingCompletedException;
import com.armedia.caliente.engine.dynamic.metadata.ExternalMetadataLoader;
import com.armedia.caliente.engine.dynamic.xml.Transformations;
import com.armedia.caliente.engine.dynamic.xml.XmlInstances;
import com.armedia.caliente.engine.dynamic.xml.XmlNotFoundException;
import com.armedia.caliente.store.CmfObject;
import com.armedia.caliente.store.CmfStorageException;
import com.armedia.caliente.store.CmfValue;
import com.armedia.caliente.store.CmfValueMapper;

public class Transformer {

	private static final XmlInstances<Transformations> INSTANCES = new XmlInstances<>(Transformations.class);

	public static Transformer getTransformer(String location, ExternalMetadataLoader metadataLoader,
		boolean failIfMissing) throws TransformerException {
		try {
			try {
				Transformations transformations = Transformer.INSTANCES.getInstance(location);
				if (transformations == null) { return null; }
				return new Transformer(location, transformations, metadataLoader);
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

	private final Transformations transformations;
	private final ExternalMetadataLoader metadataLoader;

	private Transformer(String location, Transformations transformations, ExternalMetadataLoader metadataLoader)
		throws TransformerException {
		this.transformations = transformations;
		this.metadataLoader = metadataLoader;
	}

	private DynamicElementContext createContext(CmfValueMapper mapper, CmfObject<CmfValue> object) {
		return new DynamicElementContext(object, new DefaultDynamicObject(object), mapper, this.metadataLoader);
	}

	public CmfObject<CmfValue> transform(CmfValueMapper mapper, CmfObject<CmfValue> object) throws CmfStorageException {
		if (this.transformations == null) {
			//
			return object;
		}
		DynamicElementContext ctx = createContext(mapper, object);
		try {
			try {
				this.transformations.apply(ctx);
			} catch (ProcessingCompletedException e) {
				// Do nothing - this is simply our shortcut for stopping the transformation work in
				// its tracks
			}

			return ctx.getDynamicObject().applyChanges(object);
		} catch (ActionException | TransformerException e) {
			throw new CmfStorageException(
				String.format("Exception caught while performing the transformation for %s", object.getDescription()),
				e);
		} finally {
			destroyContext(ctx);
		}
	}

	private void destroyContext(DynamicElementContext ctx) {
		// Clean things out... to help the GC...
		ctx.getDynamicObject().getAtt().clear();
		ctx.getDynamicObject().getPriv().clear();
		ctx.getVariables().clear();
	}

	public void close() {
		if (this.metadataLoader != null) {
			this.metadataLoader.close();
		}
	}

}