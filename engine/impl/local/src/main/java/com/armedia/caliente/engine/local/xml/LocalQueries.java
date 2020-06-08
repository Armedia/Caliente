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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.armedia.caliente.engine.exporter.ExportTarget;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
	"dataSources", "queries"
})
@XmlRootElement(name = "local-queries")
public class LocalQueries {

	@XmlTransient
	private final Logger log = LoggerFactory.getLogger(getClass());

	@XmlElement(name = "data-source", required = true)
	protected List<LocalQueryDataSource> dataSources;

	@XmlElement(name = "query", required = true)
	protected List<LocalQuery> queries;

	public List<LocalQueryDataSource> getDataSources() {
		if (this.dataSources == null) {
			this.dataSources = new ArrayList<>();
		}
		return this.dataSources;
	}

	public List<LocalQuery> getQueries() {
		if (this.queries == null) {
			this.queries = new ArrayList<>();
		}
		return this.queries;
	}

	private Map<String, LocalQueryDataSource> buildDataSources() throws Exception {
		Map<String, LocalQueryDataSource> dataSources = new LinkedHashMap<>();
		for (LocalQueryDataSource ds : getDataSources()) {
			if (dataSources.containsKey(ds.getName())) {
				this.log.warn("Duplicate data source names found: [{}]] - will only use the first one defined",
					ds.getName());
				continue;
			}

			dataSources.put(ds.getName(), ds);
		}
		if (dataSources.isEmpty()) { throw new Exception("No datasources were successfully built"); }
		return dataSources;
	}

	public Stream<ExportTarget> execute() throws Exception {
		Map<String, LocalQueryDataSource> dataSources = buildDataSources();
		Stream<ExportTarget> ret = Stream.empty();
		for (LocalQuery q : getQueries()) {
			LocalQueryDataSource ds = dataSources.get(q.getDataSource());
			if (ds == null) {
				this.log.warn("Query [{}] references undefined DataSource [{}], ignoring", q.getId(),
					q.getDataSource());
				continue;
			}
			ret = Stream.concat(ret, q.getStream(ds::getConnection));
		}

		return ret.onClose(() -> dataSources.values().forEach(LocalQueryDataSource::close));
	}

}