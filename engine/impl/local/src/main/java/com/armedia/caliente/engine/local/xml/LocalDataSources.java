package com.armedia.caliente.engine.local.xml;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.armedia.commons.utilities.Tools;
import com.armedia.commons.utilities.concurrent.BaseShareableLockable;

public class LocalDataSources extends BaseShareableLockable implements AutoCloseable {

	private final Logger log = LoggerFactory.getLogger(getClass());

	private volatile boolean closed = false;
	private final Map<String, DataSource> dataSources;

	LocalDataSources(List<LocalQueryDataSource> list) throws Exception {
		Objects.requireNonNull(list, "Must provide a list of DataSource definitions");
		final Map<String, DataSource> dataSources = new LinkedHashMap<>();
		try {
			for (LocalQueryDataSource ds : list) {
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
		this.dataSources = Tools.freezeMap(dataSources);
	}

	DataSource getDataSource(String name) {
		return shareLocked(() -> this.dataSources.get(name));
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

	@Override
	public void close() {
		shareLockedUpgradable(() -> this.closed, () -> {
			this.dataSources.values().forEach(this::close);
			this.closed = true;
		});
	}
}