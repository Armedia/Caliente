/**
 *
 */

package com.armedia.cmf.engine.exporter;

import org.slf4j.Logger;

import com.armedia.cmf.engine.TransferContext;
import com.armedia.cmf.storage.ContentStreamStore;
import com.armedia.cmf.storage.ObjectStorageTranslator;
import com.armedia.cmf.storage.ObjectStore;

/**
 * @author Diego Rivera &lt;diego.rivera@armedia.com&gt;
 *
 */
public final class ExportContext<S, T, V> extends TransferContext<S, T, V> {
	/**
	 * @param rootId
	 * @param session
	 * @param objectStore
	 * @param fileSystem
	 * @param output
	 */
	ExportContext(ObjectStorageTranslator<T, V> translator, String rootId, S session, ObjectStore<?, ?> objectStore,
		ContentStreamStore fileSystem, Logger output) {
		super(translator, rootId, session, objectStore, fileSystem, output);
	}
}