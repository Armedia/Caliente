package com.armedia.caliente.engine.cmis;

public interface CmisResultTransformer<S, E> {

	public E transform(S result) throws Exception;

}