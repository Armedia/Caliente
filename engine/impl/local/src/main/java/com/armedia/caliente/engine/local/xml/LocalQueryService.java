package com.armedia.caliente.engine.local.xml;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

import javax.sql.DataSource;

import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.ResultSetHandler;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.armedia.caliente.engine.dynamic.xml.XmlInstances;
import com.armedia.commons.utilities.CloseableIterator;
import com.armedia.commons.utilities.Tools;
import com.armedia.commons.utilities.concurrent.BaseShareableLockable;
import com.armedia.commons.utilities.concurrent.SharedAutoLock;
import com.armedia.commons.utilities.io.CloseUtils;

public class LocalQueryService extends BaseShareableLockable implements AutoCloseable {

	private static final XmlInstances<LocalQueries> LOCAL_QUERIES = new XmlInstances<>(LocalQueries.class);

	private class Search {
		private final DataSource dataSource;
		private final String id;
		private final List<String> pathColumns;
		private final String sql;
		private final int skip;
		private final int count;
		private final List<LocalQueryPostProcessor> postProcessors;

		private Search(LocalQuerySearch definition) throws Exception {
			this.dataSource = LocalQueryService.this.dataSources.get(definition.getDataSource());
			Objects.requireNonNull(this.dataSource, "Must provide a non-null DataSource");

			this.id = definition.getId();

			Integer skip = definition.getSkip();
			if ((skip != null) && (skip < 0)) {
				skip = null;
			}
			this.skip = (skip != null ? skip.intValue() : -1);

			Integer count = definition.getCount();
			if ((count != null) && (count < 0)) {
				count = null;
			}
			this.count = (count != null ? count.intValue() : -1);

			this.sql = definition.getSql();
			// TODO: Perform a sanity check?

			this.postProcessors = Tools.freezeCopy(definition.getPostProcessors(), true);

			this.pathColumns = Tools.freezeCopy(definition.getPathColumns(), true);
			if (this.pathColumns.isEmpty()) { throw new Exception("No candidate columns given"); }
		}

		private Stream<Path> build() {
			@SuppressWarnings("resource")
			CloseableIterator<Path> it = new CloseableIterator<Path>() {

				private Connection c = null;
				private Statement s = null;
				private ResultSet rs = null;
				private Set<Integer> candidates = null;

				@Override
				protected void initialize() throws Exception {
					try {
						this.c = Search.this.dataSource.getConnection();
						this.c.setAutoCommit(false);
						// Execute the query, stow the result set
						this.s = this.c.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
						this.rs = this.s.executeQuery(Search.this.sql);

						Set<Integer> candidates = new LinkedHashSet<>();
						ResultSetMetaData md = this.rs.getMetaData();

						for (String p : Search.this.pathColumns) {
							int index = -1;
							try {
								index = Integer.valueOf(p);
								if ((index < 1) || (index > md.getColumnCount())) {
									LocalQueryService.this.log.warn(
										"The column index [{}] is not valid for query [{}], ignoring it", p,
										Search.this.id);
									continue;
								}
							} catch (NumberFormatException e) {
								// Must be a column name
								try {
									index = this.rs.findColumn(p);
								} catch (SQLException ex) {
									LocalQueryService.this.log.warn("No column named [{}] for query [{}], ignoring it",
										p, Search.this.id);
									continue;
								}
							}

							candidates.add(index);
						}

						if (candidates.isEmpty()) {
							throw new Exception("No valid candidate columns found - can't continue with query ["
								+ Search.this.id + "]");
						}

						this.candidates = Tools.freezeSet(candidates);
					} catch (SQLException e) {
						doClose();
						throw e;
					}
				}

				private String postProcess(String str) {
					if (StringUtils.isEmpty(str)) { return str; }
					final String orig = str;
					for (LocalQueryPostProcessor p : Search.this.postProcessors) {
						try {
							final String prev = str;
							str = p.postProcess(str);
							if (LocalQueryService.this.log.isTraceEnabled()) {
								LocalQueryService.this.log.trace("Post-processed [{}] into [{}] (by {})", prev, str, p);
							}
						} catch (Exception e) {
							if (LocalQueryService.this.log.isDebugEnabled()) {
								LocalQueryService.this.log.error(
									"Exception caught from {} post-processor for [{}] (from [{}]), returning null", p,
									str, orig, e);
							}
							str = null;
						}

						if (StringUtils.isEmpty(str)) {
							LocalQueryService.this.log
								.error("Post-processing result for [{}] is null or empty, returning null", orig);
							return null;
						}
					}
					LocalQueryService.this.log.debug("Final result of string post-processing: [{}] -> [{}]", orig, str);
					return str;
				}

				@Override
				protected Result findNext() throws Exception {
					while (this.rs.next()) {
						for (Integer column : this.candidates) {
							String str = this.rs.getString(column);
							if (this.rs.wasNull() || StringUtils.isEmpty(str)) {
								continue;
							}

							// Apply postProcessor
							str = postProcess(str);

							if (StringUtils.isEmpty(str)) {
								// If this resulted in an empty string, we try the next column
								continue;
							}

							// If we ended up with a non-empty string, we return it!
							return found(Paths.get(str).normalize());
						}

						// If we get here, we found nothing, so we try the next record
						// on the result set
					}
					return null;
				}

				@Override
				protected void doClose() {
					if (this.c != null) {
						try {
							try {
								this.c.rollback();
							} catch (SQLException e) {
								if (LocalQueryService.this.log.isDebugEnabled()) {
									LocalQueryService.this.log.debug("Rollback failed on connection for query [{}]",
										Search.this.id, e);
								}
							}
							CloseUtils.closeQuietly(this.rs, this.s, this.c);
						} finally {
							this.rs = null;
							this.s = null;
							this.c = null;
						}
					}
				}
			};

			// Make sure we skip all null and empty strings, and apply the conversion
			Stream<Path> stream = it.stream() //
				.filter(Objects::nonNull) //
			//
			;

			if (this.skip > 0) {
				stream = stream.skip(this.skip);
			}
			if (this.count >= 0) {
				stream = stream.limit(this.count);
			}

			return stream;
		}

	}

	private static final ResultSetHandler<String> HANDLER_HISTORY_ID = (rs) -> {
		if (!rs.next()) { return null; }
		String val = rs.getString(1);
		return (!rs.wasNull() ? val : null);
	};

	private static final ResultSetHandler<List<Pair<String, Path>>> HANDLER_MEMBER_LIST = (rs) -> {
		List<Pair<String, Path>> ret = new LinkedList<>();
		while (rs.next()) {
			String key = rs.getString(1);
			if (rs.wasNull()) {
				continue;
			}
			String value = rs.getString(2);
			if (rs.wasNull()) {
				continue;
			}
			if (StringUtils.isEmpty(key) || StringUtils.isEmpty(value)) {
				continue;
			}

			ret.add(Pair.of(key, Paths.get(value)));
		}
		return ret;
	};

	private class Query<T> {
		private final DataSource dataSource;
		private final String id;
		private final String sql;
		private final QueryRunner qr;
		private final ResultSetHandler<T> handler;

		private Query(String label, LocalQuerySql sql, ResultSetHandler<T> handler) {
			Objects.requireNonNull(sql, "Must provide a non-null LocalQuerySql instance");
			this.handler = Objects.requireNonNull(handler, "Must provide a non-null ResultSetHandler instance");
			this.id = sql.getId();

			this.dataSource = LocalQueryService.this.dataSources.get(sql.getDataSource());
			if (this.dataSource == null) {
				throw new NullPointerException(String.format("No dataSource named [%s] was found for %s query [%s]",
					sql.getDataSource(), label, this.id));
			}

			this.sql = sql.getSql();
			if (StringUtils.isEmpty(this.sql)) {
				throw new NullPointerException(String.format("%s query [%s] has no SQL", label, this.id));
			}

			this.qr = new QueryRunner(this.dataSource);
		}

		private T run(Object... params) throws SQLException {
			return this.qr.query(this.sql, this.handler, params);
		}
	}

	private final Logger log = LoggerFactory.getLogger(getClass());
	private volatile boolean closed = false;
	private final Map<String, DataSource> dataSources;
	private final Map<String, Search> searches;
	private final Map<String, Query<String>> history;
	private final Map<String, Query<List<Pair<String, Path>>>> members;

	public LocalQueryService() throws Exception {
		this(LocalQueryService.LOCAL_QUERIES.getInstance());
	}

	public LocalQueryService(String location) throws Exception {
		this(LocalQueryService.LOCAL_QUERIES.getInstance(location));
	}

	public LocalQueryService(URL location) throws Exception {
		this(LocalQueryService.LOCAL_QUERIES.getInstance(location));
	}

	public LocalQueryService(LocalQueries queries) throws Exception {
		if (queries == null) {
			queries = LocalQueryService.LOCAL_QUERIES.getInstance();
		}
		Objects.requireNonNull(queries, "Must provide a LocalQueries instance");
		final Map<String, DataSource> dataSources = new LinkedHashMap<>();
		try {
			for (LocalQueryDataSource ds : queries.getDataSourceDefinitions()) {
				if (dataSources.containsKey(ds.getName())) {
					this.log.warn("Duplicate data source names found: [{}]] - will only use the first one defined",
						ds.getName());
					continue;
				}

				DataSource dataSource = ds.build();
				if (dataSource == null) {
					this.log.warn("DataSource [{}] failed to be built", ds.getName());
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

		Map<String, Search> searchMap = new LinkedHashMap<>();
		for (LocalQuerySearch search : queries.getSearches()) {
			String id = search.getId();
			if (StringUtils.isEmpty(id)) {
				this.log.warn("Empty ID found for a search - can't use it!");
				continue;
			}

			try {
				searchMap.put(id, new Search(search));
			} catch (Exception e) {
				this.log.error("Failed to construct the search [{}]", id, e);
			}
		}
		this.searches = Tools.freezeMap(searchMap);

		Map<String, Query<String>> historyMap = new LinkedHashMap<>();
		for (LocalQuerySql sql : queries.getHistoryIdQueries()) {
			String id = sql.getId();
			if (StringUtils.isEmpty(id)) {
				this.log.warn("Empty ID found for a historyId query - can't use it!");
				continue;
			}

			historyMap.put(id, new Query<>("History id", sql, LocalQueryService.HANDLER_HISTORY_ID));
		}
		this.history = Tools.freezeMap(historyMap);

		Map<String, Query<List<Pair<String, Path>>>> membersMap = new LinkedHashMap<>();
		for (LocalQuerySql sql : queries.getHistoryIdQueries()) {
			String id = sql.getId();
			if (StringUtils.isEmpty(id)) {
				this.log.warn("Empty ID found for a historyId query - can't use it!");
				continue;
			}

			membersMap.put(id, new Query<>("Member list", sql, LocalQueryService.HANDLER_MEMBER_LIST));
		}
		this.members = Tools.freezeMap(membersMap);
	}

	public Stream<Path> searchPaths() throws Exception {
		final SharedAutoLock lock = autoSharedLock();
		Stream<Path> ret = Stream.empty();
		for (String id : this.searches.keySet()) {
			Search search = this.searches.get(id);
			ret = Stream.concat(ret, search.build());
		}
		// Make sure we hold the read lock all the way until the streams are exhausted
		return ret.onClose(lock::close);
	}

	public String getHistoryId(String objectId) throws Exception {
		try (SharedAutoLock lock = autoSharedLock()) {
			for (String id : this.history.keySet()) {
				Query<String> q = this.history.get(id);
				try {
					final String ret = q.run(objectId);
					if (!StringUtils.isBlank(ret)) { return ret; }
				} catch (Exception e) {
					this.log.warn("Exception caught from history seek query [{}] while searching for ID [{}]", id,
						objectId, e);
					continue;
				}
			}
			return null;
		}
	}

	public List<Pair<String, Path>> getHistoryMembers(String historyId) throws Exception {
		try (SharedAutoLock lock = autoSharedLock()) {
			for (String id : this.members.keySet()) {
				Query<List<Pair<String, Path>>> q = this.members.get(id);
				try {
					final List<Pair<String, Path>> ret = q.run(historyId);
					ret.removeIf(Objects::isNull);
					if ((ret != null) && !ret.isEmpty()) { return Tools.freezeList(ret); }
				} catch (Exception e) {
					this.log.warn("Exception caught from history members query [{}] while searching for ID [{}]", id,
						historyId, e);
					continue;
				}
			}
			return Collections.emptyList();
		}
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