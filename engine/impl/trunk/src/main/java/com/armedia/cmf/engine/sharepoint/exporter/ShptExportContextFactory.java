/**
 *
 */

package com.armedia.cmf.engine.sharepoint.exporter;

import org.slf4j.Logger;

import com.armedia.cmf.engine.exporter.ExportContextFactory;
import com.armedia.cmf.engine.sharepoint.ShptSession;
import com.armedia.cmf.engine.sharepoint.ShptSessionWrapper;
import com.armedia.cmf.storage.ContentStore;
import com.armedia.cmf.storage.ObjectStore;
import com.armedia.cmf.storage.StoredObjectType;
import com.armedia.cmf.storage.StoredValue;
import com.armedia.commons.utilities.CfgTools;

/**
 * @author diego
 *
 */
public class ShptExportContextFactory extends
ExportContextFactory<ShptSession, ShptSessionWrapper, StoredValue, ShptExportContext, ShptExportEngine> {

	ShptExportContextFactory(ShptExportEngine engine, CfgTools settings) {
		super(engine, settings);
	}

	@Override
	protected ShptExportContext constructContext(String rootId, StoredObjectType rootType, ShptSession session,
		Logger output, ObjectStore<?, ?> objectStore, ContentStore<?> contentStore) {
		return new ShptExportContext(this, getSettings(), rootId, rootType, session, output);
	}
}