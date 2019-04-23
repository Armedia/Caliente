module com.armedia.caliente.engine.cmis {
	exports com.armedia.caliente.engine.cmis;
	exports com.armedia.caliente.engine.cmis.exporter;
	exports com.armedia.caliente.engine.cmis.importer;

	provides com.armedia.caliente.engine.importer.ImportEngineFactory with //
		com.armedia.caliente.engine.cmis.importer.CmisImportEngineFactory;

	provides com.armedia.caliente.engine.exporter.ExportEngineFactory with //
		com.armedia.caliente.engine.cmis.exporter.CmisExportEngineFactory;

	requires java.activation;

	requires chemistry.opencmis.client.api;
	requires chemistry.opencmis.client.impl;
	requires chemistry.opencmis.commons.api;
	requires chemistry.opencmis.commons.impl;
	requires org.apache.commons.collections4;
	requires org.apache.commons.io;
	requires org.apache.commons.lang3;
	requires org.apache.commons.text;

	requires transitive com.armedia.caliente.engine;
	requires transitive com.armedia.caliente.store;
	requires com.armedia.caliente.tools;
	requires com.armedia.caliente.tools.datasource;
	requires com.armedia.commons.utilities;

	requires commons.pool2;
	requires slf4j.api;
}