/*******************************************************************************
 * #%L
 * Armedia Caliente
 * %%
 * Copyright (c) 2010 - 2019 Armedia LLC
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