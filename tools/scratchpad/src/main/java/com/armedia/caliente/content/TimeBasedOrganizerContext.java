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