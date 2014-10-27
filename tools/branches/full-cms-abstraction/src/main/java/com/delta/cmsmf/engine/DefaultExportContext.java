/**
 *
 */

package com.delta.cmsmf.engine;

import org.apache.log4j.Logger;

import com.armedia.cmf.storage.ContentStreamStore;
import com.armedia.cmf.storage.ObjectStore;
import com.armedia.cmf.storage.StoredAttributeMapper;
import com.armedia.cmf.storage.StoredObject;
import com.armedia.cmf.storage.StoredObjectType;
import com.delta.cmsmf.cms.DctmExportContext;
import com.delta.cmsmf.cms.DctmExportListener;
import com.delta.cmsmf.cms.DefaultTransferContext;
import com.documentum.fc.client.IDfSession;

/**
 * @author Diego Rivera &lt;diego.rivera@armedia.com&gt;
 *
 */
public class DefaultExportContext extends DefaultTransferContext implements DctmExportContext {

	private final DctmExportListener listener;

	/**
	 * @param rootId
	 * @param session
	 * @param objectStore
	 * @param fileSystem
	 * @param output
	 */
	public DefaultExportContext(String rootId, IDfSession session, ObjectStore objectStore,
		ContentStreamStore fileSystem, Logger output, DctmExportListener listener) {
		this(rootId, session, objectStore, fileSystem, null, output, listener);
	}

	/**
	 * @param rootId
	 * @param session
	 * @param objectStore
	 * @param fileSystem
	 * @param mapper
	 * @param output
	 */
	public DefaultExportContext(String rootId, IDfSession session, ObjectStore objectStore,
		ContentStreamStore fileSystem, StoredAttributeMapper mapper, Logger output, DctmExportListener listener) {
		super(rootId, session, objectStore, fileSystem, mapper, output);
		this.listener = listener;
	}

	@Override
	public void objectExportStarted(StoredObjectType objectType, String objectId) {
		if (this.listener != null) {
			this.listener.objectExportStarted(objectType, objectId);
		}
	}

	@Override
	public void objectExportCompleted(StoredObject object) {
		if (this.listener != null) {
			this.listener.objectExportCompleted(object);
		}
	}

	@Override
	public void objectSkipped(StoredObjectType objectType, String objectId) {
		if (this.listener != null) {
			this.listener.objectSkipped(objectType, objectId);
		}
	}

	@Override
	public void objectExportFailed(StoredObjectType objectType, String objectId, Throwable thrown) {
		if (this.listener != null) {
			this.listener.objectExportFailed(objectType, objectId, thrown);
		}
	}
}