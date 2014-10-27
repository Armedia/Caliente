/**
 *
 */

package com.delta.cmsmf.cms;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;

import com.armedia.cmf.storage.ContentStreamStore;
import com.armedia.cmf.storage.ObjectStorageTranslator;
import com.armedia.cmf.storage.ObjectStore;
import com.armedia.cmf.storage.StoredAttributeMapper;
import com.armedia.cmf.storage.StoredObjectHandler;
import com.armedia.cmf.storage.StoredObjectType;
import com.delta.cmsmf.exception.CMSMFException;
import com.documentum.fc.client.IDfSession;
import com.documentum.fc.common.IDfValue;

/**
 * @author Diego Rivera &lt;diego.rivera@armedia.com&gt;
 *
 */
public class DefaultTransferContext implements DctmTransferContext {

	private final String rootId;
	private final IDfSession session;
	private final ObjectStore objectStore;
	private final StoredAttributeMapper mapper;
	private final Map<String, IDfValue> values = new HashMap<String, IDfValue>();
	private final ContentStreamStore fileSystem;
	private final Logger output;

	public DefaultTransferContext(String rootId, IDfSession session, ObjectStore objectStore,
		ContentStreamStore fileSystem, Logger output) {
		this(rootId, session, objectStore, fileSystem, null, output);
	}

	public DefaultTransferContext(String rootId, IDfSession session, ObjectStore objectStore,
		ContentStreamStore fileSystem, StoredAttributeMapper mapper, Logger output) {
		this.rootId = rootId;
		this.session = session;
		this.objectStore = objectStore;
		this.mapper = (mapper != null ? mapper : objectStore.getAttributeMapper());
		this.fileSystem = fileSystem;
		this.output = output;
	}

	@Override
	public String getRootObjectId() {
		return this.rootId;
	}

	@Override
	public IDfSession getSession() {
		return this.session;
	}

	@Override
	public StoredAttributeMapper getAttributeMapper() {
		return this.mapper;
	}

	private void assertValidName(String name) {
		if (name == null) { throw new IllegalArgumentException("Must provide a value name"); }
	}

	@Override
	public IDfValue getValue(String name) {
		assertValidName(name);
		return this.values.get(name);
	}

	@Override
	public IDfValue setValue(String name, IDfValue value) {
		assertValidName(name);
		if (value == null) { return clearValue(name); }
		return this.values.put(name, value);
	}

	@Override
	public IDfValue clearValue(String name) {
		assertValidName(name);
		return this.values.remove(name);
	}

	@Override
	public boolean hasValue(String name) {
		assertValidName(name);
		return this.values.containsKey(name);
	}

	@Override
	public void deserializeObjects(Class<? extends DctmPersistentObject<?>> klass, Set<String> ids,
		StoredObjectHandler<IDfValue> handler) throws CMSMFException {
		ObjectStorageTranslator<IDfValue> translator = null;
		StoredObjectType type = DctmObjectType.decodeFromClass(klass).getCmsType();
		try {
			this.objectStore.loadObjects(translator, type, ids, handler);
		} catch (Exception e) {
			throw new CMSMFException(String.format("Failed to load objects of type [%s] in set %s", type, ids), e);
		}
	}

	@Override
	public ContentStreamStore getContentStreamStore() {
		return this.fileSystem;
	}

	@Override
	public void printf(String format, Object... args) {
		if (this.output != null) {
			this.output.info(String.format(format, args));
		}
	}
}