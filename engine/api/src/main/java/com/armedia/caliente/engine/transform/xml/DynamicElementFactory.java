package com.armedia.caliente.engine.transform.xml;

public interface DynamicElementFactory<E> {

	public <V> E acquireInstance() throws Exception;

	public void releaseInstance(E e);

}