package com.armedia.caliente.engine.cmis;

import org.apache.chemistry.opencmis.client.api.ItemIterable;

public class CmisPagingIterable<E> implements Iterable<E> {

	private final ItemIterable<E> results;

	public CmisPagingIterable(ItemIterable<E> results) {
		this.results = results;
	}

	public final ItemIterable<E> getResults() {
		return this.results;
	}

	@Override
	public CmisPagingIterator<E> iterator() {
		return new CmisPagingIterator<>(this.results);
	}
}