/**
 *
 */

package com.armedia.cmf.documentum.engine.exporter;

import com.armedia.cmf.documentum.engine.DctmObjectType;
import com.documentum.fc.client.IDfFolder;
import com.documentum.fc.client.IDfSession;
import com.documentum.fc.client.IDfSysObject;
import com.documentum.fc.common.DfException;
import com.documentum.fc.common.IDfId;

/**
 * @author diego
 *
 */
public class DctmExportSysObject<T extends IDfSysObject> extends DctmExportAbstract<T> {

	protected DctmExportSysObject(DctmExportEngine engine, DctmObjectType type) {
		super(engine, type);
	}

	private String calculateVersionString(IDfSysObject sysObject, boolean full) throws DfException {
		if (!full) { return String.format("%s%s", sysObject.getImplicitVersionLabel(),
			sysObject.getHasFolder() ? ",CURRENT" : ""); }
		int labelCount = sysObject.getVersionLabelCount();
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < labelCount; i++) {
			if (i > 0) {
				sb.append(',');
			}
			sb.append(sysObject.getVersionLabel(i));
		}
		return sb.toString();
	}

	@Override
	protected String calculateLabel(IDfSession session, IDfSysObject sysObject) throws DfException {
		final int folderCount = sysObject.getFolderIdCount();
		final String objectName = sysObject.getObjectName();
		for (int i = 0; i < folderCount; i++) {
			IDfId id = sysObject.getFolderId(i);
			IDfFolder f = IDfFolder.class.cast(session.getFolderBySpecification(id.getId()));
			if (f != null) {
				String path = (f.getFolderPathCount() > 0 ? f.getFolderPath(0) : String.format("(unknown-folder:[%s])",
					id.getId()));
				return String.format("%s/%s [%s]", path, objectName, calculateVersionString(sysObject, true));
			}
		}
		throw new DfException(String.format("None of the parent paths for object [%s] were found", sysObject
			.getObjectId().getId()));
	}

}