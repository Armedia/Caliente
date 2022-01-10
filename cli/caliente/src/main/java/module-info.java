module com.armedia.caliente.cli.caliente {
	requires java.xml;

	requires org.apache.commons.codec;
	requires org.apache.commons.io;
	requires org.apache.commons.lang3;
	requires org.apache.commons.text;

	requires chemistry.opencmis.client.api;
	requires com.armedia.caliente.store;
	requires com.armedia.caliente.engine;
	requires com.armedia.caliente.engine.alfresco.bi;
	requires com.armedia.caliente.engine.cmis;
	requires com.armedia.caliente.engine.dctm;
	requires com.armedia.commons.utilities;

	requires transitive org.slf4j;
	requires static log4j;
	requires static ch.qos.logback.classic;
	requires static ch.qos.logback.core;
}