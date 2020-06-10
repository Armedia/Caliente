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

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

import javax.sql.DataSource;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.armedia.caliente.engine.dynamic.xml.XmlInstanceException;
import com.armedia.caliente.engine.dynamic.xml.XmlInstances;
import com.armedia.caliente.engine.dynamic.xml.XmlNotFoundException;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
	"dataSources", "queries"
})
@XmlRootElement(name = "local-queries")
public class LocalQueries {

	private static final XmlInstances<LocalQueries> INSTANCES = new XmlInstances<>(LocalQueries.class);

	public static LocalQueries getInstance(URL location) throws XmlInstanceException {
		return LocalQueries.INSTANCES
			.getInstance(Objects.requireNonNull(location, "Must provide a location to load the queries from"));
	}

	public static LocalQueries getInstance(String location) throws XmlInstanceException, XmlNotFoundException {
		return LocalQueries.INSTANCES
			.getInstance(Objects.requireNonNull(location, "Must provide a location to load the queries from"));
	}

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

	private Map<String, DataSource> buildDataSources() throws Exception {
		final Map<String, DataSource> dataSources = new LinkedHashMap<>();
		try {
			for (LocalQueryDataSource ds : getDataSources()) {
				if (dataSources.containsKey(ds.getName())) {
					this.log.warn("Duplicate data source names found: [{}]] - will only use the first one defined",
						ds.getName());
					continue;
				}

				DataSource dataSource = ds.getInstance();
				if (dataSource == null) {
					this.log.warn("DataSource [{}] failed to construct", ds.getName());
					continue;
				}

				dataSources.put(ds.getName(), dataSource);
			}
		} catch (Exception e) {
			// If there's an exception, we close whatever was opened
			dataSources.values().forEach(this::close);
			throw e;
		}
		if (dataSources.isEmpty()) { throw new Exception("No datasources were successfully built"); }
		return dataSources;
	}

	public Stream<Path> execute() throws Exception {
		Map<String, DataSource> dataSources = buildDataSources();
		Stream<Path> ret = Stream.empty();
		for (LocalQuery q : getQueries()) {
			DataSource ds = dataSources.get(q.getDataSource());
			if (ds == null) {
				this.log.warn("Query [{}] references undefined DataSource [{}], ignoring", q.getId(),
					q.getDataSource());
				continue;
			}
			try {
				ret = Stream.concat(ret, q.getStream(ds));
			} catch (Exception e) {
				this.log.warn("Query [{}] failed to construct the stream, ignoring", q.getId(), e);
			}
		}
		return ret.onClose(() -> dataSources.values().forEach(this::close));
	}

	private void close(DataSource dataSource) {
		if (dataSource == null) { return; }
		try {
			// We do it like this since this is faster than reflection
			if (AutoCloseable.class.isInstance(dataSource)) {
				AutoCloseable.class.cast(dataSource).close();
			} else {
				// No dice on the static linking, does it have a public void close() method?
				Method m = null;
				try {
					m = dataSource.getClass().getMethod("close");
				} catch (Exception ex) {
					// Do nothing...
				}
				if ((m != null) && Modifier.isPublic(m.getModifiers())) {
					m.invoke(dataSource);
				}
			}
		} catch (Exception e) {
			if (this.log.isDebugEnabled()) {
				this.log.debug("Failed to close a datasource", e);
			}
		}
	}
}