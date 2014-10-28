package com.armedia.cmf.engine;

import java.util.Set;

import com.armedia.cmf.storage.ContentStreamStore;
import com.armedia.cmf.storage.StorageException;
import com.armedia.cmf.storage.StoredAttributeMapper;
import com.armedia.cmf.storage.StoredObjectHandler;
import com.armedia.cmf.storage.StoredObjectType;
import com.armedia.cmf.storage.StoredValueDecoderException;

public interface TransferContext<S, V> {

	public String getRootObjectId();

	public S getSession();

	public StoredAttributeMapper getAttributeMapper();

	public V getValue(String name);

	public V setValue(String name, V value);

	public V clearValue(String name);

	public boolean hasValue(String name);

	public int loadObjects(StoredObjectType type, Set<String> ids, StoredObjectHandler<V> handler)
		throws StorageException, StoredValueDecoderException;

	public ContentStreamStore getContentStreamStore();

	public void printf(String format, Object... args);
}