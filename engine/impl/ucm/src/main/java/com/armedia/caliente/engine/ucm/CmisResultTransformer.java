package com.armedia.caliente.engine.ucm;

public interface CmisResultTransformer<S, E> {

	public E transform(S result) throws Exception;

}