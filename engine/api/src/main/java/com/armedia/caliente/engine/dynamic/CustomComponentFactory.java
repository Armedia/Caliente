package com.armedia.caliente.engine.dynamic;

import java.util.Set;

public interface CustomComponentFactory<E> {

	public E acquireInstance(String classNameOrAlias) throws Exception;

	public Set<String> getClassNamesOrAliases();

	public void releaseInstance(E e);

}