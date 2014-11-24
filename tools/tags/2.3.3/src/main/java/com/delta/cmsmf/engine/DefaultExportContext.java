/**
 *
 */

package com.delta.cmsmf.engine;

import org.apache.log4j.Logger;

import com.delta.cmsmf.cms.CmsAttributeMapper;
import com.delta.cmsmf.cms.CmsExportContext;
import com.delta.cmsmf.cms.CmsExportListener;
import com.delta.cmsmf.cms.CmsFileSystem;
import com.delta.cmsmf.cms.CmsObject;
import com.delta.cmsmf.cms.CmsObjectType;
import com.delta.cmsmf.cms.DefaultTransferContext;
import com.delta.cmsmf.cms.storage.CmsObjectStore;
import com.documentum.fc.client.IDfSession;

/**
 * @author Diego Rivera &lt;diego.rivera@armedia.com&gt;
 *
 */
public class DefaultExportContext extends DefaultTransferContext implements CmsExportContext {

	private final CmsExportListener listener;

	/**
	 * @param rootId
	 * @param session
	 * @param objectStore
	 * @param fileSystem
	 * @param output
	 */
	public DefaultExportContext(String rootId, IDfSession session, CmsObjectStore objectStore,
		CmsFileSystem fileSystem, Logger output, CmsExportListener listener) {
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
	public DefaultExportContext(String rootId, IDfSession session, CmsObjectStore objectStore,
		CmsFileSystem fileSystem, CmsAttributeMapper mapper, Logger output, CmsExportListener listener) {
		super(rootId, session, objectStore, fileSystem, mapper, output);
		this.listener = listener;
	}

	@Override
	public void objectExportStarted(CmsObjectType objectType, String objectId) {
		if (this.listener != null) {
			this.listener.objectExportStarted(objectType, objectId);
		}
	}

	@Override
	public void objectExportCompleted(CmsObject<?> object) {
		if (this.listener != null) {
			this.listener.objectExportCompleted(object);
		}
	}

	@Override
	public void objectSkipped(CmsObjectType objectType, String objectId) {
		if (this.listener != null) {
			this.listener.objectSkipped(objectType, objectId);
		}
	}

	@Override
	public void objectExportFailed(CmsObjectType objectType, String objectId, Throwable thrown) {
		if (this.listener != null) {
			this.listener.objectExportFailed(objectType, objectId, thrown);
		}
	}
}