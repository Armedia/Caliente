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