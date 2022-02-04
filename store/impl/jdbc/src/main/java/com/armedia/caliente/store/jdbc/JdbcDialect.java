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
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.dbutils.ResultSetHandler;
import org.apache.commons.lang3.StringUtils;

import com.armedia.caliente.store.CmfStorageException;
import com.armedia.commons.utilities.Tools;

public abstract class JdbcDialect {

	public static enum EngineType {
		//
		H2 {
			@Override
			protected JdbcDialect newDialect(DatabaseMetaData md) throws SQLException {
				return new JdbcDialectH2(md);
			}
		}, //
		PostgreSQL {
			@Override
			protected JdbcDialect newDialect(DatabaseMetaData md) throws SQLException {
				return new JdbcDialectPostgreSQL(md);
			}
		}, //
			//
		;

		private final Set<String> matches;

		private EngineType(String... matches) {
			Set<String> m = new TreeSet<>();
			m.add(StringUtils.upperCase(name()));
			for (String s : matches) {
				s = StringUtils.strip(s);
				if (!StringUtils.isEmpty(s)) {
					m.add(StringUtils.upperCase(s));
				}
			}
			// Keep order, but gain speed
			this.matches = Tools.freezeSet(new LinkedHashSet<>(m));
		}

		private boolean matches(String dbName) {
			return this.matches.contains(StringUtils.upperCase(dbName));
		}

		protected abstract JdbcDialect newDialect(DatabaseMetaData md) throws SQLException;

		private static JdbcDialect constructDialect(DatabaseMetaData md) throws SQLException, CmfStorageException {
			Objects.requireNonNull(md, "Must provide a valid DatabaseMetaData instance");
			final String dbName = md.getDatabaseProductName();
			if (StringUtils.isEmpty(dbName)) {
				throw new IllegalArgumentException("Must provide a DB Name to check against");
			}
			for (EngineType t : EngineType.values()) {
				if (t.matches(dbName)) { return t.newDialect(md); }
			}
			throw new CmfStorageException(String.format("DB Type [%s] is unsupported", dbName));
		}
	}

	public static enum Query {

		//
		CHECK_IF_OBJECT_EXISTS( //
			"       select object_id " + //
				"     from cmf_object " + //
				"    where object_id = ? " + //
				"      and object_type = ?" //
		),

		INSERT_OBJECT( //
			"       insert into " + //
				"          cmf_object (" + //
				"              object_id, object_name, search_key, object_type, " + //
				"              object_subtype, object_label, tier_id, history_id, history_current " + //
				"          ) " + //
				"   values (?, ?, ?, ?, ?, ?, ?, ?, ?)" //
		),

		UPSERT_ALT_NAME( //
			null //
		),

		RESET_ALT_NAME( //
			"       delete from cmf_alt_name " //
		),

		INSERT_OBJECT_PARENTS( //
			"       insert into " + //
				"          cmf_object_tree (" + //
				"              object_id, parent_pos, parent_id " + //
				"          ) " + //
				"   values (?, ?, ?)" //
		),

		INSERT_OBJECT_SECONDARIES( //
			"       insert into " + //
				"          cmf_object_secondary_subtype (" + //
				"              object_id, pos, name " + //
				"          ) " + //
				"   values (?, ?, ?)" //
		),

		INSERT_ATTRIBUTE( //
			"       insert into " + //
				"          cmf_attribute (" + //
				"              object_id, name, id, data_type, " + //
				"              length, qualifiable, multivalued" + //
				"          ) " + //
				"   values (?, ?, ?, ?, ?, ?, ?)" //
		),

		INSERT_ATTRIBUTE_VALUE( //
			"       insert into " + //
				"          cmf_attribute_value (" + //
				"              object_id, name, value_number, null_value, data" + //
				"          ) " + //
				"   values (?, ?, ?, ?, ?)" //
		),

		INSERT_PROPERTY( //
			"       insert into " + //
				"          cmf_property (" + //
				"              object_id, name, data_type, multivalued" + //
				"          ) " + //
				"   values (?, ?, ?, ?)" //
		),

		INSERT_PROPERTY_VALUE( //
			"       insert into " + //
				"          cmf_property_value (" + //
				"              object_id, name, value_number, null_value, data" + //
				"          ) " + //
				"   values (?, ?, ?, ?, ?)" //
		),

		INSERT_CONTENT( //
			"       insert into " + //
				"          cmf_content (" + //
				"              object_id, content_number, rendition_id, rendition_page, modifier, extension, " + //
				"              stream_length, mime_type, file_name, locator" + //
				"           ) " + //
				"    values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)" //
		),

		DELETE_CONTENT( //
			"       delete from cmf_content " + //
				"    where object_id = ?" //
		),

		INSERT_CONTENT_PROPERTY( //
			"       insert into " + //
				"          cmf_content_property (" + //
				"              object_id, content_number, name, data" + //
				"          ) " + //
				"   values (?, ?, ?, ?)" //
		),

		INSERT_HISTORY_LOCK( //
			"       insert into " + //
				"          cmf_history_lock (" + //
				"              object_type, history_id, lock_id" + //
				"          ) " + //
				"   values (?, ?, ?) " //
		),

		UPDATE_HISTORY_LOCK_COUNTER( //
			"       update cmf_history_lock " + //
				"      set counter = counter + 1 " + //
				"    where object_type = ? " + //
				"      and history_id = ? " + //
				"      and lock_id = ? " //
		),

		INSERT_EXPORT_PLAN( //
			"       insert into " + //
				"          cmf_export_plan (" + //
				"              object_type, object_id" + //
				"          ) " + //
				"   values (?, ?) " //
		),

		UPDATE_EXPORT_RESULT( //
			"      update cmf_export_plan " + //
				"     set result = ?, " + //
				"         message = ? " + //
				"   where object_type = ? " + //
				"     and object_id = ? " + //
				"     and result is null " //
		),

		GET_EXPORT_RESULT( //
			"       select result " + //
				"     from cmf_export_plan " + //
				"    where object_id = ? " + //
				"      and object_type = ?" //
		),

		CLEAR_ALL_MAPPINGS( //
			"     truncate table cmf_mapper" //
		),

		RESTART_SEQUENCE( //
			null //
		),

		LIST_SEQUENCES( //
			null //
		),

		LOAD_ALL_MAPPINGS( //
			"       select distinct object_type, name " + //
				"     from cmf_mapper " + //
				" order by object_type, name" //
		),

		LOAD_TYPE_MAPPINGS( //
			"       select distinct name " + //
				"     from cmf_mapper " + //
				"    where object_type = ? " + //
				" order by name" //
		),

		LOAD_TYPE_NAME_MAPPINGS( //
			"       select source_value, target_value " + //
				"     from cmf_mapper " + //
				"    where object_type = ? " + //
				"      and name = ? " + //
				" order by source_value" //
		),

		FIND_EXACT_MAPPING( //
			"       select target_value " + //
				"     from cmf_mapper " + //
				"    where object_type = ? " + //
				"      and name = ? " + //
				"      and source_value = ? " + //
				"      and target_value = ?" //
		),

		FIND_TARGET_MAPPING( //
			"       select target_value " + //
				"     from cmf_mapper " + //
				"    where object_type = ? " + //
				"      and name = ? " + //
				"      and source_value = ?" //
		),

		FIND_SOURCE_MAPPING( //
			"       select source_value " + //
				"     from cmf_mapper " + //
				"    where object_type = ? " + //
				"      and name = ? " + //
				"      and target_value = ?" //
		),

		INSERT_MAPPING( //
			"       insert into " + //
				"          cmf_mapper (" + //
				"              object_type, name, source_value, target_value" + //
				"          ) " + //
				"   values (?, ?, ?, ?)" //
		),

		DELETE_TARGET_MAPPING( //
			"       delete from cmf_mapper " + //
				"    where object_type = ? " + //
				"      and name = ? " + //
				"      and source_value = ?" //
		),

		DELETE_SOURCE_MAPPING( //
			"       delete from cmf_mapper " + //
				"    where object_type = ? " + //
				"      and name = ? " + //
				"      and target_value = ?" //
		),

		DELETE_BOTH_MAPPINGS( //
			"       delete from cmf_mapper " + //
				"    where object_type = ? " + //
				"      and name = ? " + //
				"      and not (source_value = ? and target_value = ?) " + //
				"      and (source_value = ? or target_value = ?)" //
		),

		LOAD_OBJECT_TYPES( //
			"       select object_type, count(*) " + //
				"     from cmf_object " + //
				" group by object_type " + // ),
				"   having count(*) > 0 " + //
				" order by object_type " //
		),

		LOAD_CONTENTS( //
			"       select * " + //
				"     from cmf_content " + //
				"    where object_id = ? " + //
				" order by content_number" //
		),

		LOAD_OBJECT_HISTORY_CURRENT_BY_HISTORY_ID( //
			"       select o.*, n.new_name " + //
				"     from cmf_object o left outer join cmf_alt_name n on (o.object_id = n.object_id)" + //
				"    where o.object_type = ? " + //
				"      and o.history_id = ? " + //
				"      and o.history_current = true " + //
				" order by o.object_number desc " //
		),

		LOAD_OBJECT_HISTORY_LATEST_BY_HISTORY_ID( //
			"       select o.*, n.new_name " + //
				"     from cmf_object o left outer join cmf_alt_name n on (o.object_id = n.object_id)" + //
				"    where o.object_number = ( " + //
				"              select max(object_number) " + //
				"                from cmf_object " + //
				"               where object_type = ? " + //
				"                 and history_id = ? " + //
				"          ) " //
		),

		LOAD_OBJECTS( //
			"       select o.*, n.new_name " + //
				"     from cmf_object o left outer join cmf_alt_name n on (o.object_id = n.object_id)" + //
				"    where o.object_type = ? " + //
				" order by o.tier_id, o.history_id, o.object_number" //
		),

		LOAD_FILTERED_OBJECTS( //
			"       select o.*, n.new_name " + //
				"     from cmf_object o left outer join cmf_alt_name n on (o.object_id = n.object_id), " + //
				"          cmf_object_filter f " + //
				"    where o.object_id = f.object_id " + //
				"      and o.object_type = ? " + //
				" order by o.tier_id, o.history_id, o.object_number" //
		),

		LOAD_OBJECTS_BY_ID_CURRENT( //
			null),

		LOAD_OBJECTS_BY_ID( //
			null),

		LOAD_OBJECTS_BY_HISTORY_ID( //
			null),

		LOAD_OBJECT_NAMES_BY_ID( //
			null),

		LOAD_OBJECT_NAMES_BY_ID_CURRENT( //
			null),

		LOAD_PARENT_IDS( //
			"       select parent_id " + //
				"     from cmf_object_tree " + //
				"    where object_id = ? " + //
				" order by parent_pos " //
		),

		LOAD_SECONDARIES( //
			"       select name " + //
				"     from cmf_object_secondary_subtype " + //
				"    where object_id = ? " + //
				" order by pos " //
		),

		LOAD_ATTRIBUTES( //
			"       select * " + //
				"     from cmf_attribute " + //
				"    where object_id = ? " + //
				" order by name" //
		),

		LOAD_ATTRIBUTE_VALUES( //
			"       select * " + //
				"     from cmf_attribute_value " + //
				"    where object_id = ? " + //
				"      and name = ? " + //
				" order by value_number" //
		),

		LOAD_PROPERTIES( //
			"       select * " + //
				"     from cmf_property " + //
				"    where object_id = ? " + //
				" order by name" //
		),

		LOAD_PROPERTY_VALUES( //
			"       select * " + //
				"     from cmf_property_value " + //
				"    where object_id = ? " + //
				"      and name = ? " + //
				" order by value_number" //
		),

		LOAD_CONTENT_PROPERTIES( //
			"       select * " + //
				"     from cmf_content_property " + //
				"    where object_id = ? " + //
				"      and content_number = ?" + //
				" order by name" //
		),

		DISABLE_REFERENTIAL_INTEGRITY(//
			null //
		),

		ENABLE_REFERENTIAL_INTEGRITY(//
			null //
		),

		DELETE_ALL_STREAMS( //
			"       delete from cmf_content_stream" //
		),

		TRUNCATE_STREAMS( //
			"       truncate table cmf_content_stream" //
		),

		CHECK_IF_CONTENT_EXISTS( //
			"       select object_id " + //
				"     from cmf_content_stream " + //
				"    where object_id = ? " + //
				"      and content_number = ? " //
		),

		GET_STREAM_LENGTH( //
			"       select length " + //
				"     from cmf_content_stream " + //
				"    where object_id = ? " + //
				"      and content_number = ? " //
		),

		GET_STREAM( //
			"       select length, data " + //
				"     from cmf_content_stream " + //
				"    where object_id = ? " + //
				"      and content_number = ? " //
		),

		DELETE_STREAM( //
			"       delete from cmf_content_stream " + //
				"    where object_id = ? " + //
				"      and content_number = ? " //
		),

		INSERT_STREAM( //
			"       insert into " + //
				"          cmf_content_stream (" + //
				"              object_id, content_number, length, data" + //
				"          ) " + //
				"   values (?, ?, ?, ?)" //
		),

		TRUNCATE_TABLE_FMT( //
			"     truncate table %s " //
		),

		LOAD_RENAME_MAPPINGS( //
			"       select * " + //
				"     from cmf_alt_name " //
		),

		SCAN_OBJECT_TREE( //
			"       select t.*, coalesce(n.new_name, o.object_name) as name " + //
				"     from cmf_object o left outer join cmf_alt_name n on (o.object_id = n.object_id), " + //
				"          cmf_object_tree t " + //
				"    where o.object_id = t.object_id " + //
				"      and o.history_current = true " + //
				" order by t.object_id, t.parent_pos " //
		),
		//

		LOAD_CONTAINERS( //
			"       select parent_id " + //
				"     from cmf_object_tree " + //
				"    where object_id = ? " + //
				" order by parent_pos " //
		),
		//

		LOAD_CONTAINED_OBJECTS( //
			"       select object_id " + //
				"     from cmf_object_tree " + //
				"    where parent_id = ? " + //
				" order by object_id " //
		),
		//

		INSERT_REQUIREMENT( //
			"       insert into " + //
				"          cmf_requirement (" + //
				"              object_id, requirement_id " + //
				"          ) " + //
				"   values (?, ?)" //
		),
		//

		INSERT_IMPORT_PLAN( //
			"       insert into " + //
				"          cmf_import_plan (" + //
				"              object_id, status, info " + //
				"          ) " + //
				"   values (?, ?, ?)" //
		),
		//

		LOAD_IMPORT_PLAN( //
			"       select r.requirement_id, p.status, p.info " + //
				"     from cmf_requirement r left outer join cmf_import_plan p on (r.requirement_id = p.object_id)" + //
				"    where r.object_id = ? " + //
				" order by r.requirement_id " //
		),
		//

		CLEAR_IMPORT_PLAN( //
			"       delete from cmf_import_plan " //
		),
		//

		INSERT_LOADER_FILTER( //
			"       insert into cmf_object_filter (object_id) values ( ? ) " //
		),
		//

		LOAD_FILTER( //
			"       select object_id from cmf_object_filter order by object_id " //
		),
		//

		CLEAR_LOADER_FILTER( //
			"       delete from cmf_object_filter " //
		),
		//

		SHUTDOWN_DB( //
			null //
		),
		//

		;

		private final String sql;

		private Query(String sql) {
			this.sql = sql;
		}
	}

	public final String dbName;
	public final int dbMajor;
	public final int dbMinor;
	public final String dbVersion;
	public final EngineType engineType;

	protected JdbcDialect(EngineType engineType, DatabaseMetaData md) throws SQLException {
		this.engineType = engineType;
		this.dbName = md.getDatabaseProductName();
		this.dbVersion = md.getDatabaseProductVersion();
		this.dbMajor = md.getDatabaseMajorVersion();
		this.dbMinor = md.getDatabaseMinorVersion();
	}

	protected abstract boolean isSupportsArrays();

	protected abstract boolean isTruncateBypassesConstraints();

	protected abstract boolean isTruncateRestartsSequences();

	protected abstract ResultSetHandler<Long> getObjectNumberHandler();

	protected boolean isShutdownOnLastConnectionClose() {
		return false;
	}

	protected boolean isDuplicateKeyException(SQLException e) {
		return StringUtils.equalsIgnoreCase(e.getSQLState(), "23505");
	}

	protected boolean isForeignKeyMissingException(SQLException e) {
		return StringUtils.equalsIgnoreCase(e.getSQLState(), "23503");
	}

	final String translateQuery(Query query) {
		if (query == null) { throw new IllegalArgumentException("Must provide a SQL query to resolve"); }
		return Tools.coalesce(doTranslate(query), query.sql);
	}

	protected String doTranslate(Query sql) {
		return sql.sql;
	}

	@Override
	public final String toString() {
		return String.format("%s v%d.%d (%s)", getClass().getSimpleName(), this.dbMajor, this.dbMinor, this.dbVersion);
	}

	public static JdbcDialect getDialect(DatabaseMetaData md) throws CmfStorageException, SQLException {
		return EngineType.constructDialect(md);
	}
}