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

import java.sql.SQLException;
import java.util.Objects;

import javax.sql.DataSource;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlValue;

import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.ResultSetHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.armedia.commons.utilities.function.CheckedFunction;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "localQuerySql.t", propOrder = {
	"sql"
})
public class LocalQuerySql {

	private static final ResultSetHandler<String> SINGLE_STRING = (rs) -> {
		if (!rs.next()) { return null; }
		String val = rs.getString(1);
		return (!rs.wasNull() ? val : null);
	};

	@XmlTransient
	private final Logger log = LoggerFactory.getLogger(getClass());

	@XmlValue
	protected String sql;

	@XmlAttribute(name = "dataSource", required = true)
	protected String dataSource;

	public String getSql() {
		return this.sql;
	}

	public void setSql(String value) {
		this.sql = value;
	}

	public String getDataSource() {
		return this.dataSource;
	}

	public void setDataSource(String value) {
		this.dataSource = value;
	}

	public CheckedFunction<String, String, SQLException> getSearch(final DataSource dataSource) throws SQLException {
		Objects.requireNonNull(dataSource, "Must provide a non-null DataSource");
		final String sql = this.sql;
		final QueryRunner qr = new QueryRunner(dataSource);
		return (param) -> qr.query(sql, LocalQuerySql.SINGLE_STRING);
	}
}