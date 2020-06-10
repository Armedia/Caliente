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

import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;

import javax.sql.DataSource;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.armedia.caliente.engine.exporter.ExportTarget;
import com.armedia.commons.utilities.CloseableIterator;
import com.armedia.commons.utilities.Tools;
import com.armedia.commons.utilities.io.CloseUtils;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "localQuery.t", propOrder = {
	"sql", "skip", "count", "pathColumns", "relativeTo", "postProcessors"
})
public class LocalQuery {

	@XmlTransient
	private final Logger log = LoggerFactory.getLogger(getClass());

	@XmlElement(name = "sql", required = true)
	protected String sql;

	@XmlElement(name = "skip", required = false)
	protected Integer skip = 0;

	@XmlElement(name = "count", required = false)
	protected Integer count = 0;

	@XmlElementWrapper(name = "path-columns", required = true)
	@XmlElement(name = "path-column", required = true)
	protected List<String> pathColumns;

	@XmlElement(name = "relative-to", required = false)
	protected String relativeTo;

	@XmlElementWrapper(name = "post-processors", required = false)
	@XmlElement(name = "post-processor") //
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

	public String getRelativeTo() {
		return this.relativeTo;
	}

	public void setRelativeTo(String value) {
		this.relativeTo = value;
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

	public Stream<ExportTarget> getStream(DataSource dataSource, final Function<String, ExportTarget> targetConverter)
		throws SQLException {
		Objects.requireNonNull(dataSource, "Must provide a non-null DataSource");
		Objects.requireNonNull(targetConverter, "Must provide a non-null target converter function");

		@SuppressWarnings("resource")
		CloseableIterator<ExportTarget> it = new CloseableIterator<ExportTarget>() {
			private final String id = getId();
			private int skip = 0;
			private int count = 0;
			private final String sql = getSql();
			private final Path root;
			private Set<Integer> candidates = null;
			private List<LocalQueryPostProcessor> postProcessors = Tools.freezeCopy(LocalQuery.this.postProcessors,
				true);
			private List<String> pathColumns = Tools.freezeCopy(LocalQuery.this.pathColumns, true);

			{
				Integer skip = getSkip();
				if ((skip == null) || (skip < 0)) {
					skip = 0;
				}
				this.skip = skip.intValue();

				Integer count = getCount();
				if ((count == null) || (count < 0)) {
					count = Integer.MAX_VALUE;
				}
				this.count = count.intValue();

				String relativeTo = getRelativeTo();
				if (StringUtils.isBlank(relativeTo)) {
					this.root = null;
				} else {
					this.root = Tools.canonicalize(Paths.get(relativeTo));
				}
			}

			private Connection c = null;
			private Statement s = null;
			private ResultSet rs = null;

			@Override
			protected void initialize() throws Exception {
				try {
					this.c = dataSource.getConnection();
					this.c.setAutoCommit(false);
					// Execute the query, stow the result set
					this.s = this.c.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
					this.rs = this.s.executeQuery(this.sql);

					Set<Integer> candidates = new LinkedHashSet<>();
					ResultSetMetaData md = this.rs.getMetaData();

					for (String p : this.pathColumns) {
						int index = -1;
						try {
							index = Integer.valueOf(p);
							if ((index < 1) || (index > md.getColumnCount())) {
								LocalQuery.this.log
									.warn("The column index [{}] is not valid for query [{}], ignoring it", p, getId());
								continue;
							}
						} catch (NumberFormatException e) {
							// Must be a column name
							try {
								index = this.rs.findColumn(p);
							} catch (SQLException ex) {
								LocalQuery.this.log.warn("No column named [{}] for query [{}], ignoring it", p,
									getId());
								continue;
							}
						}

						candidates.add(index);
					}

					if (candidates.isEmpty()) {
						throw new Exception(
							"No valid candidate columns found - can't continue with query [" + getId() + "]");
					}

					this.candidates = Tools.freezeSet(candidates);
				} catch (SQLException e) {
					doClose();
					throw e;
				}
			}

			@Override
			protected Result findNext() throws Exception {
				// First, skip whatever needs skipping
				while (this.skip > 0) {
					if (!this.rs.next()) { return null; }
					this.skip--;
				}

				if (this.count <= 0) { return null; }

				return buildResult();
			}

			private String postProcess(String str) {
				if (StringUtils.isEmpty(str)) { return str; }
				final String orig = str;
				for (LocalQueryPostProcessor p : this.postProcessors) {
					try {
						str = p.postProcess(str);
					} catch (Exception e) {
						if (LocalQuery.this.log.isDebugEnabled()) {
							LocalQuery.this.log.error("Exception caught from {} post-processor for [{}] (from [{}])",
								p.getType(), str, orig, e);
							return null;
						}
					}
					if (StringUtils.isEmpty(str)) {
						LocalQuery.this.log.error("Post-processing result for [{}] is null or empty, returning null",
							orig);
						return null;
					}
				}
				LocalQuery.this.log.debug("String [{}] post processed as [{}]", orig, str);
				return str;
			}

			private String relativize(String str) {
				if (this.root == null) { return str; }
				Path p = Paths.get(str);
				if (!p.startsWith(this.root)) {
					LocalQuery.this.log.warn("Path [{}] is not a child of [{}] and thus can't be relativized");
					return null;
				}
				return this.root.relativize(p).toString();
			}

			private Result buildResult() throws SQLException {
				try {
					while (this.rs.next()) {
						for (Integer column : this.candidates) {
							String str = this.rs.getString(column);
							if (this.rs.wasNull() || StringUtils.isEmpty(str)) {
								continue;
							}

							// Relativize the path, if necessary
							str = relativize(str);

							// Apply postProcessor
							str = postProcess(str);

							if (StringUtils.isEmpty(str)) {
								// If this resulted in an empty string, we try the next column
								continue;
							}

							// If we ended up with a non-empty string, we return it!
							return found(targetConverter.apply(str));
						}

						// If we get here, we found nothing, so we try the next record
						// on the result set
					}
					return null;
				} finally {
					this.count--;
				}
			}

			@Override
			protected void doClose() {
				if (this.c != null) {
					try {
						try {
							this.c.rollback();
						} catch (SQLException e) {
							if (LocalQuery.this.log.isDebugEnabled()) {
								LocalQuery.this.log.debug("Rollback failed on connection for query [{}]", this.id, e);
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

		return it.stream();
	}
}