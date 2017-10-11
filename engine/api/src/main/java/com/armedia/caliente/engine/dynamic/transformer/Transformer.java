package com.armedia.caliente.engine.dynamic.transformer;

import com.armedia.caliente.engine.dynamic.ActionException;
import com.armedia.caliente.engine.dynamic.DefaultTransformableObjectFacade;
import com.armedia.caliente.engine.dynamic.DynamicElementContext;
import com.armedia.caliente.engine.dynamic.ProcessingCompletedException;
import com.armedia.caliente.engine.dynamic.metadata.ExternalMetadataLoader;
import com.armedia.caliente.engine.dynamic.xml.Transformations;
import com.armedia.caliente.engine.dynamic.xml.XmlInstanceException;
import com.armedia.caliente.engine.dynamic.xml.XmlInstances;
import com.armedia.caliente.store.CmfObject;
import com.armedia.caliente.store.CmfStorageException;
import com.armedia.caliente.store.CmfTransformer;
import com.armedia.caliente.store.CmfValue;
import com.armedia.caliente.store.CmfValueMapper;

public class Transformer implements CmfTransformer {

	private static final XmlInstances<Transformations> INSTANCES = new XmlInstances<>(Transformations.class);

	private final Transformations transformations;
	private final ExternalMetadataLoader metadataLoader;

	public Transformer(String location, ExternalMetadataLoader metadataLoader) throws TransformationException {
		try {
			this.transformations = Transformer.INSTANCES.getInstance(location);
		} catch (XmlInstanceException e) {
			String pre = "";
			String post = "";
			if (location == null) {
				pre = "default ";
			} else {
				post = String.format(" from [%s]", location);
			}
			throw new TransformationException(
				String.format("Failed to load the %stransformation configuration%s", pre, post), e);
		}
		this.metadataLoader = metadataLoader;
	}

	private DynamicElementContext createContext(CmfValueMapper mapper, CmfObject<CmfValue> object) {
		return new DynamicElementContext(object, new DefaultTransformableObjectFacade(object), mapper, this.metadataLoader);
	}

	@Override
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
		} catch (ActionException | TransformationException e) {
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

	@Override
	public void close() {
		if (this.metadataLoader != null) {
			this.metadataLoader.close();
		}
	}

}