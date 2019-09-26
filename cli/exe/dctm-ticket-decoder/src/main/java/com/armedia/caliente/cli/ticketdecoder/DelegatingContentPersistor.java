package com.armedia.caliente.cli.ticketdecoder;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Objects;

import com.armedia.caliente.cli.ticketdecoder.xml.Content;
import com.armedia.commons.utilities.Tools;

public class DelegatingContentPersistor extends ContentPersistor {

	private final Collection<ContentPersistor> delegates;
	private final Collection<ContentPersistor> active = new LinkedList<>();

	public DelegatingContentPersistor(Collection<ContentPersistor> delegates) {
		super(null);
		Collection<ContentPersistor> d = new ArrayList<>(delegates.size());
		delegates.stream().filter(Objects::nonNull).forEach(d::add);
		this.delegates = Tools.freezeCollection(d);
	}

	@Override
	protected void startup() throws Exception {
		for (ContentPersistor persistor : this.delegates) {
			try {
				persistor.initialize();
				this.active.add(persistor);
			} catch (Exception e) {
				this.error.error("Failed to initialize the persistor {}", persistor, e);
			}
		}
	}

	@Override
	protected void persistContent(Content content) {
		this.active.forEach((w) -> w.persist(content));
	}

	@Override
	protected void cleanup() {
		try {
			this.active.forEach(ContentPersistor::close);
		} finally {
			this.active.clear();
		}
	}

}