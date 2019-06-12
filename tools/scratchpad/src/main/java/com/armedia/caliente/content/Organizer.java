package com.armedia.caliente.content;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.jackrabbit.commons.JcrUtils;

public abstract class Organizer<CONTEXT extends OrganizerContext> {

	protected final String folderType;
	protected final String contentType;

	public Organizer(String folderType, String contentType) {
		this.folderType = folderType;
		this.contentType = contentType;
	}

	public final String getFolderType() {
		return this.folderType;
	}

	public final String getContentType() {
		return this.contentType;
	}

	public final Node newContentNode(Session session, ContentStoreClient client) throws RepositoryException {
		final String applicationName = client.getApplicationName();
		final String clientId = client.getId();

		try (final CONTEXT state = newState(applicationName, clientId, client.getNextFileId())) {
			String intermediatePath = FilenameUtils.normalize(renderIntermediatePath(state), true);
			if (StringUtils.isBlank(intermediatePath)) {
				intermediatePath = String.format("%08x", state.getFileIdHex().hashCode());
			}
			intermediatePath = String.format("/%s", intermediatePath);
			String fileName = renderFileNameTag(state);
			if (StringUtils.isBlank(fileName)) {
				fileName = "";
			} else {
				fileName = String.format("-%s", fileName);
			}
			final String fullPath = String.format("/%s%s/%s%s-%s-%s", applicationName, intermediatePath,
				applicationName, fileName, clientId, state.getFileIdHex());
			return JcrUtils.getOrCreateByPath(fullPath, this.folderType, this.contentType, session, false);
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