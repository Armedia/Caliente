/**
 *
 */

package com.armedia.caliente.engine.documentum.importer;

import org.slf4j.Logger;

import com.armedia.caliente.engine.documentum.DctmMappingUtils;
import com.armedia.caliente.engine.documentum.common.DctmSpecialValues;
import com.armedia.caliente.engine.importer.ImportContext;
import com.armedia.caliente.store.CmfAttributeTranslator;
import com.armedia.caliente.store.CmfContentStore;
import com.armedia.caliente.store.CmfObjectStore;
import com.armedia.caliente.store.CmfType;
import com.armedia.caliente.store.CmfTypeMapper;
import com.armedia.commons.utilities.CfgTools;
import com.documentum.fc.client.IDfSession;
import com.documentum.fc.common.DfException;
import com.documentum.fc.common.IDfValue;

/**
 * @author diego
 *
 */
public class DctmImportContext extends ImportContext<IDfSession, IDfValue, DctmImportContextFactory> {

	private final DctmSpecialValues specialValues;

	DctmImportContext(DctmImportContextFactory factory, CfgTools settings, String rootId, CmfType rootType,
		IDfSession session, Logger output, CmfTypeMapper typeMapper, CmfAttributeTranslator<IDfValue> translator,
		CmfObjectStore<?, ?> objectStore, CmfContentStore<?, ?, ?> streamStore, int batchPosition) {
		super(factory, settings, rootId, rootType, session, output, typeMapper, translator, objectStore, streamStore,
			batchPosition);
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