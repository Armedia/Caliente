package com.armedia.caliente.engine.ucm;

import java.util.Iterator;

import org.apache.chemistry.opencmis.client.api.ItemIterable;

public final class CmisPagingTransformerIterator<S, T> implements Iterator<T> {

	private final CmisPagingIterator<S> it;
	private final CmisResultTransformer<S, T> transformer;

	public CmisPagingTransformerIterator(ItemIterable<S> results, CmisResultTransformer<S, T> transformer) {
		if (transformer == null) { throw new IllegalArgumentException("Must provide a transformer"); }
		this.transformer = transformer;
		this.it = new CmisPagingIterator<>(results);
	}

	@Override
	public boolean hasNext() {
		return this.it.hasNext();
	}

	@Override
	public T next() {
		try {
			return this.transformer.transform(this.it.next());
		} catch (Exception e) {
			throw new RuntimeException(String.format("Failed to transform query result #%d", this.it.getCount()), e);
		}
	}

	@Override
	public void remove() {
		this.it.remove();
	}
}