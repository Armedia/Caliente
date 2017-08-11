package com.armedia.caliente.engine.xds;

import java.util.Iterator;

public final class CmisTransformerIterator<S, T> implements Iterator<T> {

	private final Iterator<S> it;
	private final CmisResultTransformer<S, T> transformer;
	private long current = 0;

	public CmisTransformerIterator(Iterator<S> results, CmisResultTransformer<S, T> transformer) {
		if (transformer == null) { throw new IllegalArgumentException("Must provide a transformer"); }
		this.transformer = transformer;
		this.it = results;
	}

	@Override
	public boolean hasNext() {
		return this.it.hasNext();
	}

	@Override
	public T next() {
		try {
			this.current++;
			return this.transformer.transform(this.it.next());
		} catch (Exception e) {
			throw new RuntimeException(String.format("Failed to transform query result #%d", this.current), e);
		}
	}

	@Override
	public void remove() {
		this.it.remove();
	}
}