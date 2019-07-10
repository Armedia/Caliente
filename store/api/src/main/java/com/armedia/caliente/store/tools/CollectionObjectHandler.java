/*******************************************************************************
 * #%L
 * Armedia Caliente
 * %%
 * Copyright (c) 2010 - 2019 Armedia LLC
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
package com.armedia.caliente.store.tools;

import java.util.BitSet;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;

import com.armedia.caliente.store.CmfObject;
import com.armedia.caliente.store.CmfStorageException;

public final class CollectionObjectHandler<VALUE> extends DefaultCmfObjectHandler<VALUE>
	implements Iterable<CmfObject<VALUE>> {

	private final Collection<CmfObject<VALUE>> objects;

	public CollectionObjectHandler() {
		this(null, null);
	}

	public CollectionObjectHandler(BitSet flags) {
		this(flags, null);
	}

	public CollectionObjectHandler(Collection<CmfObject<VALUE>> objects) {
		this(null, objects);
	}

	public CollectionObjectHandler(BitSet flags, Collection<CmfObject<VALUE>> objects) {
		super(flags);
		if (objects == null) {
			objects = new LinkedList<>();
		}
		this.objects = objects;
	}

	@Override
	public boolean handleObject(CmfObject<VALUE> dataObject) throws CmfStorageException {
		this.objects.add(dataObject);
		return this.retHandleObject;
	}

	public Collection<CmfObject<VALUE>> getObjects() {
		return this.objects;
	}

	@Override
	public Iterator<CmfObject<VALUE>> iterator() {
		return this.objects.iterator();
	}
}