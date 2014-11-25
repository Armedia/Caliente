/**
 *
 */

package com.armedia.cmf.documentum.engine.exporter;

import java.util.Iterator;

import com.armedia.cmf.documentum.engine.DctmCollectionIterator;
import com.documentum.fc.client.IDfCollection;
import com.documentum.fc.client.IDfTypedObject;
import com.documentum.fc.common.DfException;

/**
 * @author diego
 *
 */
public class DctmObjectIdIterator implements Iterator<String> {

	private final String idAttribute;
	private final DctmCollectionIterator iterator;
	private int current = 0;

	public DctmObjectIdIterator(IDfCollection collection) {
		this(collection, null);
	}

	public DctmObjectIdIterator(IDfCollection collection, String idAttribute) {
		this.iterator = new DctmCollectionIterator(collection);
		this.idAttribute = idAttribute;
	}

	@Override
	public boolean hasNext() {
		return this.iterator.hasNext();
	}

	@Override
	public String next() {
		IDfTypedObject next = this.iterator.next();
		this.current++;
		try {
			return next.getString(this.idAttribute);
		} catch (DfException e) {
			throw new RuntimeException(String.format("DfException caught retrieving object ID # %d", this.current), e);
		}
	}

	@Override
	public void remove() {
		this.iterator.remove();
	}
}