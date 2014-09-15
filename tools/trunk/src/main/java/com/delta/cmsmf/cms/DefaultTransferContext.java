/**
 *
 */

package com.delta.cmsmf.cms;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.delta.cmsmf.cms.storage.CmsObjectStore;
import com.delta.cmsmf.cms.storage.CmsObjectStore.ObjectHandler;
import com.delta.cmsmf.exception.CMSMFException;
import com.documentum.fc.client.IDfSession;
import com.documentum.fc.common.IDfValue;

/**
 * @author Diego Rivera <diego.rivera@armedia.com>
 *
 */
public class DefaultTransferContext implements CmsTransferContext {

	private final String rootId;
	private final IDfSession session;
	private final CmsObjectStore objectStore;
	private final CmsAttributeMapper mapper;
	private final Map<String, IDfValue> values = new HashMap<String, IDfValue>();

	public DefaultTransferContext(String rootId, IDfSession session, CmsObjectStore objectStore) {
		this(rootId, session, objectStore, null);
	}

	public DefaultTransferContext(String rootId, IDfSession session, CmsObjectStore objectStore,
		CmsAttributeMapper mapper) {
		this.rootId = rootId;
		this.session = session;
		this.objectStore = objectStore;
		this.mapper = (mapper != null ? mapper : objectStore.getAttributeMapper());
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
	public CmsAttributeMapper getAttributeMapper() {
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
	public void deserializeObjects(CmsObjectType type, Set<String> ids, ObjectHandler handler) throws CMSMFException {
		this.objectStore.deserializeObjects(type, ids, handler);
	}
}