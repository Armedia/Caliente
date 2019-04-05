/**
 *
 */

package com.armedia.caliente.tools.dfc;

import com.armedia.commons.utilities.CloseableIterator;
import com.documentum.fc.client.IDfCollection;
import com.documentum.fc.client.IDfTypedObject;
import com.documentum.fc.common.DfException;

/**
 * @author diego
 *
 */
public class DctmCollectionIterator extends CloseableIterator<IDfTypedObject> {

	private final IDfCollection collection;

	public DctmCollectionIterator(IDfCollection collection) {
		this.collection = collection;
	}

	@Override
	protected CloseableIterator<IDfTypedObject>.Result findNext() throws Exception {
		if (!this.collection.next()) { return null; }
		return found(this.collection.getTypedObject());
	}

	@Override
	protected void doClose() throws DfException {
		this.collection.close();
	}
}