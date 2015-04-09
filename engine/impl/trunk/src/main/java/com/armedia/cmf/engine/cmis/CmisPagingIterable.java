package com.armedia.cmf.engine.cmis;

import org.apache.chemistry.opencmis.client.api.ItemIterable;
import org.apache.chemistry.opencmis.client.api.QueryResult;

public class CmisPagingIterable implements Iterable<QueryResult> {

	private final ItemIterable<QueryResult> results;

	public CmisPagingIterable(ItemIterable<QueryResult> results) {
		this.results = results;
	}

	public final ItemIterable<QueryResult> getResults() {
		return this.results;
	}

	@Override
	public CmisPagingIterator iterator() {
		return new CmisPagingIterator(this.results);
	}
}