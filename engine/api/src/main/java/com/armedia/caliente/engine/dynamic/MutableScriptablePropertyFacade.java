package com.armedia.caliente.engine.dynamic;

public interface MutableScriptablePropertyFacade extends ScriptablePropertyFacade {

	@Override
	public default boolean isMutable() {
		return true;
	}
}