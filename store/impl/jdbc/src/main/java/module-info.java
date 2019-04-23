module com.armedia.caliente.store.jdbc {
	exports com.armedia.caliente.store.jdbc;

	provides com.armedia.caliente.store.CmfObjectStoreFactory with //
		com.armedia.caliente.store.jdbc.JdbcObjectStoreFactory;

	requires transitive java.sql;

	requires org.apache.commons.lang3;

	requires transitive com.armedia.caliente.store;
	requires com.armedia.caliente.tools.datasource;
	requires com.armedia.commons.utilities;

	requires commons.dbutils;
	requires liquibase.core;
	requires slf4j.api;
}