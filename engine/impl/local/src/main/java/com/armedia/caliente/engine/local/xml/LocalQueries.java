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
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
	"rootPath", "dataSourceDefinitions", "postProcessors", "searches", "historyIds", "versionLists"
})
@XmlRootElement(name = "local-queries")
public class LocalQueries {

	@XmlTransient
	private final Logger log = LoggerFactory.getLogger(getClass());

	@XmlElement(name = "root-path", required = false)
	protected String rootPath;

	@XmlElementWrapper(name = "common-post-processors")
	@XmlElement(name = "post-processors")
	protected List<LocalQueryPostProcessorDef> postProcessors;

	@XmlElementWrapper(name = "data-sources", required = false)
	@XmlElement(name = "data-source", required = false)
	protected List<LocalQueryDataSource> dataSourceDefinitions;

	@XmlElementWrapper(name = "searches", required = false)
	@XmlElement(name = "search", required = false)
	protected List<LocalQuerySearch> searches;

	@XmlElementWrapper(name = "history-ids", required = false)
	@XmlElement(name = "history-id", required = false)
	protected List<LocalQuerySql> historyIds;

	@XmlElementWrapper(name = "version-lists", required = false)
	@XmlElement(name = "version-list", required = false)
	protected List<LocalQueryVersionList> versionLists;

	public List<LocalQueryDataSource> getDataSourceDefinitions() {
		if (this.dataSourceDefinitions == null) {
			this.dataSourceDefinitions = new ArrayList<>();
		}
		return this.dataSourceDefinitions;
	}

	public List<LocalQueryPostProcessorDef> getPostProcessorDefs() {
		if (this.postProcessors == null) {
			this.postProcessors = new ArrayList<>();
		}
		return this.postProcessors;
	}

	public List<LocalQuerySearch> getSearches() {
		if (this.searches == null) {
			this.searches = new ArrayList<>();
		}
		return this.searches;
	}

	public List<LocalQuerySql> getHistoryIdQueries() {
		if (this.historyIds == null) {
			this.historyIds = new ArrayList<>();
		}
		return this.historyIds;
	}

	public List<LocalQueryVersionList> getVersionListQueries() {
		if (this.versionLists == null) {
			this.versionLists = new ArrayList<>();
		}
		return this.versionLists;
	}
}