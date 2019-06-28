/*******************************************************************************
 * #%L
 * Armedia Caliente
 * %%
 * Copyright (c) 2010 - 2019 Armedia LLC
 * %%
 * This file is part of the Caliente software. 
 *  
 * If the software was purchased under a paid Caliente license, the terms of 
 * the paid license agreement will prevail.  Otherwise, the software is 
 * provided under the following open source license terms:
 *
 * Caliente is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *   
 * Caliente is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Caliente. If not, see <http://www.gnu.org/licenses/>.
 * #L%
 *******************************************************************************/
/**
 *
 */

package com.armedia.caliente.engine.dfc.importer;

import org.slf4j.Logger;

import com.armedia.caliente.engine.WarningTracker;
import com.armedia.caliente.engine.dfc.DctmMappingUtils;
import com.armedia.caliente.engine.dfc.common.DctmSpecialValues;
import com.armedia.caliente.engine.dynamic.transformer.Transformer;
import com.armedia.caliente.engine.importer.ImportContext;
import com.armedia.caliente.store.CmfAttributeTranslator;
import com.armedia.caliente.store.CmfContentStore;
import com.armedia.caliente.store.CmfObject;
import com.armedia.caliente.store.CmfObjectStore;
import com.armedia.commons.utilities.CfgTools;
import com.documentum.fc.client.IDfSession;
import com.documentum.fc.common.DfException;
import com.documentum.fc.common.IDfValue;

/**
 *
 *
 */
public class DctmImportContext extends ImportContext<IDfSession, IDfValue, DctmImportContextFactory> {

	private final DctmSpecialValues specialValues;

	DctmImportContext(DctmImportContextFactory factory, CfgTools settings, String rootId, CmfObject.Archetype rootType,
		IDfSession session, Logger output, WarningTracker warningTracker, Transformer transformer,
		CmfAttributeTranslator<IDfValue> translator, CmfObjectStore<?> objectStore, CmfContentStore<?, ?> streamStore,
		int historyPosition) {
		super(factory, settings, rootId, rootType, session, output, warningTracker, transformer, translator,
			objectStore, streamStore, historyPosition);
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