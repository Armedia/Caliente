module com.armedia.caliente.tools.alfrescobi {
	exports com.armedia.caliente.tools.alfresco.bi;
	exports com.armedia.caliente.tools.alfresco.bi.xml;

	requires java.xml;
	requires java.xml.bind;

	requires org.apache.commons.io;
	requires org.apache.commons.lang3;

	requires com.armedia.caliente.tools;
	requires com.armedia.commons.utilities;
}