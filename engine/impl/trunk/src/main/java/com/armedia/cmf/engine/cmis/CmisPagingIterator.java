package com.armedia.cmf.engine.cmis;

import java.util.Iterator;
import java.util.NoSuchElementException;

import org.apache.chemistry.opencmis.client.api.ItemIterable;
import org.apache.chemistry.opencmis.client.api.QueryResult;

public class CmisPagingIterator implements Iterator<QueryResult> {

	/**
	 * The set of pages to iterate over
	 */
	private final ItemIterable<QueryResult> results;

	/**
	 * The current page beinge iterated over
	 */
	private ItemIterable<QueryResult> currentPage = null;

	/**
	 * The current delegate iterator
	 */
	private Iterator<QueryResult> it = null;

	/**
	 * the number of elements that have been successfully returned by this iterator's next() method
	 */
	private int count = 0;

	public CmisPagingIterator(ItemIterable<QueryResult> results) {
		this.results = results;
		if (results != null) {
			this.currentPage = this.results.skipTo(0).getPage();
			this.it = this.currentPage.iterator();
		}
	}

	public final ItemIterable<QueryResult> getResults() {
		return this.results;
	}

	public final int getCount() {
		return this.count;
	}

	protected boolean terminate() {
		this.currentPage = null;
		this.it = null;
		return false;
	}

	@Override
	public boolean hasNext() {
		if (this.currentPage == null) { return false; }
		if (this.it.hasNext()) { return true; }

		// If the current iterator is exhausted, we seek the next page.
		if (!this.currentPage.getHasMoreItems()) { return terminate(); }

		this.currentPage = this.results.skipTo(this.count).getPage();
		this.it = this.currentPage.iterator();
		if (this.it.hasNext()) { return true; }

		// If the iterator has no more items, then this page must have no more
		// items, so the query results must be over... the question is: how'd we
		// get this far? Doesn't really matter...just clean up for exit and so that
		// the code more easily doesn't trip over this
		return terminate();
	}

	@Override
	public QueryResult next() {
		if (!hasNext()) { throw new NoSuchElementException(); }
		QueryResult ret = this.it.next();
		this.count++;
		return ret;
	}

	@Override
	public void remove() {
		this.it.remove();
	}
}