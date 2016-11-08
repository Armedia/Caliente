package com.armedia.caliente.store.tools;

import java.util.BitSet;

import com.armedia.caliente.store.CmfObject;
import com.armedia.caliente.store.CmfObjectHandler;
import com.armedia.caliente.store.CmfStorageException;

public abstract class DefaultCmfObjectHandler<V> implements CmfObjectHandler<V> {

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
	protected final boolean retHandleObject;
	protected final boolean retCloseBatch;

	public DefaultCmfObjectHandler(BitSet flags) {
		if (flags == null) {
			this.retNewBatch = DefaultCmfObjectHandler.DEF_NEW_BATCH;
			this.retHandleObject = DefaultCmfObjectHandler.DEF_HANDLE_OBJECT;
			this.retHandleException = DefaultCmfObjectHandler.DEF_HANDLE_EXCEPTION;
			this.retCloseBatch = DefaultCmfObjectHandler.DEF_CLOSE_BATCH;
		} else {
			this.retNewBatch = flags.get(DefaultCmfObjectHandler.NEW_BATCH);
			this.retHandleObject = flags.get(DefaultCmfObjectHandler.HANDLE_OBJECT);
			this.retHandleException = flags.get(DefaultCmfObjectHandler.HANDLE_EXCEPTION);
			this.retCloseBatch = flags.get(DefaultCmfObjectHandler.CLOSE_BATCH);
		}
	}

	public DefaultCmfObjectHandler() {
		this(DefaultCmfObjectHandler.DEF_NEW_BATCH, DefaultCmfObjectHandler.DEF_HANDLE_OBJECT,
			DefaultCmfObjectHandler.DEF_HANDLE_EXCEPTION, DefaultCmfObjectHandler.DEF_CLOSE_BATCH);
	}

	public DefaultCmfObjectHandler(boolean newBatch) {
		this(newBatch, DefaultCmfObjectHandler.DEF_HANDLE_OBJECT, DefaultCmfObjectHandler.DEF_HANDLE_EXCEPTION,
			DefaultCmfObjectHandler.DEF_CLOSE_BATCH);
	}

	public DefaultCmfObjectHandler(boolean newBatch, boolean handleObject) {
		this(newBatch, handleObject, DefaultCmfObjectHandler.DEF_HANDLE_EXCEPTION,
			DefaultCmfObjectHandler.DEF_CLOSE_BATCH);
	}

	public DefaultCmfObjectHandler(boolean newBatch, boolean handleObject, boolean handleException) {
		this(newBatch, handleObject, handleException, DefaultCmfObjectHandler.DEF_CLOSE_BATCH);
	}

	public DefaultCmfObjectHandler(boolean newBatch, boolean handleObject, boolean handleException, boolean closeBatch) {
		this.retNewBatch = newBatch;
		this.retHandleObject = handleObject;
		this.retHandleException = handleException;
		this.retCloseBatch = closeBatch;
	}

	@Override
	public boolean newBatch(String batchId) throws CmfStorageException {
		return this.retNewBatch;
	}

	@Override
	public boolean handleObject(CmfObject<V> dataObject) throws CmfStorageException {
		return this.retHandleObject;
	}

	@Override
	public boolean handleException(Exception e) {
		return this.retHandleException;
	}

	@Override
	public boolean closeBatch(boolean ok) throws CmfStorageException {
		return this.retCloseBatch;
	}
}