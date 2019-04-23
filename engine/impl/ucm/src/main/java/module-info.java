module com.armedia.caliente.engine.ucm {
	exports com.armedia.caliente.engine.ucm;
	exports com.armedia.caliente.engine.ucm.exporter;
	exports com.armedia.caliente.engine.ucm.model;

	provides com.armedia.caliente.engine.exporter.ExportEngineFactory with //
		com.armedia.caliente.engine.ucm.exporter.UcmExportEngineFactory;

	requires java.activation;
	requires org.apache.commons.codec;
	requires org.apache.commons.collections4;
	requires org.apache.commons.io;
	requires org.apache.commons.lang3;

	requires transitive com.armedia.caliente.engine;
	requires com.armedia.caliente.store;
	requires transitive com.armedia.caliente.tools;
	requires com.armedia.caliente.tools.datasource;
	requires com.armedia.commons.utilities;

	requires commons.pool2;
	requires transitive oracle.ridc;
	requires slf4j.api;
}