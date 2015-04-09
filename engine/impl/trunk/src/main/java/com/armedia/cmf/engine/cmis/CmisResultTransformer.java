package com.armedia.cmf.engine.cmis;


public interface CmisResultTransformer<S, E> {

	public E transform(S result) throws Exception;

}