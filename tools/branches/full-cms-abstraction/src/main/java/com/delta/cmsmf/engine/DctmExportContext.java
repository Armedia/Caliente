/**
 *
 */

package com.delta.cmsmf.engine;

import org.slf4j.Logger;

import com.armedia.cmf.documentum.engine.DctmTranslator;
import com.armedia.cmf.engine.exporter.ExportContext;
import com.armedia.cmf.engine.exporter.ExportListener;
import com.armedia.cmf.storage.ContentStore;
import com.armedia.cmf.storage.ObjectStore;
import com.armedia.cmf.storage.StoredAttributeMapper;
import com.documentum.fc.client.IDfSession;
import com.documentum.fc.common.IDfValue;

/**
 * @author Diego Rivera &lt;diego.rivera@armedia.com&gt;
 *
 */
public class DctmExportContext extends DefaultExportContext<IDfSession, IDfValue> {

	/**
	 * @param rootId
	 * @param session
	 * @param objectStore
	 * @param fileSystem
	 * @param output
	 */
	public DctmExportContext(String rootId, IDfSession session, ObjectStore objectStore, ContentStore fileSystem,
		Logger output, ExportListener listener) {
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
	public DctmExportContext(String rootId, IDfSession session, ObjectStore objectStore, ContentStore fileSystem,
		StoredAttributeMapper mapper, Logger output, ExportListener listener) {
		super(DctmTranslator.INSTANCE, rootId, session, objectStore, fileSystem, mapper, output, listener);
	}
}