package com.armedia.caliente.content;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class TimeBasedOrganizerTest {

	private final TimeBasedOrganizer organizer = new TimeBasedOrganizer("foler", "file");
	private final ZoneId zoneId = TimeBasedOrganizerContext.ZONE_ID;

	@Test
	public void testRenderIntermediatePath() {
		final String appName = "Application-Name";
		final String clientId = UUID.randomUUID().toString();
		final List<Pair<ZonedDateTime, String>> values = new LinkedList<>();

		int[] years = {
			2019, 2020
		};
		int[] months = {
			1, 3, 6, 9, 11, 12
		};
		int[] days = {
			1, 13, 25, 27, 30
		};
		int[] hours = {
			0, 1, 5, 9, 11, 12, 15, 17, 19, 23
		};
		int[] minutes = {
			0, 15, 23, 35, 48, 59
		};

		for (int y : years) {
			for (int M : months) {
				for (int d : days) {
					for (int h : hours) {
						for (int m : minutes) {
							for (int s = 0; s < 60; s++) {
								LocalDateTime ldt = LocalDateTime.of(y, M, d, h, m, s, 123456789);
								String path = String.format("%04d/%02d%02d/%02d%02d", y, M, d, h, m);
								values.add(Pair.of(ZonedDateTime.of(ldt, this.zoneId), path));
							}
						}
					}
				}
			}
		}

		for (Pair<ZonedDateTime, String> p : values) {
			TimeBasedOrganizerContext context = new TimeBasedOrganizerContext(appName, clientId, 0, p.getLeft());
			Assertions.assertEquals(p.getRight(), this.organizer.renderIntermediatePath(context));
		}
	}

	@Test
	public void testRenderFileNameTag() {
		final String appName = "Application-Name";
		final String clientId = UUID.randomUUID().toString();
		final List<Pair<ZonedDateTime, String>> values = new LinkedList<>();

		int[] years = {
			2019, 2020
		};
		int[] months = {
			1, 3, 6, 9, 11, 12
		};
		int[] days = {
			1, 13, 25, 27, 30
		};
		int[] hours = {
			0, 1, 5, 9, 11, 12, 15, 17, 19, 23
		};
		int[] minutes = {
			0, 15, 23, 35, 48, 59
		};

		for (int y : years) {
			for (int M : months) {
				for (int d : days) {
					for (int h : hours) {
						for (int m : minutes) {
							for (int s = 0; s < 60; s++) {
								int S = 123456789;
								LocalDateTime ldt = LocalDateTime.of(y, M, d, h, m, s, S);
								String file = String.format("%04d%02d%02d%02d%02d%02d%03d", y, M, d, h, m, s,
									TimeUnit.NANOSECONDS.toMillis(S));
								values.add(Pair.of(ZonedDateTime.of(ldt, this.zoneId), file));
							}
						}
					}
				}
			}
		}

		for (Pair<ZonedDateTime, String> p : values) {
			TimeBasedOrganizerContext context = new TimeBasedOrganizerContext(appName, clientId, 0, p.getLeft());
			Assertions.assertEquals(p.getRight(), this.organizer.renderFileNameTag(context));
		}
	}

}