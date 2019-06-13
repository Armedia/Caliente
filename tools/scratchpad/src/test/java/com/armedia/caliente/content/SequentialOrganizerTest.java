package com.armedia.caliente.content;

import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class SequentialOrganizerTest {

	private final SequentialOrganizer organizer = new SequentialOrganizer();

	@Test
	public void testRenderIntermediatePath() {
		final String appName = "Application-Name";
		final String clientId = UUID.randomUUID().toString();
		final List<Pair<Long, String>> values = new LinkedList<>();

		values.add(Pair.of(Long.MIN_VALUE, "80/000/000/000/000"));
		values.add(Pair.of(0L, "00/000/000/000/000"));
		for (long i = 0; i < 256; i++) {
			values.add(Pair.of(i, "00/000/000/000/000"));
		}
		for (long i = 256; i < 512; i++) {
			values.add(Pair.of(i, "00/000/000/000/001"));
		}
		for (long i = 512; i < 768; i++) {
			values.add(Pair.of(i, "00/000/000/000/002"));
		}
		values.add(Pair.of(0x0123456789ABCDEFL, "01/234/567/89a/bcd"));
		values.add(Pair.of(0xFEDCBA9876543210L, "fe/dcb/a98/765/432"));
		values.add(Pair.of(Long.MAX_VALUE, "7f/fff/fff/fff/fff"));

		for (Pair<Long, String> p : values) {
			OrganizerContext context = this.organizer.newState(appName, clientId, p.getLeft());
			Assertions.assertEquals(p.getRight(), this.organizer.renderIntermediatePath(context));
		}
	}
}