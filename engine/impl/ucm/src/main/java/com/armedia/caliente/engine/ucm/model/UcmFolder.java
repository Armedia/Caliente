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
package com.armedia.caliente.engine.ucm.model;

import java.net.URI;
import java.util.Collection;
import java.util.Map;

import com.armedia.caliente.engine.ucm.UcmSession;
import com.armedia.caliente.engine.ucm.model.UcmModel.ObjectHandler;

public class UcmFolder extends UcmFSObject {

	UcmFolder(UcmModel model, URI uri, UcmAttributes data) {
		super(model, uri, data, UcmAtt.fFolderName);
	}

	@Override
	public UcmFolder getParentFolder(UcmSession s) throws UcmFolderNotFoundException, UcmServiceException {
		if (isRoot()) { return null; }
		return super.getParentFolder(s);
	}

	public boolean isRoot() {
		return UcmModel.ROOT_URI.equals(getURI());
	}

	public String getDisplayDescription() throws UcmException {
		return getString(UcmAtt.fDisplayDescription);
	}

	public String getFolderDescription() throws UcmException {
		return getString(UcmAtt.fFolderDescription);
	}

	public Map<String, UcmFSObject> getContents(UcmSession s) throws UcmFolderNotFoundException, UcmServiceException {
		return s.getFolderContents(this);
	}

	public int iterateFolderContents(final UcmSession s, final ObjectHandler handler)
		throws UcmServiceException, UcmFolderNotFoundException {
		return s.iterateFolderContents(this, handler);
	}

	public int iterateFolderContentsRecursive(final UcmSession s, final ObjectHandler handler)
		throws UcmServiceException, UcmFolderNotFoundException {
		return iterateFolderContentsRecursive(s, false, handler);
	}

	public int iterateFolderContentsRecursive(final UcmSession s, boolean recurseShortcuts, final ObjectHandler handler)
		throws UcmServiceException, UcmFolderNotFoundException {
		return s.iterateFolderContentsRecursive(this, recurseShortcuts, handler);
	}

	public Collection<UcmFSObject> getFolderContentsRecursive(final UcmSession s, boolean followShortCuts)
		throws UcmServiceException, UcmFolderNotFoundException {
		return s.getFolderContentsRecursive(this, followShortCuts);
	}
}