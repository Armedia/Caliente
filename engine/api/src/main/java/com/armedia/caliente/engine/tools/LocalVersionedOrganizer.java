/*******************************************************************************
 * #%L
 * Armedia Caliente
 * %%
 * Copyright (C) 2013 - 2019 Armedia, LLC
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
package com.armedia.caliente.engine.tools;

import java.util.List;

import com.armedia.caliente.engine.converter.IntermediateAttribute;
import com.armedia.caliente.engine.converter.IntermediateProperty;
import com.armedia.caliente.store.CmfAttribute;
import com.armedia.caliente.store.CmfAttributeTranslator;
import com.armedia.caliente.store.CmfContentStream;
import com.armedia.caliente.store.CmfObject;
import com.armedia.caliente.store.CmfProperty;

public class LocalVersionedOrganizer extends LocalOrganizer {

	public static final String NAME = "versionedlocalfs";

	public LocalVersionedOrganizer() {
		super(LocalVersionedOrganizer.NAME);
	}

	protected LocalVersionedOrganizer(String name) {
		super(name);
	}

	@Override
	protected <T> List<String> calculateContainerSpec(CmfAttributeTranslator<T> translator, CmfObject<T> object,
		CmfContentStream info) {
		final List<String> container = super.calculateContainerSpec(translator, object, info);

		// Next step: add the object name
		CmfProperty<?> name = object.getProperty(IntermediateProperty.HEAD_NAME);
		if (name == null) {
			name = object.getAttribute(
				translator.getAttributeNameMapper().decodeAttributeName(object.getType(), IntermediateAttribute.NAME));
		}

		final String objectName;
		if (name == null) {
			objectName = object.getName();
		} else {
			objectName = name.getValue().toString();
		}
		container.add(objectName);

		// Finally, add the version number, appending "CURRENT" if it's the current version
		String versionLabel = null;
		CmfAttribute<?> version = object.getAttribute(translator.getAttributeNameMapper()
			.decodeAttributeName(object.getType(), IntermediateAttribute.VERSION_LABEL));
		if (version == null) {
			versionLabel = "0.0";
		} else {
			versionLabel = version.getValue().toString();
		}

		if (object.isHistoryCurrent()) {
			versionLabel += ",CURRENT";
		}
		container.add(versionLabel);

		return container;
	}

	@Override
	protected <T> String calculateBaseName(CmfAttributeTranslator<T> translator, CmfObject<T> object,
		CmfContentStream info) {
		return super.calculateDescriptor(translator, object, info);
	}

	@Override
	protected <T> String calculateDescriptor(CmfAttributeTranslator<T> translator, CmfObject<T> object,
		CmfContentStream info) {
		// There is no descriptor in this scheme ...
		return "";
	}
}