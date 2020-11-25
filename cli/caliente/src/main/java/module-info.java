module com.armedia.caliente.cli {
	exports com.armedia.caliente.cli;
	exports com.armedia.caliente.cli.classpath;
	exports com.armedia.caliente.cli.exception;
	exports com.armedia.caliente.cli.filter;
	exports com.armedia.caliente.cli.launcher;
	exports com.armedia.caliente.cli.launcher.log;
	exports com.armedia.caliente.cli.token;
	exports com.armedia.caliente.cli.utils;

	requires java.xml;

	requires org.apache.commons.codec;
	requires org.apache.commons.io;
	requires org.apache.commons.lang3;
	requires org.apache.commons.text;

	requires com.armedia.commons.utilities;

	requires transitive slf4j.api;
	requires static log4j;
	requires static logback.classic;
	requires static logback.core;
}