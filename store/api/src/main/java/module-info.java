module com.armedia.caliente.store {
	exports com.armedia.caliente.store;
	exports com.armedia.caliente.store.tools;
	exports com.armedia.caliente.store.xml;

	uses com.armedia.caliente.store.CmfObjectStoreFactory;
	uses com.armedia.caliente.store.CmfContentStoreFactory;

	requires java.activation;
	requires java.xml;
	requires java.xml.bind;

	requires org.apache.commons.codec;
	requires org.apache.commons.io;
	requires org.apache.commons.lang3;
	requires org.apache.commons.text;

	requires transitive com.armedia.commons.utilities;

	requires slf4j.api;
}