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

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "localQuerySearch.t", propOrder = {
	"sql", "skip", "count", "pathColumns", "postProcessors"
})
public class LocalQuerySearch {

	@XmlElement(name = "sql", required = true)
	protected String sql;

	@XmlElement(name = "skip", required = false)
	protected Integer skip = 0;

	@XmlElement(name = "count", required = false)
	protected Integer count = 0;

	@XmlElementWrapper(name = "path-columns", required = true)
	@XmlElement(name = "path-column", required = true)
	protected List<String> pathColumns;

	@XmlElementWrapper(name = "post-processors", required = false)
	@XmlElement(name = "post-processor", required = false)
	protected List<LocalQueryPostProcessor> postProcessors;

	@XmlAttribute(name = "id", required = true)
	protected String id;

	@XmlAttribute(name = "dataSource", required = true)
	protected String dataSource;

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

}