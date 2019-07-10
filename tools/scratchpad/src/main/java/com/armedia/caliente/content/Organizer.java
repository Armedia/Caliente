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
package com.armedia.caliente.content;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.jackrabbit.commons.JcrUtils;

import com.armedia.commons.utilities.Tools;

public abstract class Organizer<CONTEXT extends OrganizerContext> {

	protected static final String FOLDER_TYPE = "nt:folder";

	protected final String folderType;

	public Organizer(String folderType) {
		this.folderType = Tools.coalesce(folderType, Organizer.FOLDER_TYPE);
	}

	public final String getFolderType() {
		return this.folderType;
	}

	public final Pair<Node, String> newContentLocation(Session session, ContentStoreClient client)
		throws RepositoryException {
		final String applicationName = client.getApplicationName();
		final String clientId = client.getId();

		try (final CONTEXT state = newState(applicationName, clientId, client.getNextFileId())) {
			String intermediatePath = FilenameUtils.normalize(renderIntermediatePath(state), true);
			if (StringUtils.isBlank(intermediatePath)) {
				intermediatePath = String.format("%08x", state.getFileIdHex().hashCode());
			}
			intermediatePath = String.format("/%s", intermediatePath);
			String fileNameTag = renderFileNameTag(state);
			if (StringUtils.isBlank(fileNameTag)) {
				fileNameTag = "";
			} else {
				fileNameTag = String.format("-%s", fileNameTag);
			}
			final String parentPath = String.format("/%s%s", applicationName, intermediatePath);
			final Node parent = JcrUtils.getOrCreateByPath(parentPath, this.folderType, session);
			final String fileName = String.format("%s%s-%s-%s", applicationName, fileNameTag, clientId,
				state.getFileIdHex());
			return Pair.of(parent, fileName);
		}
	}

	protected abstract CONTEXT newState(String applicationName, String clientId, long fileId);

	protected String renderIntermediatePath(CONTEXT context) {
		return null;
	}

	protected String renderFileNameTag(CONTEXT context) {
		return null;
	}
}