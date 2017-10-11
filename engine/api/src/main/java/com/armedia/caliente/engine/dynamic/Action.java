package com.armedia.caliente.engine.dynamic;

public interface Action {

	public void apply(DynamicElementContext ctx) throws ActionException;

}