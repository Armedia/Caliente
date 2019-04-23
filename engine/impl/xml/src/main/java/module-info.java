module com.armedia.caliente.engine.xml {
	exports com.armedia.caliente.engine.xml.common;
	exports com.armedia.caliente.engine.xml.importer;

	provides com.armedia.caliente.engine.importer.ImportEngineFactory with //
		com.armedia.caliente.engine.xml.importer.XmlImportEngineFactory;

	requires java.activation;
	requires java.xml;
	requires java.xml.bind;
	// requires javax.xml.stream.1.0.1.v201004272200;

	requires chemistry.opencmis.commons.api;
	requires org.apache.commons.collections4;
	requires org.apache.commons.io;
	requires org.apache.commons.lang3;

	requires transitive com.armedia.caliente.engine;
	requires com.armedia.caliente.store;
	requires com.armedia.caliente.tools;
	requires com.armedia.caliente.tools.datasource;
	requires com.armedia.commons.utilities;

	requires commons.pool2;
	requires slf4j.api;
}