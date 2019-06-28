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

public class OrganizerContext implements AutoCloseable {

	private final String applicationName;
	private final String clientId;
	private final long fileId;
	private final String fileIdHex;

	public OrganizerContext(String applicationName, String clientId, long fileId) {
		this.applicationName = applicationName;
		this.clientId = clientId;
		this.fileId = fileId;
		this.fileIdHex = String.format("%016x", fileId);
	}

	public final String getApplicationName() {
		return this.applicationName;
	}

	public final String getClientId() {
		return this.clientId;
	}

	public final long getFileId() {
		return this.fileId;
	}

	public final String getFileIdHex() {
		return this.fileIdHex;
	}

	@Override
	public void close() {
	}
}