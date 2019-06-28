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
package com.armedia.caliente.engine.dfc;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.armedia.caliente.engine.tools.LocalOrganizer;
import com.armedia.caliente.store.CmfAttributeTranslator;
import com.armedia.caliente.store.CmfContentStream;
import com.armedia.caliente.store.CmfObject;

public class DocumentumOrganizer extends LocalOrganizer {

	public static final String NAME = "documentum";

	public DocumentumOrganizer() {
		super(DocumentumOrganizer.NAME);
	}

	@Override
	protected <T> List<String> calculateContainerSpec(CmfAttributeTranslator<T> translator, CmfObject<T> object,
		CmfContentStream info) {
		final String objectId = object.getId();
		if (objectId.length() != 16) { return null; }

		try {
			Long.parseLong(objectId, 16);
		} catch (NumberFormatException e) {
			// Not a documentum ID, so do something else
			return null;
		}

		// 16 character object id in dctm consists of first 2 chars of obj type, next 6
		// chars of docbase id in hex and last 8 chars server generated.
		// For ex: if the id is 0600a92b80054db8 than the SSP would be
		// 06/00a92b/800/54d/0600a92b80054db8
		String uniqueId = objectId.substring(8);
		String[] components = {
			objectId.substring(0, 2), // The object type
			objectId.substring(2, 8), // The docbase ID
			uniqueId.substring(0, 3), // The first 3 characters of the unique object ID
			uniqueId.substring(3, 6), // The 2nd 3 characters of the unique object ID
		};
		return new ArrayList<>(Arrays.asList(components));
	}

	@Override
	protected <T> String calculateBaseName(CmfAttributeTranslator<T> translator, CmfObject<T> object,
		CmfContentStream info) {
		return object.getId();
	}

	@Override
	protected <T> String calculateDescriptor(CmfAttributeTranslator<T> translator, CmfObject<T> object,
		CmfContentStream info) {
		return String.format("%s.%08x", info.getRenditionIdentifier(), info.getRenditionPage());
	}
}