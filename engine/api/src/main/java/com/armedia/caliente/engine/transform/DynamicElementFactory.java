package com.armedia.caliente.engine.transform;

public interface DynamicElementFactory<E> {

	public E acquireInstance() throws Exception;

	public void releaseInstance(E e);

}