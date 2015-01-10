/**
 *
 */

package com.armedia.cmf.engine.sharepoint.exporter;

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
public class ShptExportContextFactory extends
	ContextFactory<Service, ShptObject<?>, Object, ShptExportContext, ShptExportEngine> {

	ShptExportContextFactory(ShptExportEngine engine, CfgTools settings) {
		super(engine, settings);
	}

	@Override
	protected ShptExportContext constructContext(String rootId, StoredObjectType rootType, Service session,
		Logger output, ObjectStore<?, ?> objectStore, ContentStore contentStore) {
		return null;
	}
}