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
package com.armedia.caliente.engine.ucm.model;

import java.net.URI;
import java.util.Collection;

import oracle.stellent.ridc.model.DataObject;
import oracle.stellent.ridc.model.DataResultSet.Field;

public class UcmRevision {

	public static enum Status {
		//
		RELEASED,
		//
		;
	}

	private final URI uri;
	private final String format;
	private final String id;
	private final String processingState;
	private final String revLabel;
	private final int revisionId;
	private final Status status;

	UcmRevision(URI fileUri, DataObject obj, Collection<Field> structure) {
		UcmAttributes data = new UcmAttributes(obj, structure);
		this.uri = fileUri;
		this.format = data.getString(UcmAtt.dFormat);
		this.id = data.getString(UcmAtt.dID);
		this.processingState = data.getString(UcmAtt.dProcessingState);
		this.revLabel = data.getString(UcmAtt.dRevLabel);
		this.revisionId = data.getInteger(UcmAtt.dRevisionID);
		this.status = Status.valueOf(data.getString(UcmAtt.dStatus).toUpperCase());
	}

	public URI getUri() {
		return this.uri;
	}

	public String getFormat() {
		return this.format;
	}

	public String getId() {
		return this.id;
	}

	public String getProcessingState() {
		return this.processingState;
	}

	public Status getStatus() {
		return this.status;
	}

	public String getRevLabel() {
		return this.revLabel;
	}

	public int getRevisionId() {
		return this.revisionId;
	}
}