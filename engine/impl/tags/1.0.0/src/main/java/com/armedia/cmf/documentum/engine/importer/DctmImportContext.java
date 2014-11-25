/**
 *
 */

package com.armedia.cmf.documentum.engine.importer;

import org.slf4j.Logger;

import com.armedia.cmf.documentum.engine.DctmObjectType;
import com.armedia.cmf.documentum.engine.DctmTranslator;
import com.armedia.cmf.documentum.engine.common.DctmSpecialValues;
import com.armedia.cmf.engine.importer.ImportContext;
import com.armedia.cmf.storage.ContentStore;
import com.armedia.cmf.storage.ObjectStore;
import com.armedia.cmf.storage.StoredObjectType;
import com.documentum.fc.client.IDfPersistentObject;
import com.documentum.fc.client.IDfSession;
import com.documentum.fc.common.IDfValue;

/**
 * @author diego
 *
 */
public class DctmImportContext extends ImportContext<IDfSession, IDfPersistentObject, IDfValue> {

	private final DctmSpecialValues specialValues;

	DctmImportContext(DctmImportContextFactory factory, String rootId, StoredObjectType rootType, IDfSession session,
		Logger output, ObjectStore<?, ?> objectStore, ContentStore streamStore) {
		super(factory.getEngine(), factory.getSettings(), rootId, rootType, session, output, DctmTranslator.INSTANCE,
			objectStore, streamStore);
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

	@Override
	protected boolean isSurrogateType(StoredObjectType rootType, StoredObjectType target) {
		DctmObjectType dctmRootType = DctmTranslator.translateType(rootType);
		if (dctmRootType == null) { return false; }
		DctmObjectType dctmTarget = DctmTranslator.translateType(target);
		if (dctmTarget == null) { return false; }
		return dctmTarget.getSurrogateOf().contains(dctmRootType) || super.isSurrogateType(rootType, target);
	}
}