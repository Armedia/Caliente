package com.armedia.caliente.engine.transform;

import java.util.Set;

public interface DynamicElementFactory<E> {

	public E acquireInstance(String classNameOrAlias) throws Exception;

	public Set<String> getClassNamesOrAliases();

	public void releaseInstance(E e);

}