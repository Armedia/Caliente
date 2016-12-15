/**
 *
 */

package com.armedia.caliente.engine.documentum.exporter;

import org.slf4j.Logger;

import com.armedia.caliente.engine.WarningTracker;
import com.armedia.caliente.engine.documentum.common.DctmSpecialValues;
import com.armedia.caliente.engine.exporter.ExportContext;
import com.armedia.caliente.store.CmfType;
import com.documentum.fc.client.IDfSession;
import com.documentum.fc.common.IDfValue;

/**
 * @author diego
 *
 */
class DctmExportContext extends ExportContext<IDfSession, IDfValue, DctmExportContextFactory> {

	private final DctmSpecialValues specialValues;

	DctmExportContext(DctmExportContextFactory factory, String rootId, CmfType rootType, IDfSession session,
		Logger output, WarningTracker warningTracker) {
		super(factory, factory.getSettings(), rootId, rootType, session, output, warningTracker);
		this.specialValues = factory.getSpecialValues();
	}

	@Override
	public boolean shouldWaitForRequirement(CmfType referrent, CmfType referenced) {
		switch (referrent) {
			case FOLDER:
			case DOCUMENT:
				return (referenced == CmfType.FOLDER);
			default:
				return false;
		}
	}

	public final boolean isSpecialGroup(String group) {
		return this.specialValues.isSpecialGroup(group);
	}

	public final boolean isSpecialUser(String user) {
		return this.specialValues.isSpecialUser(user);
	}

	public final boolean isSpecialType(String type) {
		return this.specialValues.isSpecialType(type);
	}
}