package com.armedia.caliente.content;

import java.time.LocalTime;

public class TimeBasedOrganizerContext extends OrganizerContext {
	private final LocalTime timestamp;

	public TimeBasedOrganizerContext(String applicationName, String clientId, long fileId) {
		this(applicationName, clientId, fileId, null);
	}

	public TimeBasedOrganizerContext(String applicationName, String clientId, long fileId, LocalTime timestamp) {
		super(applicationName, clientId, fileId);
		if (timestamp == null) {
			timestamp = LocalTime.now(TimeBasedOrganizer.TIME_ZONE_ID);
		}
		this.timestamp = timestamp;
	}

	public final LocalTime getTimestamp() {
		return this.timestamp;
	}
}