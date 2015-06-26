/**
 *
 */

package com.armedia.cmf.engine.documentum.exporter;

import org.slf4j.Logger;

import com.armedia.cmf.engine.documentum.DctmSessionWrapper;
import com.armedia.cmf.engine.documentum.common.DctmSpecialValues;
import com.armedia.cmf.engine.exporter.ExportContextFactory;
import com.armedia.cmf.storage.CmfContentStore;
import com.armedia.cmf.storage.CmfObjectStore;
import com.armedia.cmf.storage.CmfType;
import com.armedia.cmf.storage.CmfTypeMapper;
import com.armedia.commons.utilities.CfgTools;
import com.documentum.fc.client.IDfSession;
import com.documentum.fc.common.IDfValue;

/**
 * @author diego
 *
 */
public class DctmExportContextFactory extends
	ExportContextFactory<IDfSession, DctmSessionWrapper, IDfValue, DctmExportContext, DctmExportEngine> {

	private final DctmSpecialValues specialValues;

	DctmExportContextFactory(DctmExportEngine engine, CfgTools settings, IDfSession session) throws Exception {
		super(engine, settings, session);
		this.specialValues = new DctmSpecialValues(settings);
	}

	@Override
	protected DctmExportContext constructContext(String rootId, CmfType rootType, IDfSession session, Logger output,
		CmfObjectStore<?, ?> objectStore, CmfContentStore<?> streamStore, CmfTypeMapper typeMapper) {
		return new DctmExportContext(this, rootId, rootType, session, output);
	}

	public final DctmSpecialValues getSpecialValues() {
		return this.specialValues;
	}

	@Override
	protected final String calculateProductName(IDfSession session) {
		return "Documentum";
	}

	@Override
	protected final String calculateProductVersion(IDfSession session) throws Exception {
		return session.getServerVersion();
	}
}