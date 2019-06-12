package com.armedia.caliente.content;

import java.time.LocalTime;

public class TimeBasedOrganizerContext extends OrganizerContext {
	private final LocalTime timestamp;

	public TimeBasedOrganizerContext(String applicationName, String clientId, long fileId) {
		super(applicationName, clientId, fileId);
		this.timestamp = LocalTime.now(TimeBasedOrganizer.TIME_ZONE_ID);
	}

	public LocalTime getTimestamp() {
		return this.timestamp;
	}
}