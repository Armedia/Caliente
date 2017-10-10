package com.armedia.caliente.engine.transform;

import com.armedia.caliente.engine.xml.Transformations;
import com.armedia.caliente.engine.xml.XmlInstanceException;
import com.armedia.caliente.engine.xml.XmlInstances;
import com.armedia.caliente.store.CmfObject;
import com.armedia.caliente.store.CmfStorageException;
import com.armedia.caliente.store.CmfTransformer;
import com.armedia.caliente.store.CmfValue;
import com.armedia.caliente.store.CmfValueMapper;

public class Transformer implements CmfTransformer {

	private static final XmlInstances<Transformations> INSTANCES = new XmlInstances<>(Transformations.class);

	private final Transformations transformations;

	public Transformer(String location) throws TransformationException {
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
	}

	private TransformationContext createContext(CmfValueMapper mapper, CmfObject<CmfValue> object) {
		return new TransformationContext(object, new DefaultTransformableObjectFacade(object), mapper);
	}

	@Override
	public CmfObject<CmfValue> transform(CmfValueMapper mapper, CmfObject<CmfValue> object) throws CmfStorageException {
		if (this.transformations == null) {
			//
			return object;
		}
		TransformationContext ctx = createContext(mapper, object);
		try {
			try {
				this.transformations.apply(ctx);
			} catch (TransformationCompletedException e) {
				// Do nothing - this is simply our shortcut for stopping the transformation work in
				// its tracks
			}

			return ctx.getTransformableObject().applyChanges(object);
		} catch (TransformationException e) {
			throw new CmfStorageException(
				String.format("Exception caught while performing the transformation for %s", object.getDescription()),
				e);
		} finally {
			destroyContext(ctx);
		}
	}

	private void destroyContext(TransformationContext ctx) {
		// Clean things out... to help the GC...
		ctx.getTransformableObject().getAtt().clear();
		ctx.getTransformableObject().getPriv().clear();
		ctx.getVariables().clear();
	}

	@Override
	public void close() {
		// Don't really need to do anything here...
	}

}