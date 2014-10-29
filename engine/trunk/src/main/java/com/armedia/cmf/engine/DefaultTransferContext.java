/**
 *
 */

package com.armedia.cmf.engine;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;

import com.armedia.cmf.storage.ContentStreamStore;
import com.armedia.cmf.storage.ObjectStorageTranslator;
import com.armedia.cmf.storage.ObjectStore;
import com.armedia.cmf.storage.StorageException;
import com.armedia.cmf.storage.StoredAttributeMapper;
import com.armedia.cmf.storage.StoredObjectHandler;
import com.armedia.cmf.storage.StoredObjectType;
import com.armedia.cmf.storage.StoredValueDecoderException;

/**
 * @author Diego Rivera &lt;diego.rivera@armedia.com&gt;
 *
 */
public abstract class DefaultTransferContext<T, S, V> implements TransferContext<S, V> {

	private final ObjectStorageTranslator<T, V> translator;
	private final String rootId;
	private final S session;
	private final ObjectStore objectStore;
	private final StoredAttributeMapper mapper;
	private final Map<String, V> values = new HashMap<String, V>();
	private final ContentStreamStore fileSystem;
	private final Logger output;

	protected DefaultTransferContext(ObjectStorageTranslator<T, V> translator, String rootId, S session,
		ObjectStore objectStore, ContentStreamStore fileSystem, Logger output) {
		this(translator, rootId, session, objectStore, fileSystem, null, output);
	}

	protected DefaultTransferContext(ObjectStorageTranslator<T, V> translator, String rootId, S session,
		ObjectStore objectStore, ContentStreamStore fileSystem, StoredAttributeMapper mapper, Logger output) {
		this.translator = translator;
		this.rootId = rootId;
		this.session = session;
		this.objectStore = objectStore;
		this.mapper = (mapper != null ? mapper : objectStore.getAttributeMapper());
		this.fileSystem = fileSystem;
		this.output = output;
	}

	@Override
	public final String getRootObjectId() {
		return this.rootId;
	}

	@Override
	public final S getSession() {
		return this.session;
	}

	@Override
	public final StoredAttributeMapper getAttributeMapper() {
		return this.mapper;
	}

	private void assertValidName(String name) {
		if (name == null) { throw new IllegalArgumentException("Must provide a value name"); }
	}

	@Override
	public final V getValue(String name) {
		assertValidName(name);
		return this.values.get(name);
	}

	@Override
	public final V setValue(String name, V value) {
		assertValidName(name);
		if (value == null) { return clearValue(name); }
		return this.values.put(name, value);
	}

	@Override
	public final V clearValue(String name) {
		assertValidName(name);
		return this.values.remove(name);
	}

	@Override
	public final boolean hasValue(String name) {
		assertValidName(name);
		return this.values.containsKey(name);
	}

	@Override
	public final int loadObjects(StoredObjectType type, Set<String> ids, StoredObjectHandler<V> handler)
		throws StorageException, StoredValueDecoderException {
		return this.objectStore.loadObjects(this.translator, type, ids, handler);
	}

	@Override
	public final ContentStreamStore getContentStreamStore() {
		return this.fileSystem;
	}

	@Override
	public final void printf(String format, Object... args) {
		if (this.output != null) {
			this.output.info(String.format(format, args));
		}
	}
}