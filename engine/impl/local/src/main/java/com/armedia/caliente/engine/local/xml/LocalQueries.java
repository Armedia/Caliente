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

import java.net.URL;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import javax.sql.DataSource;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;

import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.armedia.caliente.engine.dynamic.xml.XmlInstanceException;
import com.armedia.caliente.engine.dynamic.xml.XmlInstances;
import com.armedia.caliente.engine.dynamic.xml.XmlNotFoundException;
import com.armedia.caliente.engine.exporter.ExportTarget;
import com.armedia.commons.utilities.concurrent.BaseShareableLockable;
import com.armedia.commons.utilities.concurrent.MutexAutoLock;
import com.armedia.commons.utilities.concurrent.ShareableList;
import com.armedia.commons.utilities.concurrent.SharedAutoLock;
import com.armedia.commons.utilities.function.CheckedLazySupplier;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
	"rootPath", "dataSourceDefinitions", "searches", "historyIds", "versionLists"
})
@XmlRootElement(name = "local-queries")
public class LocalQueries extends BaseShareableLockable implements AutoCloseable {

	private static final XmlInstances<LocalQueries> INSTANCES = new XmlInstances<>(LocalQueries.class);

	public static LocalQueries getInstance(URL location) throws XmlInstanceException {
		return LocalQueries.INSTANCES
			.getInstance(Objects.requireNonNull(location, "Must provide a location to load the searches from"));
	}

	public static LocalQueries getInstance(String location) throws XmlInstanceException, XmlNotFoundException {
		return LocalQueries.INSTANCES
			.getInstance(Objects.requireNonNull(location, "Must provide a location to load the searches from"));
	}

	@XmlTransient
	private final Logger log = LoggerFactory.getLogger(getClass());

	@XmlElement(name = "root-path", required = false)
	protected String rootPath;

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
	protected List<LocalQuerySql> versionLists;

	@XmlTransient
	private final CheckedLazySupplier<LocalDataSources, Exception> dataSources = new CheckedLazySupplier<>(() -> {
		try (MutexAutoLock lock = autoMutexLock()) {
			return new LocalDataSources(getDataSourceDefinitions());
		}
	});

	protected void afterUnmarshal(Unmarshaller u, Object parent) {
		if (this.dataSourceDefinitions != null) {
			if (!ShareableList.class.isInstance(this.dataSourceDefinitions)) {
				this.dataSourceDefinitions = new ShareableList<>(this, this.dataSourceDefinitions);
			}
		}
		if (this.searches != null) {
			if (!ShareableList.class.isInstance(this.searches)) {
				this.searches = new ShareableList<>(this, this.searches);
			}
		}
	}

	public List<LocalQueryDataSource> getDataSourceDefinitions() {
		try (SharedAutoLock lock = autoSharedLock()) {
			if (this.dataSourceDefinitions == null) {
				this.dataSourceDefinitions = new ShareableList<>(this, new ArrayList<>());
			}
			return this.dataSourceDefinitions;
		}
	}

	public List<LocalQuerySearch> getSearches() {
		try (SharedAutoLock lock = autoSharedLock()) {
			if (this.searches == null) {
				this.searches = new ShareableList<>(this, new ArrayList<>());
			}
			return this.searches;
		}
	}

	public Stream<Path> searchPaths() throws Exception {
		try (SharedAutoLock lock = autoSharedLock()) {
			LocalDataSources dataSources = this.dataSources.getChecked();
			Stream<Path> ret = Stream.empty();
			for (LocalQuerySearch q : getSearches()) {
				DataSource ds = dataSources.getDataSource(q.getDataSource());
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
			return ret;
		}
	}

	public String getHistoryId(ExportTarget sourceTarget) throws SQLException {
		return null;
	}

	// Pair = versionLabel:path
	public List<Pair<String, Path>> getHistoryMembers(String historyId) throws SQLException {
		return null;
	}

	@Override
	public void close() throws Exception {
		try (MutexAutoLock lock = autoMutexLock()) {
			this.dataSources.applyIfSet(LocalDataSources::close);
			this.dataSources.reset();
		}
	}
}