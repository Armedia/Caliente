package com.armedia.cmf.engine.tools;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public enum BooleanOperator {

	//
	AND {
		@Override
		public <C extends BooleanContext> boolean evaluate(Collection<BooleanExpression<C>> terms, C c) {
			for (BooleanExpression<C> e : terms) {
				if (!e.evaluate(c)) { return false; }
			}
			return true;
		}
	},
	XOR {
		@Override
		public <C extends BooleanContext> boolean evaluate(Collection<BooleanExpression<C>> terms, C c) {
			int count = 0;
			for (BooleanExpression<C> e : terms) {
				if (e.evaluate(c)) {
					count++;
				}
			}
			return ((count % 2) != 0);
		}
	},
	OR {
		@Override
		public <C extends BooleanContext> boolean evaluate(Collection<BooleanExpression<C>> terms, C c) {
			for (BooleanExpression<C> e : terms) {
				if (e.evaluate(c)) { return true; }
			}
			return false;
		}
	},
	//
	;

	public abstract <C extends BooleanContext> boolean evaluate(Collection<BooleanExpression<C>> terms, C c);

	public final <C extends BooleanContext> BooleanExpression<C> construct(Collection<BooleanExpression<C>> terms) {
		return new Group<C>(this, terms);
	}

	public final <C extends BooleanContext> BooleanExpression<C> construct(BooleanExpression<C> a,
		BooleanExpression<C> b, BooleanExpression<C>... c) {
		if (a == null) { throw new IllegalArgumentException("Must provide the first term"); }
		if (b == null) { throw new IllegalArgumentException("Must provide the second term"); }
		List<BooleanExpression<C>> terms = new ArrayList<BooleanExpression<C>>();
		terms.add(a);
		terms.add(b);
		if (c != null) {
			for (BooleanExpression<C> e : c) {
				if (e == null) { throw new IllegalArgumentException("May not provide null terms"); }
				terms.add(e);
			}
		}
		return construct(terms);
	}

	private static class Group<C extends BooleanContext> implements BooleanExpression<C> {

		private final BooleanOperator op;
		protected final List<BooleanExpression<C>> terms;

		private Group(BooleanOperator op, Collection<BooleanExpression<C>> terms) {
			if (op == null) { throw new IllegalArgumentException("Must provide an operator to evaluate with"); }
			if (terms == null) { throw new IllegalArgumentException("Must provide a valid set of terms"); }
			if (terms.size() < 2) { throw new IllegalArgumentException(
				String.format("Must include at least 2 terms for %s (got %d)", op.name(), terms.size())); }
			this.op = op;
			List<BooleanExpression<C>> t = new ArrayList<BooleanExpression<C>>(terms.size());
			for (BooleanExpression<C> e : terms) {
				if (e == null) { throw new IllegalArgumentException("May not include null terms"); }
				t.add(e);
			}
			this.terms = Collections.unmodifiableList(t);
		}

		@Override
		public final boolean evaluate(C c) {
			return this.op.evaluate(this.terms, c);
		}

		@Override
		public final String toString() {
			return String.format("%s%s", this.op.name(), this.terms);
		}
	}
}