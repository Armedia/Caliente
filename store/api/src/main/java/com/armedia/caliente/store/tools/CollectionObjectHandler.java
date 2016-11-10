package com.armedia.caliente.store.tools;

import java.util.BitSet;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;

import com.armedia.caliente.store.CmfObject;
import com.armedia.caliente.store.CmfStorageException;

public final class CollectionObjectHandler<V> extends DefaultCmfObjectHandler<V> implements Iterable<CmfObject<V>> {

	private final Collection<CmfObject<V>> objects;

	public CollectionObjectHandler() {
		this(null, null);
	}

	public CollectionObjectHandler(BitSet flags) {
		this(flags, null);
	}

	public CollectionObjectHandler(Collection<CmfObject<V>> objects) {
		this(null, objects);
	}

	public CollectionObjectHandler(BitSet flags, Collection<CmfObject<V>> objects) {
		super(flags);
		if (objects == null) {
			objects = new LinkedList<>();
		}
		this.objects = objects;
	}

	@Override
	public boolean handleObject(CmfObject<V> dataObject) throws CmfStorageException {
		this.objects.add(dataObject);
		return this.retHandleObject;
	}

	public Collection<CmfObject<V>> getObjects() {
		return this.objects;
	}

	@Override
	public Iterator<CmfObject<V>> iterator() {
		return this.objects.iterator();
	}
}