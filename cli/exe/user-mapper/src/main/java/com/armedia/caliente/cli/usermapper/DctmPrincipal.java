package com.armedia.caliente.cli.usermapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class DctmPrincipal extends BasePrincipal {
	private static final long serialVersionUID = 1L;

	protected static final Logger LOG = LoggerFactory.getLogger(DctmPrincipal.class);

	private final String source;

	/**
	 * @param name
	 * @param source
	 * @param guid
	 */
	public DctmPrincipal(String name, String source, String guid) {
		super(name, guid);
		this.source = source;
	}

	public String getSource() {
		return this.source;
	}

	@Override
	public String toString() {
		return String.format("%s [name=%s, source=%s, guid=%s]", getClass().getSimpleName(), getName(), this.source,
			getGuid());
	}
}