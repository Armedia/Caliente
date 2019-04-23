module com.armedia.caliente.tools.dfc {
	exports com.armedia.caliente.tools.dfc;
	exports com.armedia.caliente.tools.dfc.pool;

	requires org.apache.commons.lang3;
	requires org.apache.commons.text;

	requires com.armedia.caliente.tools;
	requires transitive com.armedia.commons.utilities;

	requires transitive dfc;
	requires commons.pool2;
	requires slf4j.api;
}