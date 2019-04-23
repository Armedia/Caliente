module com.armedia.caliente.engine.dfc {
	exports com.armedia.caliente.engine.dfc;
	exports com.armedia.caliente.engine.dfc.common;
	exports com.armedia.caliente.engine.dfc.exporter;
	exports com.armedia.caliente.engine.dfc.importer;

	provides com.armedia.caliente.engine.exporter.ExportEngineFactory with //
		com.armedia.caliente.engine.dfc.exporter.DctmExportEngineFactory;

	provides com.armedia.caliente.engine.importer.ImportEngineFactory with //
		com.armedia.caliente.engine.dfc.importer.DctmImportEngineFactory;

	provides com.armedia.caliente.store.CmfContentOrganizer with //
		com.armedia.caliente.engine.dfc.DocumentumOrganizer;

	requires java.activation;

	requires chemistry.opencmis.commons.api;
	requires org.apache.commons.collections4;
	requires org.apache.commons.io;
	requires org.apache.commons.lang3;
	requires org.apache.commons.text;

	requires transitive com.armedia.caliente.engine;
	requires com.armedia.caliente.store;
	requires com.armedia.caliente.tools;
	requires com.armedia.caliente.tools.datasource;
	requires com.armedia.caliente.tools.dfc;
	requires com.armedia.commons.utilities;

	requires commons.pool2;
	requires dfc;
	requires slf4j.api;
}