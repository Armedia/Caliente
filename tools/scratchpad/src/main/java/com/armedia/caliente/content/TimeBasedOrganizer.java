package com.armedia.caliente.content;

import java.time.ZoneId;
import java.util.TimeZone;

import org.apache.commons.lang3.time.FastDateFormat;

public class TimeBasedOrganizer extends Organizer<TimeBasedOrganizerContext> {

	protected static final ZoneId TIME_ZONE_ID = ZoneId.of("Z");
	protected static final TimeZone TIME_ZONE = TimeZone.getTimeZone(TimeBasedOrganizer.TIME_ZONE_ID);
	private static final FastDateFormat PATH_FORMAT = FastDateFormat.getInstance("yyyy/MMdd/HHmm",
		TimeBasedOrganizer.TIME_ZONE);
	private static final FastDateFormat NAME_FORMAT = FastDateFormat.getInstance("yyyyMMddHHmmssSSS",
		TimeBasedOrganizer.TIME_ZONE);

	public TimeBasedOrganizer(String folderType, String contentType) {
		super(folderType, contentType);
	}

	@Override
	protected TimeBasedOrganizerContext newState(String applicationName, String clientId, long fileId) {
		return new TimeBasedOrganizerContext(applicationName, clientId, fileId);
	}

	@Override
	protected String renderIntermediatePath(TimeBasedOrganizerContext state) {
		return TimeBasedOrganizer.PATH_FORMAT.format(state.getTimestamp());
	}

	@Override
	protected String renderFileNameTag(TimeBasedOrganizerContext state) {
		return TimeBasedOrganizer.NAME_FORMAT.format(state.getTimestamp());
	}
}