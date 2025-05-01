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
package com.armedia.caliente.engine.xml.importer.jaxb;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.xml.bind.Marshaller;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "documentIndexEntry.t", propOrder = {
	"historyId", "count", "versions"
})
public class DocumentIndexEntryT {

	@XmlElement(name = "historyId", required = true)
	protected String historyId;

	@XmlElement(name = "count", required = true)
	protected long count;

	@XmlElement(name = "version", required = true)
	protected List<DocumentIndexVersionT> versions;

	protected void beforeMarshal(Marshaller m) {
		this.count = getVersions().size();
	}

	public String getHistoryId() {
		return this.historyId;
	}

	public void setHistoryId(String value) {
		this.historyId = value;
	}

	public long getCount() {
		return getVersions().size();
	}

	protected final List<DocumentIndexVersionT> getVersions() {
		if (this.versions == null) {
			this.versions = new ArrayList<>();
		}
		return this.versions;
	}

	public boolean add(DocumentIndexVersionT v) {
		return getVersions().add(v);
	}

	public boolean add(Collection<DocumentIndexVersionT> v) {
		return getVersions().addAll(v);
	}

	public boolean remove(DocumentIndexVersionT v) {
		return getVersions().remove(v);
	}

	public boolean remove(Collection<DocumentIndexVersionT> v) {
		return getVersions().removeAll(v);
	}

	public boolean contains(DocumentIndexVersionT v) {
		return getVersions().contains(v);
	}

	public boolean contains(Collection<DocumentIndexVersionT> v) {
		return getVersions().containsAll(v);
	}

	public void clear() {
		getVersions().clear();
	}

	@Override
	public String toString() {
		return String.format("DocumentIndexEntryT [historyId=%s, count=%,d, versions=%s]", this.historyId, getCount(),
			this.versions);
	}
}