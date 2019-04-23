module com.armedia.caliente.tools {
	exports com.armedia.caliente.tools;
	exports com.armedia.caliente.tools.cfg;
	exports com.armedia.caliente.tools.xml;

	requires java.xml;
	requires transitive java.xml.bind;

	requires com.ctc.wstx;
	requires org.apache.commons.codec;
	requires org.apache.commons.io;
	requires org.apache.commons.lang3;
	requires org.codehaus.stax2;

	requires com.armedia.commons.utilities;

	requires slf4j.api;
	requires stax.utils;
}