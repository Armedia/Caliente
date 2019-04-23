package com.armedia.caliente.engine.dynamic;

import javax.xml.bind.annotation.XmlTransient;

import com.armedia.commons.utilities.function.CheckedConsumer;

@XmlTransient
@FunctionalInterface
public interface Action extends CheckedConsumer<DynamicElementContext, ActionException> {

	public void apply(DynamicElementContext ctx) throws ActionException;

	@Override
	public default void acceptChecked(DynamicElementContext ctx) throws ActionException {
		apply(ctx);
	}
}