/**
 *
 */

package com.armedia.cmf.engine.sharepoint.exporter;

import org.slf4j.Logger;

import com.armedia.cmf.engine.exporter.ExportContext;
import com.armedia.cmf.engine.sharepoint.ShptSession;
import com.armedia.cmf.engine.sharepoint.types.ShptObject;
import com.armedia.cmf.storage.StoredObjectType;
import com.armedia.cmf.storage.StoredValue;
import com.armedia.commons.utilities.CfgTools;

/**
 * @author diego
 *
 */
public class ShptExportContext extends ExportContext<ShptSession, ShptObject<?>, StoredValue> {

	public ShptExportContext(ShptExportEngine engine, CfgTools settings, String rootId, StoredObjectType rootType,
		ShptSession session, Logger output) {
		super(engine, settings, rootId, rootType, session, output);
	}

}