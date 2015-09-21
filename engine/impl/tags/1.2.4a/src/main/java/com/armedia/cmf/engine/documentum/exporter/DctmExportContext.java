/**
 *
 */

package com.armedia.cmf.engine.documentum.exporter;

import org.slf4j.Logger;

import com.armedia.cmf.engine.documentum.common.DctmSpecialValues;
import com.armedia.cmf.engine.exporter.ExportContext;
import com.armedia.cmf.storage.StoredObjectType;
import com.documentum.fc.client.IDfPersistentObject;
import com.documentum.fc.client.IDfSession;
import com.documentum.fc.common.IDfValue;

/**
 * @author diego
 *
 */
class DctmExportContext extends ExportContext<IDfSession, IDfPersistentObject, IDfValue> {

	private final DctmSpecialValues specialValues;

	DctmExportContext(DctmExportContextFactory factory, String rootId, StoredObjectType rootType, IDfSession session,
		Logger output) {
		super(factory, factory.getSettings(), rootId, rootType, session, output);
		this.specialValues = factory.getSpecialValues();
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