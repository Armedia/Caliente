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

import com.armedia.caliente.store.CmfAttributeTranslator;
import com.armedia.caliente.store.CmfContentStream;
import com.armedia.caliente.store.CmfObject;
import com.armedia.commons.utilities.Tools;

public class HierarchicalOrganizer extends LocalOrganizer {
	public static final String NAME = "hierarchical";

	public HierarchicalOrganizer() {
		this(HierarchicalOrganizer.NAME);
	}

	protected HierarchicalOrganizer(String name) {
		super(name);
	}

	@Override
	protected <T> Location calculateLocation(CmfAttributeTranslator<T> translator, CmfObject<T> object,
		CmfContentStream info) {

		int folderLevels = 3;
		// A maximum of 7 levels...
		folderLevels = Tools.ensureBetween(3, folderLevels, 7);

		List<String> paths = new ArrayList<>();
		// Make sure the contents all land in the bulk-import root location, so it's easy to point
		// the bulk importer at that directory and not import any unwanted crap
		String fullObjectNumber = PathTools.addNumericPaths(paths, object.getNumber());

		String appendix = String.format("%08x", info.getIndex());
		String baseName = fullObjectNumber;

		return newLocation(paths, baseName, null, null, appendix);
	}
}