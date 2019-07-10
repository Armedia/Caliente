/*******************************************************************************
 * #%L
 * Armedia Caliente
 * %%
 * Copyright (c) 2010 - 2019 Armedia LLC
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

public class JdbcDialectPostgreSQL extends JdbcDialect {

	private static final String OBJECT_COLUMN_NAME = "object_number";
	private static final ResultSetHandler<Long> OBJECT_NUMBER_HANDLER = (
		rs) -> (rs.next() ? rs.getLong(JdbcDialectPostgreSQL.OBJECT_COLUMN_NAME) : null);

	private static final String LOAD_OBJECTS_BY_ID = //
		"       select o.*, n.new_name " + //
			"     from cmf_object o left outer join cmf_alt_name n on (o.object_id = n.object_id)" + //
			"    where o.object_type = ? " + //
			"      and o.object_id = any ( ? ) " + //
			" order by o.tier_id, o.history_id, o.object_number" //
	;

	private static final String LOAD_OBJECTS_BY_ID_CURRENT = //
		"       select o.*, n.new_name " + //
			"     from cmf_object o left outer join cmf_alt_name n on (o.object_id = n.object_id)" + //
			"    where o.object_type = ? " + //
			"      and o.object_id = any ( ? ) " + //
			"      and o.history_current = true " + //
			" order by o.tier_id, o.history_id, o.object_number" //
	;

	private static final String LOAD_OBJECT_NAMES_BY_ID = //
		"       select o.object_id, o.object_name, n.new_name " + //
			"     from cmf_object o left outer join cmf_alt_name n on (o.object_id = n.object_id)" + //
			"    where o.object_id = any ( ? ) " + //
			" order by o.object_id " //
	;

	private static final String LOAD_OBJECT_NAMES_BY_ID_CURRENT = //
		"       select o.object_id, o2.object_name, n.new_name " + //
			"     from cmf_object o, " + //
			"          cmf_object o2 left outer join cmf_alt_name n on (o2.object_id = n.object_id) " + //
			"    where o.object_id = any ( ? ) " + //
			"      and o.object_type = o2.object_type " + //
			"      and o.history_id = o2.history_id " + //
			"      and o2.history_current = true " + //
			" order by o.object_id " //
	;

	private static final String TRUNCATE_TABLE_FMT = //
		"     truncate table %s restart identity cascade " //
	;

	private static final String UPSERT_ALT_NAME = //
		"     insert " + //
			"   into cmf_alt_name (object_id, new_name) values ( ?, ? ) " + //
			"     on conflict (object_id) do " + //
			"                 update set new_name = excluded.new_name " //
	;

	private static final String RESTART_SEQUENCE = //
		"     alter sequence %s restart" //
	;

	private static final String LIST_SEQUENCES = //
		"    select sequence_name " + //
			"  from information_schema.sequences" //
	;

	public JdbcDialectPostgreSQL(DatabaseMetaData md) throws SQLException {
		super(EngineType.PostgreSQL, md);
	}

	@Override
	protected boolean isSupportsArrays() {
		return true;
	}

	@Override
	protected boolean isTruncateBypassesConstraints() {
		return true;
	}

	@Override
	protected boolean isTruncateRestartsSequences() {
		return true;
	}

	@Override
	protected String doTranslate(Query sql) {
		switch (sql) {
			case LOAD_OBJECTS_BY_ID:
				return JdbcDialectPostgreSQL.LOAD_OBJECTS_BY_ID;
			case LOAD_OBJECTS_BY_ID_CURRENT:
				return JdbcDialectPostgreSQL.LOAD_OBJECTS_BY_ID_CURRENT;
			case LOAD_OBJECT_NAMES_BY_ID:
				return JdbcDialectPostgreSQL.LOAD_OBJECT_NAMES_BY_ID;
			case LOAD_OBJECT_NAMES_BY_ID_CURRENT:
				return JdbcDialectPostgreSQL.LOAD_OBJECT_NAMES_BY_ID_CURRENT;
			case TRUNCATE_TABLE_FMT:
				return JdbcDialectPostgreSQL.TRUNCATE_TABLE_FMT;
			case UPSERT_ALT_NAME:
				return JdbcDialectPostgreSQL.UPSERT_ALT_NAME;
			case RESTART_SEQUENCE:
				return JdbcDialectPostgreSQL.RESTART_SEQUENCE;
			case LIST_SEQUENCES:
				return JdbcDialectPostgreSQL.LIST_SEQUENCES;
			default:
				break;
		}
		return super.doTranslate(sql);
	}

	@Override
	protected ResultSetHandler<Long> getObjectNumberHandler() {
		return JdbcDialectPostgreSQL.OBJECT_NUMBER_HANDLER;
	}
}