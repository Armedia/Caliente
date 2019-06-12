package com.armedia.caliente.content;

import java.time.format.DateTimeFormatter;

public class TimeBasedOrganizer extends Organizer<TimeBasedOrganizerContext> {

	private static final DateTimeFormatter PATH_FORMAT = DateTimeFormatter.ofPattern("yyyy/MMdd/HHmm");
	private static final DateTimeFormatter NAME_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");

	public TimeBasedOrganizer(String folderType, String contentType) {
		super(folderType, contentType);
	}

	@Override
	protected TimeBasedOrganizerContext newState(String applicationName, String clientId, long fileId) {
		return new TimeBasedOrganizerContext(applicationName, clientId, fileId);
	}

	@Override
	protected String renderIntermediatePath(TimeBasedOrganizerContext state) {
		return state.getTimestamp().format(TimeBasedOrganizer.PATH_FORMAT);
	}

	@Override
	protected String renderFileNameTag(TimeBasedOrganizerContext state) {
		return state.getTimestamp().format(TimeBasedOrganizer.NAME_FORMAT);
	}
}