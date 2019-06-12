package com.armedia.caliente.content;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.jackrabbit.commons.JcrUtils;

public abstract class Organizer<CONTEXT extends OrganizerContext> {

	protected final String folderType;

	public Organizer(String folderType) {
		this.folderType = folderType;
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