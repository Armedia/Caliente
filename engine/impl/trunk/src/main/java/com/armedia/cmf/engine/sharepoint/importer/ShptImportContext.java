/**
 *
 */

package com.armedia.cmf.engine.sharepoint.importer;

import org.slf4j.Logger;

import com.armedia.cmf.engine.importer.ImportContext;
import com.armedia.cmf.engine.sharepoint.ShptSession;
import com.armedia.cmf.storage.ContentStore;
import com.armedia.cmf.storage.ObjectStorageTranslator;
import com.armedia.cmf.storage.ObjectStore;
import com.armedia.cmf.storage.StoredObjectType;
import com.armedia.cmf.storage.StoredValue;
import com.armedia.commons.utilities.CfgTools;

/**
 * @author diego
 *
 */
public class ShptImportContext extends ImportContext<ShptSession, StoredValue> {

	public ShptImportContext(ShptImportContextFactory factory, CfgTools settings, String rootId,
		StoredObjectType rootType, ShptSession session, Logger output, ObjectStorageTranslator<StoredValue> translator,
		ObjectStore<?, ?> objectStore, ContentStore streamStore) {
		super(factory, settings, rootId, rootType, session, output, translator, objectStore, streamStore);
	}
}