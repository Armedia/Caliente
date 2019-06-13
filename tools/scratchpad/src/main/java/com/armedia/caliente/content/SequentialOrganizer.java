package com.armedia.caliente.content;

public class SequentialOrganizer extends Organizer<OrganizerContext> {

	public SequentialOrganizer() {
		this(null);
	}

	public SequentialOrganizer(String folderType) {
		super(folderType);
	}

	@Override
	protected OrganizerContext newState(String applicationName, String clientId, long fileId) {
		return new OrganizerContext(applicationName, clientId, fileId);
	}

	@Override
	protected String renderIntermediatePath(OrganizerContext context) {
		String hex = context.getFileIdHex();
		StringBuilder b = new StringBuilder(21);
		b.append(hex.substring(0, 2));
		hex = hex.substring(2);
		for (int i = 0; i < 4; i++) {
			b.append('/').append(hex.subSequence(0, 3));
			hex = hex.substring(3);
		}
		return b.toString();
	}
}