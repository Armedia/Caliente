package com.armedia.caliente.cli.usermapper;

import java.io.Serializable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class BasePrincipal implements Serializable {
	private static final long serialVersionUID = 1L;

	protected static final Logger LOG = LoggerFactory.getLogger(BasePrincipal.class);

	private final String name;
	private final String guid;

	/**
	 * @param name
	 * @param guid
	 */
	public BasePrincipal(String name, String guid) {
		this.name = name;
		this.guid = guid;
	}

	public String getName() {
		return this.name;
	}

	public String getGuid() {
		return this.guid;
	}

	@Override
	public String toString() {
		return String.format("%s [name=%s, guid=%s]", getClass().getSimpleName(), getName(), getGuid());
	}
}