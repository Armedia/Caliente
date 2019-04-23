package com.armedia.caliente.cli.ticketdecoder;

import java.util.Objects;

public enum PersistenceFormat {
	//
	CSV(CsvContentPersistor.class), //
	XML(XmlContentPersistor.class), //
	//
	;

	private final Class<? extends ContentPersistor> persistor;

	private PersistenceFormat(Class<? extends ContentPersistor> persistor) {
		this.persistor = Objects.requireNonNull(persistor, "Must provide a non-null persistor class");
	}

	public ContentPersistor newPersistor() throws Exception {
		return this.persistor.getConstructor().newInstance();
	}
}