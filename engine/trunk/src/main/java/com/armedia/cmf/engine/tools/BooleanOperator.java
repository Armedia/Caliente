package com.armedia.cmf.engine.tools;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public enum BooleanOperator {

	//
	AND(2) {
		@Override
		public boolean evaluate(Collection<BooleanExpression> terms, BooleanContext c) {
			for (BooleanExpression e : terms) {
				if (!e.evaluate(c)) { return false; }
			}
			return true;
		}
	},
	XOR(2) {
		@Override
		public boolean evaluate(Collection<BooleanExpression> terms, BooleanContext c) {
			int count = 0;
			for (BooleanExpression e : terms) {
				if (e.evaluate(c)) {
					count++;
				}
			}
			return ((count % 2) != 0);
		}
	},
	OR(2) {
		@Override
		public boolean evaluate(Collection<BooleanExpression> terms, BooleanContext c) {
			for (BooleanExpression e : terms) {
				if (e.evaluate(c)) { return true; }
			}
			return false;
		}
	},
	//
	;

	private final int min;

	private BooleanOperator(int min) {
		this.min = Math.max(1, min);
	}

	public abstract boolean evaluate(Collection<BooleanExpression> terms, BooleanContext c);

	public final BooleanExpression construct(Collection<BooleanExpression> terms) {
		return new Group(this, terms);
	}

	public final BooleanExpression construct(BooleanExpression a, BooleanExpression b, BooleanExpression... c) {
		if (a == null) { throw new IllegalArgumentException("Must provide the first term"); }
		if (b == null) { throw new IllegalArgumentException("Must provide the second term"); }
		List<BooleanExpression> terms = new ArrayList<BooleanExpression>();
		terms.add(a);
		terms.add(b);
		if (c != null) {
			for (BooleanExpression e : c) {
				if (e == null) { throw new IllegalArgumentException("May not provide null terms"); }
				terms.add(e);
			}
		}
		return construct(terms);
	}

	private static class Group implements BooleanExpression {

		private final BooleanOperator op;
		protected final List<BooleanExpression> terms;

		private Group(BooleanOperator op, Collection<BooleanExpression> terms) {
			if (op == null) { throw new IllegalArgumentException("Must provide an operator to evaluate with"); }
			if (terms == null) { throw new IllegalArgumentException("Must provide a valid set of terms"); }
			if (terms.size() < op.min) { throw new IllegalArgumentException(
				String.format("Must include at least %d terms for %s (got %d)", op.min, op.name(), terms.size())); }
			this.op = op;
			List<BooleanExpression> t = new ArrayList<BooleanExpression>(terms.size());
			for (BooleanExpression e : terms) {
				if (e == null) { throw new IllegalArgumentException("May not include null terms"); }
				t.add(e);
			}
			this.terms = Collections.unmodifiableList(t);
		}

		@Override
		public final boolean evaluate(BooleanContext c) {
			return this.op.evaluate(this.terms, c);
		}

		@Override
		public final String toString() {
			return String.format("%s%s", this.op.name(), this.terms);
		}
	}
}