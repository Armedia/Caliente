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
package com.armedia.caliente.engine.local.xml;

import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementRef;
import javax.xml.bind.annotation.XmlElementRefs;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;

import org.apache.commons.lang3.tuple.Pair;

import com.armedia.caliente.engine.local.exporter.LocalSearchType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "localQuerySearch.t", propOrder = {
	"dataSource", "sql", "skip", "count", "pathColumns", "directoryOrList", "postProcessors"
})
public class LocalQuerySearch {

	@XmlAttribute(name = "id", required = true)
	protected String id;

	@XmlElement(name = "dataSource", required = true)
	protected String dataSource;

	@XmlElement(name = "sql", required = true)
	protected String sql;

	@XmlElement(name = "skip", required = false)
	protected Integer skip;

	@XmlElement(name = "count", required = false)
	protected Integer count;

	@XmlElementWrapper(name = "path-columns", required = true)
	@XmlElement(name = "path-column", required = true)
	protected List<String> pathColumns;

	@XmlElementRefs({
		@XmlElementRef(name = "list", namespace = ObjectFactory.NS, type = JAXBElement.class, required = false),
		@XmlElementRef(name = "directory", namespace = ObjectFactory.NS, type = JAXBElement.class, required = false)
	})
	protected List<JAXBElement<String>> directoryOrList;

	@XmlTransient
	protected List<Pair<LocalSearchType, Path>> fileSystemSearches;

	@XmlElementWrapper(name = "post-processors", required = false)
	@XmlElement(name = "post-processor", required = false)
	protected List<LocalQueryPostProcessor> postProcessors;

	protected void afterUnmarshal(Unmarshaller u, Object parent) {
		this.fileSystemSearches = new ArrayList<>();
		if (this.directoryOrList != null) {
			for (JAXBElement<String> element : this.directoryOrList) {
				String tag = element.getName().getLocalPart();
				LocalSearchType t = LocalSearchType.CODEC.decode(tag);
				if (t != null) {
					try {
						Path p = Paths.get(element.getValue());
						this.fileSystemSearches.add(Pair.of(t, p));
					} catch (InvalidPathException e) {
						// invalid path - report it, move on?
					}
				}
			}
		}
	}

	protected void beforeMarshal(Marshaller m) {
		if ((this.fileSystemSearches != null) && !this.fileSystemSearches.isEmpty()) {
			this.directoryOrList = new ArrayList<>();
			ObjectFactory factory = new ObjectFactory();
			for (Pair<LocalSearchType, Path> p : this.fileSystemSearches) {
				JAXBElement<String> element = null;
				switch (p.getKey()) {
					case DIRECTORY:
						element = factory.createLocalQuerySearchDirectory(p.getValue().toString());
						break;
					case LIST_FILE:
						element = factory.createLocalQuerySearchList(p.getValue().toString());
						break;
					case SQL:
						// Do nothing...this is handled differently
					default:
						continue;
				}
				this.directoryOrList.add(element);
			}
		} else {
			this.directoryOrList = null;
		}
	}

	public String getId() {
		return this.id;
	}

	public void setId(String value) {
		this.id = value;
	}

	public String getDataSource() {
		return this.dataSource;
	}

	public void setDataSource(String value) {
		this.dataSource = value;
	}

	public String getSql() {
		return this.sql;
	}

	public void setSql(String value) {
		this.sql = value;
	}

	public Integer getSkip() {
		return this.skip;
	}

	public void setSkip(Integer value) {
		this.skip = value;
	}

	public Integer getCount() {
		return this.count;
	}

	public void setCount(Integer value) {
		this.count = value;
	}

	public List<String> getPathColumns() {
		if (this.pathColumns == null) {
			this.pathColumns = new ArrayList<>();
		}
		return this.pathColumns;
	}

	public List<LocalQueryPostProcessor> getPostProcessors() {
		if (this.postProcessors == null) {
			this.postProcessors = new ArrayList<>();
		}
		return this.postProcessors;
	}

	public List<Pair<LocalSearchType, Path>> getFileSystemSearches() {
		if (this.fileSystemSearches == null) {
			this.fileSystemSearches = new ArrayList<>();
		}
		return this.fileSystemSearches;
	}
}