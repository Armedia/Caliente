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
package com.armedia.caliente.engine.ucm.exporter;

import java.util.List;

import com.armedia.caliente.engine.ucm.UcmSession;
import com.armedia.caliente.engine.ucm.model.UcmFolder;
import com.armedia.commons.utilities.FileNameTools;

public class UcmFolderExportDelegate extends UcmFSObjectExportDelegate<UcmFolder> {

	protected UcmFolderExportDelegate(UcmExportDelegateFactory factory, UcmSession session, UcmFolder object)
		throws Exception {
		super(factory, session, UcmFolder.class, object);
	}

	@Override
	protected int calculateDependencyTier(UcmSession session, UcmFolder folder) throws Exception {
		final UcmFolder original = folder;

		if (folder.isShortcut()) {
			folder = session.getFolderByGUID(folder.getTargetGUID());
		}

		String path = folder.getPath();
		List<String> l = FileNameTools.tokenize(path, '/');
		int depth = l.size();
		if (original.isShortcut()) {
			depth++;
		}
		return depth;
	}
}