package com.armedia.caliente.engine.dynamic;

public interface Action {

	public void apply(ObjectContext ctx) throws ActionException;

}