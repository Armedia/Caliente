package com.armedia.cmf.storage.jdbc;

import java.sql.DatabaseMetaData;
import java.sql.SQLException;

import org.apache.commons.dbutils.ResultSetHandler;
import org.apache.commons.lang3.StringUtils;

import com.armedia.cmf.storage.CmfStorageException;
import com.armedia.commons.utilities.Tools;

public abstract class JdbcDialect {

	public static enum EngineType {
		//
		H2, PostgreSQL,
		//
		;

		private boolean matches(String dbName) {
			if (dbName == null) { throw new IllegalArgumentException("Must provide a string to check against"); }
			return StringUtils.equalsIgnoreCase(name(), dbName);
		}

		private static EngineType parse(String dbName) throws CmfStorageException {
			if (dbName == null) { throw new IllegalArgumentException("Must provide a DB Name to check against"); }
			for (EngineType t : EngineType.values()) {
				if (t.matches(dbName)) { return t; }
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
				"              object_subtype, object_label, batch_id, batch_head, " + //
				"              product_name, product_version" + //
				"          ) " + //
				"   values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)" //
		),

		INSERT_ALT_NAME( //
			"       insert into " + //
				"          cmf_alt_name (" + //
				"              object_id, new_name " + //
				"          ) " + //
				"   values (?, ?)" //
		),

		UPDATE_ALT_NAME( //
			"       update cmf_alt_name " + //
				"      set new_name = ? " + //
				"    where object_id = ? " + //
				"      and new_name = ? " //
		),

		RESET_ALT_NAME( //
			"       update cmf_alt_name n " + //
				"      set n.new_name = ( " + //
				"              select o.object_name " + //
				"                from cmf_object o " + //
				"               where o.object_id = n.object_id " + //
				"          ) " //
		),

		INSERT_OBJECT_PARENTS( //
			"       insert into " + //
				"          cmf_object_tree (" + //
				"              object_id, parent_pos, parent_id " + //
				"          ) " + //
				"   values (?, ?, ?)" //
		),

		INSERT_ATTRIBUTE( //
			"       insert into " + //
				"          cmf_attribute (" + //
				"              object_id, name, id, data_type, " + //
				"              length, qualifiable, repeating" + //
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
				"              object_id, name, data_type, repeating" + //
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
				"              object_id, rendition_id, rendition_page, modifier, extension, content_number, " + //
				"              stream_length, mime_type, file_name" + //
				"           ) " + //
				"    values (?, ?, ?, ?, ?, ?, ?, ?, ?)" //
		),

		DELETE_CONTENT( //
			"       delete from cmf_content " + //
				"    where object_id = ?" //
		),

		INSERT_CONTENT_PROPERTY( //
			"       insert into " + //
				"          cmf_content_property (" + //
				"              object_id, rendition_id, rendition_page, modifier, name, value" + //
				"          ) " + //
				"   values (?, ?, ?, ?, ?, ?)" //
		),

		QUERY_EXPORT_PLAN_DUPE( //
			"       select * " + //
				"     from cmf_export_plan " + //
				"    where object_id = ?" //
		),

		INSERT_EXPORT_PLAN( //
			"       insert into " + //
				"          cmf_export_plan (" + //
				"              object_type, object_id" + //
				"          ) " + //
				"   values (?, ?) " //
		),

		INSERT_CACHE_TARGET( //
			"       insert into " + //
				"          cmf_target_cache (" + //
				"              object_type, object_id, search_key" + //
				"          ) " + //
				"   values (?, ?, ?) " //
		),

		CLEAR_ALL_MAPPINGS( //
			"     truncate table cmf_mapper" //
		),

		CLEAR_TARGET_CACHE( //
			"     truncate table cmf_target_cache" //
		),

		LOAD_ALL_MAPPINGS( //
			"       select distinct object_type, name " + //
				"     from cmf_mapper " + //
				" order by object_type, name" //
		),

		LOAD_ALL_CACHE_TARGETS( //
			"       select object_number, object_type, object_id, search_key " + //
				"     from cmf_target_cache " + //
				" order by object_number " //
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

		LOAD_OBJECTS( //
			"       select o.*, n.new_name " + //
				"     from cmf_object o, cmf_alt_name n" + //
				"    where o.object_id = n.object_id " + //
				"      and o.object_type = ? " + //
				" order by o.object_number" //
		),

		LOAD_OBJECTS_HEAD( //
			"       select o.*, n.new_name " + //
				"     from cmf_object o, cmf_alt_name n" + //
				"    where o.object_id = n.object_id " + //
				"      and o.object_type = ? " + //
				"      and o.batch_head = true " + //
				" order by o.object_number" //
		),

		LOAD_OBJECTS_BATCHED( //
			"       select o.*, n.new_name " + //
				"     from cmf_object o, cmf_alt_name n" + //
				"    where o.object_id = n.object_id " + //
				"      and o.object_type = ? " + //
				" order by o.batch_id, o.object_number" //
		),

		LOAD_OBJECTS_BY_ID( //
			null),

		LOAD_OBJECTS_BY_ID_HEAD( //
			null),

		LOAD_OBJECTS_BY_ID_BATCHED( //
			null),

		LOAD_PARENT_IDS( //
			"       select parent_id " + //
				"     from cmf_object_tree " + //
				"    where object_id = ? " + //
				" order by parent_pos " //
		),

		LOAD_NAME_COLLISIONS( //
			"       select * " + //
				"     from cmf_name_collision " //
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
				"      and rendition_id = ?" + //
				"      and rendition_page = ?" + //
				"      and modifier = ?" + //
				" order by name" //
		),

		CHECK_FOR_NAME_COLLISIONS( //
			"       select o.object_number, o.object_id " + //
				"     from cmf_object o, cmf_object_tree t, cmf_alt_name n " + //
				"    where o.object_id = t.object_id " + //
				"      and o.object_id = n.object_id " + //
				"      and t.parent_id = ? " + //
				"      and n.new_name = ? " + //
				" order by o.object_number, o.object_id " //
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

		CHECK_IF_CONTENT_EXISTS( //
			"       select object_id " + //
				"     from cmf_content_stream " + //
				"    where object_id = ? " + //
				"      and rendition_id = ?" + //
				"      and rendition_page = ?" + //
				"      and modifier = ?" //
		),

		GET_STREAM_LENGTH( //
			"       select length " + //
				"     from cmf_content_stream " + //
				"    where object_id = ? " + //
				"      and rendition_id = ?" + //
				"      and rendition_page = ?" + //
				"      and modifier = ?" //
		),

		GET_STREAM( //
			"       select length, data " + //
				"     from cmf_content_stream " + //
				"    where object_id = ? " + //
				"      and rendition_id = ?" + //
				"      and rendition_page = ?" + //
				"      and modifier = ?" //
		),

		DELETE_STREAM( //
			"       delete from cmf_content_stream " + //
				"    where object_id = ? " + //
				"      and rendition_id = ?" + //
				"      and rendition_page = ?" + //
				"      and modifier = ?" //
		),

		INSERT_STREAM( //
			"       insert into " + //
				"          cmf_content_stream (" + //
				"              object_id, rendition_id, rendition_page, modifier, length, data" + //
				"          ) " + //
				"   values (?, ?, ?, ?, ?, ?)" //
		),

		TRUNCATE_TABLE_FMT( //
			"     truncate table %s " //
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

	protected abstract ResultSetHandler<Long> getObjectNumberHandler();

	protected abstract boolean isDuplicateKeyException(SQLException e);

	final String translateQuery(Query query, boolean required) {
		if (query == null) { throw new IllegalArgumentException("Must provide a SQL query to resolve"); }
		String sql = Tools.coalesce(doTranslate(query), query.sql);
		if (required && (sql == null)) { throw new IllegalStateException(
			String.format("Required query [%s] is missing", query)); }
		return sql;
	}

	protected String doTranslate(Query sql) {
		return sql.sql;
	}

	@Override
	public final String toString() {
		return String.format("%s v%d.%d (%s)", getClass().getSimpleName(), this.dbMajor, this.dbMinor, this.dbVersion);
	}

	public static JdbcDialect getDialect(DatabaseMetaData md) throws CmfStorageException, SQLException {
		final String dbName = md.getDatabaseProductName();
		EngineType type = EngineType.parse(dbName);

		switch (type) {
			case H2:
				return new JdbcDialectH2(md);
			case PostgreSQL:
				return new JdbcDialectPostgreSQL(md);
			default:
				throw new CmfStorageException(String.format("Unsupported DB type [%s]", dbName));
		}
	}
}