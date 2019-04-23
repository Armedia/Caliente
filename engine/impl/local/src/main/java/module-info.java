module com.armedia.caliente.engine.local {
	exports com.armedia.caliente.engine.local.common;
	exports com.armedia.caliente.engine.local.exporter;
	exports com.armedia.caliente.engine.local.importer;

	provides com.armedia.caliente.engine.exporter.ExportEngineFactory with //
		com.armedia.caliente.engine.local.exporter.LocalExportEngineFactory;
	provides com.armedia.caliente.engine.importer.ImportEngineFactory with //
		com.armedia.caliente.engine.local.importer.LocalImportEngineFactory;

	requires java.activation;
	requires chemistry.opencmis.commons.api;
	requires org.apache.commons.codec;
	requires org.apache.commons.collections4;
	requires org.apache.commons.io;
	requires org.apache.commons.lang3;

	requires transitive com.armedia.caliente.engine;
	requires transitive com.armedia.caliente.store;
	requires com.armedia.caliente.tools;
	requires com.armedia.caliente.tools.datasource;
	requires transitive com.armedia.commons.utilities;

	requires commons.pool2;
	requires slf4j.api;

}