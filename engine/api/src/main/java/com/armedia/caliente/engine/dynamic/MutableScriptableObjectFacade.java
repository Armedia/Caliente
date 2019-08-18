package com.armedia.caliente.engine.dynamic;

public interface MutableScriptableObjectFacade<P extends ScriptablePropertyFacade> extends ScriptableObjectFacade<P> {

	public MutableScriptableObjectFacade<P> setName(String name);

	public MutableScriptableObjectFacade<P> setSubtype(String subtype);

	@Override
	public default boolean isMutable() {
		return true;
	}
}