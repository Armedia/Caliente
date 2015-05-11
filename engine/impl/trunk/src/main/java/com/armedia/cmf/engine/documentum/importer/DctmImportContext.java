/**
 *
 */

package com.armedia.cmf.engine.documentum.importer;

import org.slf4j.Logger;

import com.armedia.cmf.engine.documentum.DctmMappingUtils;
import com.armedia.cmf.engine.documentum.common.DctmSpecialValues;
import com.armedia.cmf.engine.importer.ImportContext;
import com.armedia.cmf.storage.ContentStore;
import com.armedia.cmf.storage.ObjectStorageTranslator;
import com.armedia.cmf.storage.ObjectStore;
import com.armedia.cmf.storage.StoredObjectType;
import com.armedia.commons.utilities.CfgTools;
import com.documentum.fc.client.IDfSession;
import com.documentum.fc.common.DfException;
import com.documentum.fc.common.IDfValue;

/**
 * @author diego
 *
 */
public class DctmImportContext extends ImportContext<IDfSession, IDfValue> {

	private final DctmSpecialValues specialValues;

	DctmImportContext(DctmImportContextFactory factory, CfgTools settings, String rootId, StoredObjectType rootType,
		IDfSession session, Logger output, ObjectStorageTranslator<IDfValue> translator, ObjectStore<?, ?> objectStore,
		ContentStore streamStore) {
		super(factory, settings, rootId, rootType, session, output, translator, objectStore, streamStore);
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

	public boolean isUntouchableUser(String user) throws DfException {
		return isSpecialUser(user) || DctmMappingUtils.isSubstitutionForMappableUser(user)
			|| DctmMappingUtils.isMappableUser(getSession(), user);
	}
}