/*******************************************************************************
 * #%L
 * Armedia Caliente
 * %%
 * Copyright (C) 2013 - 2019 Armedia, LLC
 * %%
 * This file is part of the Caliente software.
 *
 * If the software was purchased under a paid Caliente license, the terms of
 * the paid license agreement will prevail.  Otherwise, the software is
 * provided under the following open source license terms:
 *
 * Caliente is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Caliente is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Caliente. If not, see <http://www.gnu.org/licenses/>.
 * #L%
 *******************************************************************************/
package com.armedia.caliente.engine.cmis;

import java.util.Iterator;
import java.util.NoSuchElementException;

import org.apache.chemistry.opencmis.client.api.ItemIterable;

public class CmisPagingIterator<E> implements Iterator<E> {

	/**
	 * The set of pages to iterate over
	 */
	private final ItemIterable<E> results;

	/**
	 * The current page beinge iterated over
	 */
	private ItemIterable<E> currentPage = null;

	/**
	 * The current delegate iterator
	 */
	private Iterator<E> it = null;

	/**
	 * the number of elements that have been successfully returned by this iterator's next() method
	 */
	private int count = 0;

	public CmisPagingIterator(ItemIterable<E> results) {
		this.results = results;
		if (results != null) {
			this.currentPage = this.results.skipTo(0).getPage();
			this.it = this.currentPage.iterator();
		}
	}

	public final ItemIterable<E> getResults() {
		return this.results;
	}

	public final int getCount() {
		return this.count;
	}

	protected boolean accept(E next) {
		return true;
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
	public E next() {
		if (!hasNext()) { throw new NoSuchElementException(); }
		E ret = this.it.next();
		this.count++;
		return ret;
	}

	@Override
	public void remove() {
		this.it.remove();
	}
}