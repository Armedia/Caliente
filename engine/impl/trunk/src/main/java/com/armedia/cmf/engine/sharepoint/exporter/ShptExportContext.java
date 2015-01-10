/**
 *
 */

package com.armedia.cmf.engine.sharepoint.exporter;

import org.slf4j.Logger;

import com.armedia.cmf.engine.exporter.ExportContext;
import com.armedia.cmf.engine.sharepoint.ShptObject;
import com.armedia.cmf.storage.StoredObjectType;
import com.armedia.commons.utilities.CfgTools;
import com.independentsoft.share.Service;

/**
 * @author diego
 *
 */
public class ShptExportContext extends ExportContext<Service, ShptObject<?>, Object> {

	public ShptExportContext(ShptExportEngine engine, CfgTools settings, String rootId, StoredObjectType rootType,
		Service session, Logger output) {
		super(engine, settings, rootId, rootType, session, output);
	}

}