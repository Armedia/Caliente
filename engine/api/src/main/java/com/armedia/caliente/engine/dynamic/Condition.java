package com.armedia.caliente.engine.dynamic;

import javax.xml.bind.annotation.XmlTransient;

import com.armedia.commons.utilities.function.CheckedPredicate;

@XmlTransient
@FunctionalInterface
public interface Condition extends CheckedPredicate<DynamicElementContext, ConditionException> {

	public boolean check(DynamicElementContext ctx) throws ConditionException;

	@Override
	public default boolean testChecked(DynamicElementContext ctx) throws ConditionException {
		return check(ctx);
	}
}