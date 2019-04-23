module com.armedia.caliente.engine.sharepoint {
	exports com.armedia.caliente.engine.sharepoint;
	exports com.armedia.caliente.engine.sharepoint.exporter;

	provides com.armedia.caliente.engine.exporter.ExportEngineFactory with //
		com.armedia.caliente.engine.sharepoint.exporter.ShptExportEngineFactory;

	requires java.activation;
	requires org.apache.commons.collections4;
	requires org.apache.commons.io;
	requires org.apache.commons.lang3;
	requires org.apache.commons.text;

	requires transitive com.armedia.caliente.engine;
	requires com.armedia.caliente.store;
	requires transitive com.armedia.caliente.tools;
	requires com.armedia.caliente.tools.datasource;
	requires com.armedia.commons.utilities;

	requires commons.pool2;
	requires httpclient;
	requires httpcore;
	requires jshare;
	requires slf4j.api;
}