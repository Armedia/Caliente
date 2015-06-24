package com.armedia.cmf.storage.tools;

import java.util.BitSet;
import java.util.Collection;
import java.util.LinkedList;

import com.armedia.cmf.storage.CmfObject;
import com.armedia.cmf.storage.CmfObjectHandler;
import com.armedia.cmf.storage.CmfStorageException;

public class CollectionObjectHandler<V> implements CmfObjectHandler<V> {

	protected static final boolean DEF_NEW_BATCH = true;
	protected static final boolean DEF_HANDLE_OBJECT = true;
	protected static final boolean DEF_HANDLE_EXCEPTION = false;
	protected static final boolean DEF_CLOSE_BATCH = true;

	public static final int NEW_BATCH = 0;
	public static final int HANDLE_OBJECT = 1;
	public static final int HANDLE_EXCEPTION = 2;
	public static final int CLOSE_BATCH = 3;

	protected final boolean retNewBatch;
	protected final boolean retHandleException;
	protected final boolean retCloseBatch;

	private final Collection<CmfObject<V>> objects;

	public CollectionObjectHandler(BitSet flags) {
		this(flags, null);
	}

	public CollectionObjectHandler(BitSet flags, Collection<CmfObject<V>> objects) {
		if (flags == null) {
			this.retNewBatch = CollectionObjectHandler.DEF_NEW_BATCH;
			this.retHandleException = CollectionObjectHandler.DEF_HANDLE_EXCEPTION;
			this.retCloseBatch = CollectionObjectHandler.DEF_CLOSE_BATCH;
		} else {
			this.retNewBatch = flags.get(CollectionObjectHandler.NEW_BATCH);
			this.retHandleException = flags.get(CollectionObjectHandler.HANDLE_EXCEPTION);
			this.retCloseBatch = flags.get(CollectionObjectHandler.CLOSE_BATCH);
		}
		if (objects == null) {
			objects = new LinkedList<CmfObject<V>>();
		}
		this.objects = objects;
	}

	public CollectionObjectHandler() {
		this(CollectionObjectHandler.DEF_NEW_BATCH, CollectionObjectHandler.DEF_HANDLE_EXCEPTION,
			CollectionObjectHandler.DEF_CLOSE_BATCH, null);
	}

	public CollectionObjectHandler(boolean newBatch) {
		this(newBatch, CollectionObjectHandler.DEF_HANDLE_EXCEPTION, CollectionObjectHandler.DEF_CLOSE_BATCH, null);
	}

	public CollectionObjectHandler(boolean newBatch, boolean handleException) {
		this(newBatch, handleException, CollectionObjectHandler.DEF_CLOSE_BATCH, null);
	}

	public CollectionObjectHandler(boolean newBatch, boolean handleException, boolean closeBatch) {
		this(newBatch, handleException, closeBatch, null);
	}

	public CollectionObjectHandler(Collection<CmfObject<V>> objects) {
		this(CollectionObjectHandler.DEF_NEW_BATCH, CollectionObjectHandler.DEF_HANDLE_EXCEPTION,
			CollectionObjectHandler.DEF_CLOSE_BATCH, objects);
	}

	public CollectionObjectHandler(boolean newBatch, Collection<CmfObject<V>> objects) {
		this(newBatch, CollectionObjectHandler.DEF_HANDLE_EXCEPTION, CollectionObjectHandler.DEF_CLOSE_BATCH, objects);
	}

	public CollectionObjectHandler(boolean newBatch, boolean handleException, Collection<CmfObject<V>> objects) {
		this(newBatch, handleException, CollectionObjectHandler.DEF_CLOSE_BATCH, objects);
	}

	public CollectionObjectHandler(boolean newBatch, boolean handleException, boolean closeBatch,
		Collection<CmfObject<V>> objects) {
		this.retNewBatch = newBatch;
		this.retHandleException = handleException;
		this.retCloseBatch = closeBatch;
		if (objects == null) {
			objects = new LinkedList<CmfObject<V>>();
		}
		this.objects = objects;
	}

	@Override
	public boolean newBatch(String batchId) throws CmfStorageException {
		return this.retNewBatch;
	}

	@Override
	public boolean handleObject(CmfObject<V> dataObject) throws CmfStorageException {
		this.objects.add(dataObject);
		return true;
	}

	@Override
	public boolean handleException(Exception e) {
		return this.retHandleException;
	}

	@Override
	public boolean closeBatch(boolean ok) throws CmfStorageException {
		return this.retCloseBatch;
	}

	public Collection<CmfObject<V>> getObjects() {
		return this.objects;
	}
}