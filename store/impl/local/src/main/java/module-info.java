module com.armedia.caliente.store.local {
	exports com.armedia.caliente.store.local.xml.legacy;
	exports com.armedia.caliente.store.local.xml;
	exports com.armedia.caliente.store.local;

	provides com.armedia.caliente.store.CmfContentStoreFactory with //
		com.armedia.caliente.store.local.LocalContentStoreFactory;

	requires java.xml;
	requires java.xml.bind;

	requires org.apache.commons.io;
	requires org.apache.commons.lang3;

	requires transitive com.armedia.caliente.store;
	requires transitive com.armedia.commons.utilities;

	requires slf4j.api;
}