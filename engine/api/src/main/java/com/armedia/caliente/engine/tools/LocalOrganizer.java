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

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import com.armedia.caliente.engine.converter.IntermediateAttribute;
import com.armedia.caliente.engine.converter.IntermediateProperty;
import com.armedia.caliente.store.CmfAttribute;
import com.armedia.caliente.store.CmfAttributeTranslator;
import com.armedia.caliente.store.CmfContentOrganizer;
import com.armedia.caliente.store.CmfContentStream;
import com.armedia.caliente.store.CmfObject;
import com.armedia.caliente.store.CmfProperty;
import com.armedia.caliente.tools.FilenameFixer;
import com.armedia.commons.utilities.FileNameTools;

public class LocalOrganizer extends CmfContentOrganizer {

	public static final String NAME = "localfs";

	private final FilenameFixer fixer = new FilenameFixer(true);

	public LocalOrganizer() {
		super(LocalOrganizer.NAME);
	}

	protected LocalOrganizer(String name) {
		super(name);
	}

	@Override
	protected <T> Location calculateLocation(CmfAttributeTranslator<T> translator, CmfObject<T> object,
		CmfContentStream info) {
		final List<String> containerSpec = calculateContainerSpec(translator, object, info);
		final String baseName = calculateBaseName(translator, object, info);
		final String descriptor = calculateDescriptor(translator, object, info);
		final String extension = calculateExtension(translator, object, info);
		final String appendix = calculateAppendix(translator, object, info);
		return newLocation(containerSpec, baseName, extension, descriptor, appendix);
	}

	protected <T> List<String> calculateContainerSpec(CmfAttributeTranslator<T> translator, CmfObject<T> object,
		CmfContentStream info) {
		// Put it in the same path as it was in CMIS, but ensure each path component is
		// of a "universally-valid" format.
		CmfProperty<T> paths = object.getProperty(IntermediateProperty.FIXED_PATH);

		List<String> ret = new ArrayList<>();
		if ((paths != null) && paths.hasValues()) {
			for (String p : FileNameTools.tokenize(paths.getValue().toString(), '/')) {
				ret.add(p);
			}
		}
		return ret;
	}

	protected <T> String getLeafName(CmfAttributeTranslator<T> translator, CmfObject<T> object) {
		String objectName = null;
		CmfProperty<?> name = null;

		if (StringUtils.isEmpty(objectName)) {
			name = object.getProperty(IntermediateProperty.FIXED_NAME);
			if (name != null) {
				objectName = name.getValue().toString();
			}
		}

		if (StringUtils.isEmpty(objectName)) {
			name = object.getProperty(IntermediateProperty.HEAD_NAME);
			if (name != null) {
				objectName = name.getValue().toString();
			}
		}

		if (StringUtils.isEmpty(objectName)) {
			name = object.getAttribute(
				translator.getAttributeNameMapper().decodeAttributeName(object.getType(), IntermediateAttribute.NAME));
			if (name != null) {
				objectName = name.getValue().toString();
			}
		}

		if (StringUtils.isEmpty(objectName)) {
			objectName = object.getName();
		}

		if (StringUtils.isEmpty(objectName)) {
			// Uh-oh ... an empty filename!!! Can't have that!!
			objectName = String.format("[history-%s]", object.getHistoryId());
		} else {
			objectName = this.fixer.fixName(objectName);
		}
		return objectName;
	}

	protected <T> String calculateBaseName(CmfAttributeTranslator<T> translator, CmfObject<T> object,
		CmfContentStream info) {
		return getLeafName(translator, object);
	}

	protected <T> String calculateExtension(CmfAttributeTranslator<T> translator, CmfObject<T> object,
		CmfContentStream info) {
		return info.getExtension();
	}

	protected <T> String calculateDescriptor(CmfAttributeTranslator<T> translator, CmfObject<T> object,
		CmfContentStream info) {
		final String attName = translator.getAttributeNameMapper().decodeAttributeName(object.getType(),
			IntermediateAttribute.VERSION_LABEL);
		final CmfAttribute<?> versionLabelAtt = object.getAttribute(attName);
		String oldFrag = String.format("%s.%08x", info.getRenditionIdentifier(), info.getRenditionPage());
		if ((versionLabelAtt != null) && versionLabelAtt.hasValues()) {
			final String versionLabel = versionLabelAtt.getValue().toString();
			if (StringUtils.isBlank(versionLabel)) { return oldFrag; }
			if (StringUtils.isBlank(oldFrag)) { return versionLabel; }
			return String.format("%s@%s", oldFrag, versionLabel);
		}
		return oldFrag;
	}

	protected <T> String calculateAppendix(CmfAttributeTranslator<T> translator, CmfObject<T> object,
		CmfContentStream info) {
		return "";
	}
}