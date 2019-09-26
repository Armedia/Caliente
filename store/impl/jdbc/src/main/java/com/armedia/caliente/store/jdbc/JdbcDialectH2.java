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
package com.armedia.caliente.store.jdbc;

import java.sql.DatabaseMetaData;
import java.sql.SQLException;

import org.apache.commons.dbutils.ResultSetHandler;
import org.apache.commons.lang3.StringUtils;

public class JdbcDialectH2 extends JdbcDialect {

	private static final ResultSetHandler<Long> OBJECT_NUMBER_HANDLER = (rs) -> (rs.next() ? rs.getLong(1) : null);

	private static final String LOAD_OBJECTS_BY_ID = //
		"       select o.*, n.new_name " + //
			"     from cmf_object o left outer join cmf_alt_name n on (o.object_id = n.object_id), " + //
			"          table(x varchar=?) t " + //
			"    where o.object_id = t.x " + //
			"      and o.object_type = ? " + //
			" order by o.tier_id, o.history_id, o.object_number" //
	;

	private static final String LOAD_OBJECTS_BY_HISTORY_ID = //
		"       select o.*, n.new_name " + //
			"     from cmf_object o left outer join cmf_alt_name n on (o.object_id = n.object_id), " + //
			"          table(x varchar=?) t " + //
			"    where o.history_id = t.x " + //
			"      and o.object_type = ? " + //
			" order by o.tier_id, o.history_id, o.object_number" //
	;

	private static final String LOAD_OBJECTS_BY_ID_CURRENT = //
		"       select o.*, n.new_name " + //
			"     from cmf_object o left outer join cmf_alt_name n on (o.object_id = n.object_id), " + //
			"          table(x varchar=?) t " + //
			"    where o.object_id = t.x " + //
			"      and o.history_current = true " + //
			"      and o.object_type = ? " + //
			" order by o.tier_id, o.history_id, o.object_number" //
	;

	private static final String LOAD_OBJECT_NAMES_BY_ID = //
		"       select o.object_id, o.object_name, n.new_name " + //
			"     from cmf_object o left outer join cmf_alt_name n on (o.object_id = n.object_id), " + //
			"          table(x varchar=?) t " + //
			"    where o.object_id = t.x " + //
			" order by o.object_id " //
	;

	private static final String LOAD_OBJECT_NAMES_BY_ID_CURRENT = //
		"       select o.object_id, o.object_label, o2.object_name, n.new_name " + //
			"     from cmf_object o, table(x varchar=?) t, " + //
			"          cmf_object o2 left outer join cmf_alt_name n on (o2.object_id = n.object_id) " + //
			"    where o.object_id = t.x " + //
			"      and o.object_type = o2.object_type " + //
			"      and o.history_id = o2.history_id " + //
			"      and o2.history_current = true " + //
			" order by o.object_id " //
	;

	private static final String ENABLE_REFERENTIAL_INTEGRITY = //
		"          set REFERENTIAL_INTEGRITY true" //
	;

	private static final String DISABLE_REFERENTIAL_INTEGRITY = //
		"          set REFERENTIAL_INTEGRITY false" //
	;

	private static final String UPSERT_ALT_NAME = //
		"     merge into cmf_alt_name (object_id, new_name) key (object_id) values ( ?, ? ) " //
	;

	private static final String RESTART_SEQUENCE = //
		"     alter sequence %s restart with 1" //
	;

	private static final String LIST_SEQUENCES = //
		"    select sequence_name " + //
			"  from information_schema.columns " + //
			" where table_name like 'CMF_%' " + //
			"   and sequence_name is not null" //
	;

	private static final String SHUTDOWN_DB = //
		" shutdown compact "
	// null //
	;

	public JdbcDialectH2(DatabaseMetaData md) throws SQLException {
		super(EngineType.H2, md);
	}

	@Override
	protected boolean isSupportsArrays() {
		return false;
	}

	@Override
	protected boolean isTruncateBypassesConstraints() {
		return false;
	}

	@Override
	protected boolean isTruncateRestartsSequences() {
		return false;
	}

	@Override
	protected boolean isShutdownOnLastConnectionClose() {
		return true;
	}

	@Override
	protected boolean isForeignKeyMissingException(SQLException e) {
		return StringUtils.equalsIgnoreCase(e.getSQLState(), "23506");
	}

	@Override
	protected String doTranslate(Query sql) {
		switch (sql) {
			case LOAD_OBJECTS_BY_ID:
				return JdbcDialectH2.LOAD_OBJECTS_BY_ID;
			case LOAD_OBJECTS_BY_HISTORY_ID:
				return JdbcDialectH2.LOAD_OBJECTS_BY_HISTORY_ID;
			case LOAD_OBJECTS_BY_ID_CURRENT:
				return JdbcDialectH2.LOAD_OBJECTS_BY_ID_CURRENT;
			case LOAD_OBJECT_NAMES_BY_ID:
				return JdbcDialectH2.LOAD_OBJECT_NAMES_BY_ID;
			case LOAD_OBJECT_NAMES_BY_ID_CURRENT:
				return JdbcDialectH2.LOAD_OBJECT_NAMES_BY_ID_CURRENT;
			case ENABLE_REFERENTIAL_INTEGRITY:
				return JdbcDialectH2.ENABLE_REFERENTIAL_INTEGRITY;
			case DISABLE_REFERENTIAL_INTEGRITY:
				return JdbcDialectH2.DISABLE_REFERENTIAL_INTEGRITY;
			case UPSERT_ALT_NAME:
				return JdbcDialectH2.UPSERT_ALT_NAME;
			case RESTART_SEQUENCE:
				return JdbcDialectH2.RESTART_SEQUENCE;
			case LIST_SEQUENCES:
				return JdbcDialectH2.LIST_SEQUENCES;
			case SHUTDOWN_DB:
				return JdbcDialectH2.SHUTDOWN_DB;
			default:
				break;
		}
		return super.doTranslate(sql);
	}

	@Override
	protected ResultSetHandler<Long> getObjectNumberHandler() {
		return JdbcDialectH2.OBJECT_NUMBER_HANDLER;
	}
}