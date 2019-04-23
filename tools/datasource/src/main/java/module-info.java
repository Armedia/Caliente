module com.armedia.caliente.tools.datasource {
	exports com.armedia.caliente.tools.datasource;
	exports com.armedia.caliente.tools.datasource.jndi;
	exports com.armedia.caliente.tools.datasource.spring;
	exports com.armedia.caliente.tools.datasource.pooled;

	uses com.armedia.caliente.tools.datasource.DataSourceLocator;

	provides com.armedia.caliente.tools.datasource.DataSourceLocator with //
		com.armedia.caliente.tools.datasource.jndi.JndiDataSourceLocator,
		com.armedia.caliente.tools.datasource.pooled.PooledDataSourceLocator,
		com.armedia.caliente.tools.datasource.spring.SpringDataSourceLocator;

	requires static java.naming;
	requires java.sql;

	requires org.apache.commons.lang3;

	requires static transitive spring.beans;
	requires static transitive spring.context;
	requires static transitive spring.core;

	requires transitive com.armedia.commons.utilities;

	requires static commons.dbcp2;
	requires static commons.dbutils;
	requires static slf4j.api;
}