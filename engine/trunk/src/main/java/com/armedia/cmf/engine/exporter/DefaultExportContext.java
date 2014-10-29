/**
 *
 */

package com.armedia.cmf.engine.exporter;

import org.slf4j.Logger;

import com.armedia.cmf.engine.DefaultTransferContext;
import com.armedia.cmf.storage.ContentStreamStore;
import com.armedia.cmf.storage.ObjectStorageTranslator;
import com.armedia.cmf.storage.ObjectStore;
import com.armedia.cmf.storage.StoredAttributeMapper;
import com.armedia.cmf.storage.StoredObject;
import com.armedia.cmf.storage.StoredObjectType;

/**
 * @author Diego Rivera &lt;diego.rivera@armedia.com&gt;
 *
 */
public class DefaultExportContext<T, S, V> extends DefaultTransferContext<T, S, V> implements ExportListener {

	private final ExportListener listener;

	/**
	 * @param rootId
	 * @param session
	 * @param objectStore
	 * @param fileSystem
	 * @param output
	 */
	public DefaultExportContext(ObjectStorageTranslator<T, V> translator, String rootId, S session,
		ObjectStore objectStore, ContentStreamStore fileSystem, Logger output, ExportListener listener) {
		this(translator, rootId, session, objectStore, fileSystem, null, output, listener);
	}

	/**
	 * @param rootId
	 * @param session
	 * @param objectStore
	 * @param fileSystem
	 * @param mapper
	 * @param output
	 */
	public DefaultExportContext(ObjectStorageTranslator<T, V> translator, String rootId, S session,
		ObjectStore objectStore, ContentStreamStore fileSystem, StoredAttributeMapper mapper, Logger output,
		ExportListener listener) {
		super(translator, rootId, session, objectStore, fileSystem, mapper, output);
		this.listener = listener;
	}

	@Override
	public void objectExportStarted(StoredObjectType objectType, String objectId) {
		if (this.listener != null) {
			this.listener.objectExportStarted(objectType, objectId);
		}
	}

	@Override
	public void objectExportCompleted(StoredObject<?> object, Long objectNumber) {
		if (this.listener != null) {
			this.listener.objectExportCompleted(object, objectNumber);
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