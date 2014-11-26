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
import com.armedia.commons.utilities.CfgTools;
import com.independentsoft.share.Service;

/**
 * @author diego
 *
 */
public class ShptImportContextFactory<V> extends
	ContextFactory<Service, ShptObject<?>, V, ShptImportContext<V>, ShptImportEngine<V>> {

	ShptImportContextFactory(ShptImportEngine<V> engine, CfgTools settings) {
		super(engine, settings);
	}

	@Override
	protected ShptImportContext<V> constructContext(String rootId, StoredObjectType rootType, Service session,
		Logger output, ObjectStore<?, ?> objectStore, ContentStore contentStore) {
		return new ShptImportContext<V>(getEngine(), getSettings(), rootId, rootType, session, output, null,
			objectStore, contentStore);
	}
}
