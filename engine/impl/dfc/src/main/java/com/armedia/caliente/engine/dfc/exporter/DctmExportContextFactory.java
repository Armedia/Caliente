/**
 *
 */

package com.armedia.caliente.engine.dfc.exporter;

import org.slf4j.Logger;

import com.armedia.caliente.engine.WarningTracker;
import com.armedia.caliente.engine.dfc.DctmSessionWrapper;
import com.armedia.caliente.engine.dfc.common.DctmSpecialValues;
import com.armedia.caliente.engine.exporter.ExportContextFactory;
import com.armedia.caliente.store.CmfContentStore;
import com.armedia.caliente.store.CmfObject;
import com.armedia.caliente.store.CmfObjectStore;
import com.armedia.commons.utilities.CfgTools;
import com.documentum.fc.client.IDfSession;
import com.documentum.fc.common.IDfValue;

/**
 * @author diego
 *
 */
public class DctmExportContextFactory
	extends ExportContextFactory<IDfSession, DctmSessionWrapper, IDfValue, DctmExportContext, DctmExportEngine> {

	private final DctmSpecialValues specialValues;

	DctmExportContextFactory(DctmExportEngine engine, CfgTools settings, IDfSession session,
		CmfObjectStore<?, ?> objectStore, CmfContentStore<?, ?, ?> contentStore, Logger output,
		WarningTracker warningTracker) throws Exception {
		super(engine, settings, session, objectStore, contentStore, output, warningTracker);
		this.specialValues = new DctmSpecialValues(settings);
	}

	@Override
	protected DctmExportContext constructContext(String rootId, CmfObject.Archetype rootType, IDfSession session,
		int batchPosition) {
		return new DctmExportContext(this, rootId, rootType, session, getOutput(), getWarningTracker());
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