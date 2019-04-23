module com.armedia.caliente.engine.alfrescobi {
	exports com.armedia.caliente.engine.alfresco.bi;
	exports com.armedia.caliente.engine.alfresco.bi.importer;
	exports com.armedia.caliente.engine.alfresco.bi.importer.model;

	provides com.armedia.caliente.engine.importer.ImportEngineFactory with //
		com.armedia.caliente.engine.alfresco.bi.importer.AlfImportEngineFactory;

	requires java.activation;
	requires java.xml;
	requires java.xml.bind;
	// requires javax.xml.stream.1.0.1.v201004272200;

	requires org.apache.commons.codec;
	requires org.apache.commons.io;
	requires org.apache.commons.lang3;
	requires org.apache.commons.text;

	requires transitive com.armedia.caliente.engine;
	requires com.armedia.caliente.store;
	requires com.armedia.caliente.tools;
	requires transitive com.armedia.caliente.tools.alfrescobi;
	requires com.armedia.caliente.tools.datasource;
	requires com.armedia.commons.utilities;

	requires commons.pool2;
	requires slf4j.api;
}