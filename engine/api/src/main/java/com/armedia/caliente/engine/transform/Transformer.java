package com.armedia.caliente.engine.transform;

import com.armedia.caliente.engine.transform.xml.RuntimeTransformationException;
import com.armedia.caliente.store.CmfObject;

public class Transformer {

	public <V> CmfObject<V> transform(CmfObject<V> object, TransformationContext<V> ctx)
		throws RuntimeTransformationException {
		// Do nothing, for now...
		return object;
	}

	// TODO: Come up with a factory pattern that allows transformer instances to be cached so they
	// don't have to be un-marshaled over and over again
}