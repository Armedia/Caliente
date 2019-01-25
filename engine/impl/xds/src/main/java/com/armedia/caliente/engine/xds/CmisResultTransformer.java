package com.armedia.caliente.engine.xds;

@FunctionalInterface
public interface CmisResultTransformer<S, E> {

	public E transform(S result) throws Exception;

}