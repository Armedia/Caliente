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
package com.armedia.caliente.engine.ucm.model;

import java.io.InputStream;
import java.net.URI;
import java.util.Map;

import com.armedia.caliente.engine.ucm.UcmSession;

public class UcmFile extends UcmFSObject {

	UcmFile(UcmModel model, URI uri, UcmAttributes data) {
		super(model, uri, data, UcmAtt.fFileName, UcmAtt.dOriginalName);
	}

	public String getPublishedFileName() {
		return getString(UcmAtt.dDocTitle);
	}

	public int getRevisionRank() {
		return getInteger(UcmAtt.dRevRank);
	}

	public boolean isLatestRevision() {
		return (getRevisionRank() == 0);
	}

	public String getRevisionName() {
		return getString(UcmAtt.dOriginalName);
	}

	public String getAuthor() {
		return getString(UcmAtt.dDocAuthor);
	}

	public String getTitle() {
		return getString(UcmAtt.dDocTitle);
	}

	public long getSize() {
		return getLong(UcmAtt.dFileSize, 0);
	}

	public String getFormat() {
		return getString(UcmAtt.dFormat);
	}

	public String getRevisionId() {
		return getString(UcmAtt.dID);
	}

	public int getRevisionNumber() {
		return getInteger(UcmAtt.dRevisionID, 1);
	}

	public int getPublishedRevisionId() {
		return getInteger(UcmAtt.dPublishedRevisionID, 1);
	}

	public String getRevisionLabel() {
		return getString(UcmAtt.dRevLabel);
	}

	public String getExtension() {
		return getString(UcmAtt.dExtension);
	}

	public String getContentId() {
		return getString(UcmAtt.dDocName);
	}

	public InputStream getInputStream(UcmSession s)
		throws UcmServiceException, UcmFileRevisionNotFoundException, UcmRenditionNotFoundException {
		return s.getInputStream(this);
	}

	public InputStream getInputStream(UcmSession s, String rendition)
		throws UcmServiceException, UcmFileRevisionNotFoundException, UcmRenditionNotFoundException {
		return s.getInputStream(this, rendition);
	}

	public Map<String, UcmRenditionInfo> getRenditions(UcmSession s)
		throws UcmServiceException, UcmFileRevisionNotFoundException {
		return s.getRenditions(this);
	}
}