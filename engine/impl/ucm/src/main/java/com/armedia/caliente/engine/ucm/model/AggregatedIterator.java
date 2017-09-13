package com.armedia.caliente.engine.ucm.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;

public class AggregatedIterator<O> implements Iterator<O> {

	private final Iterator<Collection<O>> master;
	private Iterator<O> current = null;
	private boolean removed = true;

	@SafeVarargs
	public AggregatedIterator(Collection<O>... collections) {
		Collection<Collection<O>> c = new ArrayList<>();
		for (Collection<O> C : collections) {
			if ((C == null) || C.isEmpty()) {
				continue;
			}
			c.add(C);
		}
		this.master = c.iterator();
	}

	@Override
	public boolean hasNext() {
		// If we're still iterating over the same collection, then simply
		// short-circuit the check
		if ((this.current != null) && this.current.hasNext()) { return true; }

		// The current collection is done, or we haven't started yet...

		// If there are no more collections to iterate over, we return false
		if (!this.master.hasNext()) { return false; }

		// Select the next collection available...
		this.current = this.master.next().iterator();
		return this.current.hasNext();
	}

	@Override
	public O next() {
		if (!hasNext()) { throw new NoSuchElementException(); }
		O ret = this.current.next();
		this.removed = false;
		return ret;
	}

	@Override
	public void remove() {
		if (this.removed) { throw new IllegalStateException(
			"The previous element has already been removed, or next() has not yet been called"); }
		this.removed = true;
		this.current.remove();
	}
}