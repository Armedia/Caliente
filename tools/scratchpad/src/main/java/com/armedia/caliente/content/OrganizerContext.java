package com.armedia.caliente.content;

public class OrganizerContext implements AutoCloseable {

	private final String applicationName;
	private final String clientId;
	private final long fileId;
	private final String fileIdHex;

	public OrganizerContext(String applicationName, String clientId, long fileId) {
		this.applicationName = applicationName;
		this.clientId = clientId;
		this.fileId = fileId;
		this.fileIdHex = String.format("%016x", fileId);
	}

	public final String getApplicationName() {
		return this.applicationName;
	}

	public final String getClientId() {
		return this.clientId;
	}

	public final long getFileId() {
		return this.fileId;
	}

	public final String getFileIdHex() {
		return this.fileIdHex;
	}

	@Override
	public void close() {
	}
}