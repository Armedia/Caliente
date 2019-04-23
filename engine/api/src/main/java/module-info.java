module com.armedia.caliente.engine {
	exports com.armedia.caliente.engine;
	exports com.armedia.caliente.engine.converter;
	exports com.armedia.caliente.engine.dynamic;
	exports com.armedia.caliente.engine.dynamic.metadata;
	exports com.armedia.caliente.engine.dynamic.transformer;
	exports com.armedia.caliente.engine.dynamic.transformer.mapper;
	exports com.armedia.caliente.engine.dynamic.transformer.mapper.schema;
	exports com.armedia.caliente.engine.dynamic.xml.mapper;
	exports com.armedia.caliente.engine.exporter;
	exports com.armedia.caliente.engine.importer;
	exports com.armedia.caliente.engine.tools;

	uses com.armedia.caliente.engine.exporter.ExportEngineFactory;
	uses com.armedia.caliente.engine.exporter.ExportEngineListener;
	uses com.armedia.caliente.engine.importer.ImportEngineFactory;
	uses com.armedia.caliente.engine.importer.ImportEngineListener;

	provides com.armedia.caliente.store.CmfContentOrganizer with //
		com.armedia.caliente.engine.tools.LocalOrganizer, //
		com.armedia.caliente.engine.tools.HierarchicalOrganizer;

	requires transitive java.scripting;
	requires java.sql;
	requires java.xml;
	requires java.xml.bind;

	requires org.apache.commons.codec;
	requires org.apache.commons.collections4;
	requires org.apache.commons.io;
	requires org.apache.commons.lang3;
	requires org.apache.commons.text;

	requires transitive com.armedia.caliente.store;
	requires transitive com.armedia.caliente.tools;
	requires com.armedia.caliente.tools.datasource;
	requires com.armedia.commons.utilities;

	requires slf4j.api;
	requires static chemistry.opencmis.commons.api;
	requires commons.dbutils;
	requires commons.pool2;
}