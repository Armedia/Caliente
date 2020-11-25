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
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.armedia.caliente.engine.dynamic.xml.metadata.MetadataSet;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
	"rootPath", "failOnInvalidPath", "dataSourceDefinitions", "postProcessors", "searches", "historyIds",
	"versionLists", "metadata"
})
@XmlRootElement(name = "local-queries")
public class LocalQueries {

	public static final boolean DEFAULT_FAIL_ON_INVALID_PATH = true;

	@XmlTransient
	private final Logger log = LoggerFactory.getLogger(getClass());

	@XmlElement(name = "root-path", required = false)
	protected String rootPath;

	@XmlElement(name = "fail-on-invalid-path", required = false)
	protected Boolean failOnInvalidPath;

	@XmlElementWrapper(name = "common-post-processors")
	@XmlElement(name = "post-processors")
	protected List<LocalQueryPostProcessorDef> postProcessors;

	@XmlElementWrapper(name = "data-sources", required = false)
	@XmlElement(name = "data-source", required = false)
	protected List<LocalQueryDataSource> dataSourceDefinitions;

	@XmlElementWrapper(name = "searches", required = false)
	@XmlElements({
		@XmlElement(name = "sql", type = LocalSearchBySql.class, required = false), //
		@XmlElement(name = "dir", type = LocalSearchByPath.class, required = false), //
		@XmlElement(name = "list", type = LocalSearchByList.class, required = false), //
	})
	protected List<LocalSearchBase> searches;

	@XmlElementWrapper(name = "history-ids", required = false)
	@XmlElement(name = "history-id", required = false)
	protected List<LocalQuerySql> historyIds;

	@XmlElementWrapper(name = "version-lists", required = false)
	@XmlElement(name = "version-list", required = false)
	protected List<LocalQueryVersionList> versionLists;

	@XmlElementWrapper(name = "metadata", required = false)
	@XmlElement(name = "metadata-set", required = false)
	protected List<MetadataSet> metadata;

	public String getRootPath() {
		return this.rootPath;
	}

	public void setRootPath(String rootPath) {
		this.rootPath = rootPath;
	}

	public boolean isFailOnInvalidPath() {
		return (this.failOnInvalidPath != null ? this.failOnInvalidPath.booleanValue()
			: LocalQueries.DEFAULT_FAIL_ON_INVALID_PATH);
	}

	public void setFailOnInvalidPath(Boolean failOnInvalidPath) {
		this.failOnInvalidPath = failOnInvalidPath;
	}

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

	public List<LocalSearchBase> getSearches() {
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

	public List<MetadataSet> getMetadata() {
		if (this.metadata == null) {
			this.metadata = new ArrayList<>();
		}
		return this.metadata;
	}
}