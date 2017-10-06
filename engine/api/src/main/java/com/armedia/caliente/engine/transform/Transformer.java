package com.armedia.caliente.engine.transform;

import com.armedia.caliente.engine.xml.Transformations;
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
		this.transformations = Transformer.INSTANCES.getInstance(location);
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