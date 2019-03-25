package com.armedia.caliente.cli.ticketdecoder;

import java.io.File;

import com.armedia.caliente.cli.ticketdecoder.xml.Content;

public interface ContentPersistor extends AutoCloseable {

	public default void initialize(File target) throws Exception {
		// do nothing by default
	}

	public void persist(Content content) throws Exception;

	@Override
	public default void close() throws Exception {
		// Do nothing by default
	}

}