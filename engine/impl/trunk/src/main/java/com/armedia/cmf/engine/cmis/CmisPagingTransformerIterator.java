package com.armedia.cmf.engine.cmis;

import java.util.Iterator;

import org.apache.chemistry.opencmis.client.api.ItemIterable;
import org.apache.chemistry.opencmis.client.api.QueryResult;

public final class CmisPagingTransformerIterator<E> implements Iterator<E> {

	private final CmisPagingIterator it;
	private final CmisResultTransformer<E> transformer;

	public CmisPagingTransformerIterator(ItemIterable<QueryResult> results, CmisResultTransformer<E> transformer) {
		if (transformer == null) { throw new IllegalArgumentException("Must provide a transformer"); }
		this.transformer = transformer;
		this.it = new CmisPagingIterator(results);
	}

	@Override
	public boolean hasNext() {
		return this.it.hasNext();
	}

	@Override
	public E next() {
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