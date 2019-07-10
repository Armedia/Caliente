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
package com.armedia.caliente.cli.datagen.data;

import java.io.Closeable;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.armedia.commons.utilities.Tools;

public abstract class DataRecordSet<D extends Object, R extends Object, S extends Object>
	implements Iterable<DataRecord>, Iterator<DataRecord>, Closeable {

	protected final Logger log = LoggerFactory.getLogger(getClass());

	private final int loopCount;
	private final boolean requireComplete;

	private int currentLoop = 0;
	private long currentRecord = 0;
	private boolean closed = false;

	private D data = null;
	private Map<String, Integer> headers = null;
	private String[] headersArr = null;

	private Iterator<R> iterator = null;
	private DataRecord nextRecord = null;
	protected final S state;

	protected DataRecordSet(boolean requireComplete, int loopCount, S initializationState) throws Exception {
		this.requireComplete = requireComplete;
		this.currentLoop = 0;
		// Enforce the use of complete header records
		this.loopCount = (loopCount <= 0 ? loopCount = 0 : loopCount);
		this.state = initializationState;
		this.data = getData();
	}

	protected final S getState() {
		return this.state;
	}

	protected abstract D initData() throws Exception;

	protected abstract Map<String, Integer> mapColumns(D data);

	protected abstract Iterator<R> getIterator(D data);

	protected abstract DataRecord newRecord(R record);

	protected abstract void closeData(D data);

	private synchronized D getData() throws Exception {
		if (this.data == null) {
			D data = initData();
			Map<String, Integer> headerMap = mapColumns(data);
			if ((headerMap == null) || headerMap.isEmpty()) {
				throw new Exception("The data does not contain a header record");
			}
			if (this.headers == null) {
				this.headers = Tools.freezeMap(headerMap);
				this.headersArr = new String[this.headers.size()];
				for (String s : this.headers.keySet()) {
					Integer p = this.headers.get(s);
					this.headersArr[p] = s;
				}
			}
			this.data = data;
		}
		return this.data;
	}

	public final boolean isRequireComplete() {
		return this.requireComplete;
	}

	public int getLoopCount() {
		return this.loopCount;
	}

	public int getCurrentLoop() {
		return this.currentLoop;
	}

	public long getRecordNumber() {
		return this.currentRecord;
	}

	public Set<String> getColumnNames() throws Exception {
		getData();
		return this.headers.keySet();
	}

	public int getColumnCount() throws Exception {
		getData();
		return this.headers.size();
	}

	public String getColumnName(int pos) throws Exception {
		getData();
		return this.headersArr[pos];
	}

	@Override
	public Iterator<DataRecord> iterator() {
		return this;
	}

	@Override
	public synchronized void close() {
		if (this.closed) { return; }
		try {
			closeData(this.data);
		} finally {
			this.data = null;
			this.iterator = null;
			this.headers = null;
			this.closed = true;
		}
	}

	@Override
	public synchronized final boolean hasNext() {
		return hasNext(true);
	}

	private boolean hasNext(final boolean allowLoop) {
		if (this.closed) { return false; }

		// If we have a next record waiting in the wings...
		if (this.nextRecord != null) { return true; }

		// No next record waiting, so let's go look for one...
		if (this.iterator != null) {
			// Loop until we find a record that matches criteria
			// TODO: support custom criteria matchers?
			while (this.iterator.hasNext()) {
				R r = this.iterator.next();
				if (r == null) {
					continue;
				}
				DataRecord next = newRecord(r);
				if (!this.requireComplete || next.isComplete()) {
					// We've found a next record which matches our criteria, so
					// we store it and return true...
					this.nextRecord = next;
					return true;
				}
			}

			// We didn't find a matching record, so we have a problem... we'll
			// have to potentially loop around
			this.iterator = null;
		}

		// If looping is not allowed, then we fail short
		if (!allowLoop) { return false; }

		// We've reached the end...check to see if we have to loop over or not
		if ((this.loopCount <= 0) || (this.currentLoop < this.loopCount)) {
			try {
				if (this.data != null) {
					// Perform any cleanup...
					closeData(this.data);
					this.data = null;
				}
				this.data = getData();
			} catch (Exception e) {
				close();
				throw new RuntimeException("Failed to read the next record from the data", e);
			} finally {
				this.currentLoop++;
			}
			this.iterator = getIterator(this.data);
		}

		if (!hasNext(false)) {
			// There are no records...so we return false, but do NOT loop
			close();
			return false;
		}
		return true;
	}

	@Override
	public synchronized final DataRecord next() {
		if (this.closed || !hasNext()) { throw new NoSuchElementException(); }
		try {
			return this.nextRecord;
		} finally {
			// Clear out the next record, so the next invocation to hasNext() finds a new record
			this.nextRecord = null;
			this.currentRecord++;
		}
	}

	@Override
	public final void remove() {
		throw new UnsupportedOperationException("remove() is not supported for this iterator");
	}
}