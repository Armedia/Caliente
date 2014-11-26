/**
 *
 */

package com.armedia.cmf.engine.sharepoint.importer;

import org.slf4j.Logger;

import com.armedia.cmf.engine.importer.ImportContext;
import com.armedia.cmf.engine.sharepoint.ShptObject;
import com.armedia.cmf.storage.ContentStore;
import com.armedia.cmf.storage.ObjectStorageTranslator;
import com.armedia.cmf.storage.ObjectStore;
import com.armedia.cmf.storage.StoredObjectType;
import com.armedia.commons.utilities.CfgTools;
import com.independentsoft.share.Service;

/**
 * @author diego
 *
 */
public class ShptImportContext<V> extends ImportContext<Service, ShptObject<?>, V> {

	public ShptImportContext(ShptImportEngine<V> engine, CfgTools settings, String rootId, StoredObjectType rootType,
		Service session, Logger output, ObjectStorageTranslator<ShptObject<?>, V> translator,
		ObjectStore<?, ?> objectStore, ContentStore streamStore) {
		super(engine, settings, rootId, rootType, session, output, translator, objectStore, streamStore);
	}

	@Override
	protected boolean isSurrogateType(StoredObjectType rootType, StoredObjectType target) {
		return super.isSurrogateType(rootType, target);
	}
}