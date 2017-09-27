package com.armedia.caliente.engine.transform;

public interface DynamicElementFactory<E> {

	public E acquireInstance() throws Exception;

	public Class<? extends E> getInstanceClass();

	public void releaseInstance(E e);

}