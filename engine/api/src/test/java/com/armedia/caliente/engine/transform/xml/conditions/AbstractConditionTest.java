package com.armedia.caliente.engine.transform.xml.conditions;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.armedia.caliente.engine.transform.TransformationContext;
import com.armedia.caliente.engine.transform.TransformationException;
import com.armedia.caliente.engine.transform.xml.Condition;

public class AbstractConditionTest {

	protected static final Condition COND_TRUE = new Condition() {
		@Override
		public boolean check(TransformationContext ctx) throws TransformationException {
			return true;
		}
	};

	protected static final Condition COND_FALSE = new Condition() {
		@Override
		public boolean check(TransformationContext ctx) throws TransformationException {
			return false;
		}
	};

	protected static final Condition COND_FAIL = new Condition() {
		@Override
		public boolean check(TransformationContext ctx) throws TransformationException {
			throw new TransformationException("Expected failure");
		}
	};

	protected static List<Condition> convertToList(Boolean[] booleans, boolean lastIsResult) {
		Objects.requireNonNull(booleans, "Must provide a non-null array of booleans");
		if (booleans.length < 2) { throw new IllegalArgumentException(
			"The array of booleans must contain at least two elements"); }
		List<Condition> ret = new ArrayList<>(booleans.length);
		for (Boolean b : booleans) {
			ret.add( //
				(b == null) //
					? AbstractConditionTest.COND_FAIL //
					: (b //
						? AbstractConditionTest.COND_TRUE //
						: AbstractConditionTest.COND_FALSE //
					) //
			);
		}
		// The last element is the result, so remove it
		if (lastIsResult) {
			ret.remove(ret.size() - 1);
		}
		return ret;
	}
}