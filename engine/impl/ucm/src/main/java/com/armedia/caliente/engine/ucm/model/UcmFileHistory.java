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
package com.armedia.caliente.engine.ucm.model;

import java.net.URI;
import java.util.Iterator;
import java.util.List;

import com.armedia.commons.utilities.Tools;

public class UcmFileHistory extends UcmModelObject implements Iterable<UcmRevision> {

	private final List<UcmRevision> versions;
	private final UcmRevision firstRevision;
	private final UcmRevision lastRevision;

	public UcmFileHistory(UcmModel model, URI uri, List<UcmRevision> versions) {
		super(model, uri);
		this.versions = Tools.freezeCopy(versions);
		this.firstRevision = this.versions.get(0);
		this.lastRevision = this.versions.get(this.versions.size() - 1);
	}

	public UcmRevision getVersion(int revision) {
		return this.versions.get(revision);
	}

	public UcmRevision getFirstRevision() {
		return this.firstRevision;
	}

	public int getRevisionCount() {
		return this.versions.size();
	}

	public UcmRevision getLastRevision() {
		return this.lastRevision;
	}

	@Override
	public Iterator<UcmRevision> iterator() {
		return this.versions.iterator();
	}
}