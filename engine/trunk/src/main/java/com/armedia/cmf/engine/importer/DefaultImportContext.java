/**
 *
 */

package com.armedia.cmf.engine.importer;

import org.slf4j.Logger;

import com.armedia.cmf.engine.DefaultTransferContext;
import com.armedia.cmf.storage.ContentStreamStore;
import com.armedia.cmf.storage.ObjectStorageTranslator;
import com.armedia.cmf.storage.ObjectStore;
import com.armedia.cmf.storage.StoredAttributeMapper;
import com.armedia.cmf.storage.StoredObject;

/**
 * @author Diego Rivera &lt;diego.rivera@armedia.com&gt;
 *
 */
public class DefaultImportContext<S, V> extends DefaultTransferContext<S, V> implements ImportListener {

	private final ImportListener listener;

	/**
	 * @param rootId
	 * @param session
	 * @param objectStore
	 * @param fileSystem
	 * @param output
	 */
	public DefaultImportContext(ObjectStorageTranslator<V> translator, String rootId, S session,
		ObjectStore objectStore, ContentStreamStore fileSystem, Logger output, ImportListener listener) {
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
	public DefaultImportContext(ObjectStorageTranslator<V> translator, String rootId, S session,
		ObjectStore objectStore, ContentStreamStore fileSystem, StoredAttributeMapper mapper, Logger output,
		ImportListener listener) {
		super(translator, rootId, session, objectStore, fileSystem, mapper, output);
		this.listener = listener;
	}

	@Override
	public void objectImportStarted(StoredObject<?> object) {
		if (this.listener != null) {
			this.listener.objectImportStarted(object);
		}
	}

	@Override
	public void objectImportCompleted(StoredObject<?> object, ImportResult cmsImportResult, String newLabel,
		String newId) {
		if (this.listener != null) {
			this.listener.objectImportCompleted(object, cmsImportResult, newLabel, newId);
		}
	}

	@Override
	public void objectImportFailed(StoredObject<?> object, Throwable thrown) {
		if (this.listener != null) {
			this.listener.objectImportFailed(object, thrown);
		}
	}
}