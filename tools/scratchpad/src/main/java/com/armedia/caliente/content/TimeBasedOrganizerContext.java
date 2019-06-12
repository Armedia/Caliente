package com.armedia.caliente.content;

import java.time.ZoneId;
import java.time.ZonedDateTime;

import com.armedia.commons.utilities.Tools;

public class TimeBasedOrganizerContext extends OrganizerContext {

	// Everything is on ZULU time
	protected static final ZoneId ZONE_ID = ZoneId.of("Z");

	private final ZonedDateTime timestamp;

	public TimeBasedOrganizerContext(String applicationName, String clientId, long fileId) {
		this(applicationName, clientId, fileId, null);
	}

	public TimeBasedOrganizerContext(String applicationName, String clientId, long fileId, ZonedDateTime timestamp) {
		super(applicationName, clientId, fileId);
		if (timestamp == null) {
			// No time... get the current instant
			timestamp = ZonedDateTime.now(TimeBasedOrganizerContext.ZONE_ID);
		} else if (!Tools.equals(TimeBasedOrganizerContext.ZONE_ID, timestamp.getZone())) {
			// Not in Zulu time? Convert it!
			timestamp = timestamp.withZoneSameInstant(TimeBasedOrganizerContext.ZONE_ID);
		}
		this.timestamp = timestamp;
	}

	public final ZonedDateTime getTimestamp() {
		return this.timestamp;
	}
}