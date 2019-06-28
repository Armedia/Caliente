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