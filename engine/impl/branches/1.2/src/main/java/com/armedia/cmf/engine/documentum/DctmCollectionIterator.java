/**
 *
 */

package com.armedia.cmf.engine.documentum;

import java.util.Iterator;
import java.util.NoSuchElementException;

import com.documentum.fc.client.IDfCollection;
import com.documentum.fc.client.IDfTypedObject;
import com.documentum.fc.common.DfException;

/**
 * @author diego
 *
 */
public class DctmCollectionIterator implements Iterator<IDfTypedObject> {

	private final IDfCollection collection;
	private IDfTypedObject current = null;

	public DctmCollectionIterator(IDfCollection collection) {
		this.collection = collection;
	}

	private IDfTypedObject findNext() {
		if (this.current == null) {
			try {
				if (this.collection.next()) {
					this.current = this.collection.getTypedObject();
				}
			} catch (DfException e) {
				throw new RuntimeException("Failed to find the next IDfTypedObject", e);
			}
		}
		return this.current;
	}

	@Override
	public boolean hasNext() {
		return (findNext() != null);
	}

	@Override
	public IDfTypedObject next() {
		IDfTypedObject ret = findNext();
		this.current = null;
		if (ret == null) { throw new NoSuchElementException(); }
		return ret;
	}

	@Override
	public void remove() {
		throw new UnsupportedOperationException("remove() is not supported for this class");
	}
}