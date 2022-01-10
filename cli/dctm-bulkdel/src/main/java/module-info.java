module com.armedia.caliente.cli.dctm.bulkdel {
	requires static chemistry.opencmis.client.api;
	requires com.armedia.caliente.engine;
	requires static com.armedia.caliente.engine.alfrescobi;
	requires static com.armedia.caliente.engine.cmis;
	requires static com.armedia.caliente.engine.dfc;
	requires static com.armedia.caliente.engine.local;
	requires static com.armedia.caliente.engine.sharepoint;
	requires static com.armedia.caliente.engine.ucm;
	requires static com.armedia.caliente.engine.xml;
	requires com.armedia.caliente.store;
	requires static com.armedia.caliente.store.jdbc;
	requires static com.armedia.caliente.store.local;
	requires com.armedia.caliente.tools;
	requires com.armedia.caliente.tools.datasource;
	requires static com.armedia.caliente.tools.dfc;
	requires com.armedia.commons.utilities;
	requires static dfc;
	requires java.activation;
	requires java.xml;
	requires log4j;
	requires mailapi;
	requires org.apache.commons.io;
	requires org.apache.commons.lang3;
	requires org.apache.commons.text;
	requires org.slf4j;
}