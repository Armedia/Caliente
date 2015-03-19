/**
 *
 */

package com.armedia.cmf.engine.sharepoint.importer;

import org.slf4j.Logger;

import com.armedia.cmf.engine.ContextFactory;
import com.armedia.cmf.engine.sharepoint.ShptSession;
import com.armedia.cmf.engine.sharepoint.types.ShptObject;
import com.armedia.cmf.storage.ContentStore;
import com.armedia.cmf.storage.ObjectStore;
import com.armedia.cmf.storage.StoredObjectType;
import com.armedia.cmf.storage.StoredValue;
import com.armedia.commons.utilities.CfgTools;

/**
 * @author diego
 *
 */
public class ShptImportContextFactory extends
	ContextFactory<ShptSession, ShptObject<?>, StoredValue, ShptImportContext, ShptImportEngine> {

	ShptImportContextFactory(ShptImportEngine engine, CfgTools settings) {
		super(engine, settings);
	}

	@Override
	protected ShptImportContext constructContext(String rootId, StoredObjectType rootType, ShptSession session,
		Logger output, ObjectStore<?, ?> objectStore, ContentStore contentStore) {
		return new ShptImportContext(this, getSettings(), rootId, rootType, session, output, null, objectStore,
			contentStore);
	}
}