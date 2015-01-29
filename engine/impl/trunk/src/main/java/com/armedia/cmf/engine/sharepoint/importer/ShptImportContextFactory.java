/**
 *
 */

package com.armedia.cmf.engine.sharepoint.importer;

import org.slf4j.Logger;

import com.armedia.cmf.engine.ContextFactory;
import com.armedia.cmf.engine.sharepoint.ShptObject;
import com.armedia.cmf.storage.ContentStore;
import com.armedia.cmf.storage.ObjectStore;
import com.armedia.cmf.storage.StoredObjectType;
import com.armedia.cmf.storage.StoredValue;
import com.armedia.commons.utilities.CfgTools;
import com.independentsoft.share.Service;

/**
 * @author diego
 *
 */
public class ShptImportContextFactory extends
	ContextFactory<Service, ShptObject<?>, StoredValue, ShptImportContext, ShptImportEngine> {

	ShptImportContextFactory(ShptImportEngine engine, CfgTools settings) {
		super(engine, settings);
	}

	@Override
	protected ShptImportContext constructContext(String rootId, StoredObjectType rootType, Service session,
		Logger output, ObjectStore<?, ?> objectStore, ContentStore contentStore) {
		return new ShptImportContext(getEngine(), getSettings(), rootId, rootType, session, output, null, objectStore,
			contentStore);
	}
}