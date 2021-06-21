/*******************************************************************************
 * #%L
 * Armedia Caliente
 * %%
 * Copyright (C) 2013 - 2019 Armedia, LLC
 * %%
 * This file is part of the Caliente software.
 *
 * If the software was purchased under a paid Caliente license, the terms of
 * the paid license agreement will prevail.  Otherwise, the software is
 * provided under the following open source license terms:
 *
 * Caliente is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Caliente is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Caliente. If not, see <http://www.gnu.org/licenses/>.
 * #L%
 *******************************************************************************/
package com.armedia.caliente.engine.dynamic.xml.metadata;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.armedia.caliente.store.CmfAttribute;
import com.armedia.caliente.store.CmfObject;
import com.armedia.commons.utilities.Tools;
import com.armedia.commons.utilities.concurrent.BaseShareableLockable;
import com.armedia.commons.utilities.concurrent.SharedAutoLock;
import com.armedia.commons.utilities.function.CheckedSupplier;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "externalMetadataSet.t", propOrder = {
	"loaders"
})
public class MetadataSet extends BaseShareableLockable implements AutoCloseable {

	@XmlTransient
	protected final Logger log = LoggerFactory.getLogger(getClass());

	@XmlElements({
		@XmlElement(name = "from-sql", type = MetadataFromSQL.class),
		@XmlElement(name = "from-ddl", type = MetadataFromDDL.class)
	})
	protected List<AttributeValuesLoader> loaders;

	@XmlAttribute(name = "id", required = true)
	protected String id;

	@XmlAttribute(name = "dataSource", required = true)
	protected String dataSource;

	@XmlAttribute(name = "failOnError", required = false)
	protected boolean failOnError = false;

	@XmlAttribute(name = "failOnMissing", required = false)
	protected boolean failOnMissing = false;

	@XmlTransient
	private List<AttributeValuesLoader> initializedLoaders;

	@XmlTransient
	private CheckedSupplier<Connection, SQLException> connectionSource;

	public List<AttributeValuesLoader> getLoaders() {
		if (this.loaders == null) {
			this.loaders = new ArrayList<>();
		}
		return this.loaders;
	}

	public String getId() {
		return this.id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public final String getDataSource() {
		return this.dataSource;
	}

	public final void setDataSource(String dataSource) {
		this.dataSource = dataSource;
	}

	public boolean isFailOnError() {
		return this.failOnError;
	}

	public void setFailOnError(boolean value) {
		this.failOnError = value;
	}

	public boolean isFailOnMissing() {
		return this.failOnMissing;
	}

	public void setFailOnMissing(boolean value) {
		this.failOnMissing = value;
	}

	public void initialize(CheckedSupplier<Connection, SQLException> connectionSource) throws Exception {
		Objects.requireNonNull(connectionSource, "Must provide a DataSource lookup function");
		shareLockedUpgradable(() -> this.initializedLoaders, Objects::isNull, (e) -> {
			if (this.initializedLoaders != null) { return; }
			List<AttributeValuesLoader> initializedLoaders = new ArrayList<>();
			boolean ok = false;
			try {
				try (final Connection c = connectionSource.get()) {
					for (AttributeValuesLoader loader : getLoaders()) {
						if (loader == null) {
							continue;
						}
						loader.initialize(c);
						initializedLoaders.add(loader);
					}
				}
				ok = true;
			} finally {
				if (!ok) {
					// Close out all the initialized loaders, just before rolling back
					for (AttributeValuesLoader loader : initializedLoaders) {
						try {
							loader.close();
						} catch (Throwable t) {
							this.log.warn("Failed to close an initialized {} loader in MetadataSet {}",
								loader.getClass().getSimpleName(), getId(), t);
						}
					}
				}
			}
			this.initializedLoaders = Tools.freezeList(initializedLoaders);
			this.connectionSource = connectionSource;
		});
	}

	public <V> Map<String, CmfAttribute<V>> getAttributeValues(CmfObject<V> object) throws Exception {
		try (SharedAutoLock lock = sharedAutoLock()) {
			// If there are no loades initialized, this is a problem...
			if (this.initializedLoaders == null) { throw new Exception("This metadata source is not yet initialized"); }

			// If there are no loaders initialized, we always return empty
			if (this.initializedLoaders.isEmpty()) { return null; }

			Map<String, CmfAttribute<V>> finalAttributes = new HashMap<>();
			try (Connection c = this.connectionSource.get()) {
				for (AttributeValuesLoader l : this.initializedLoaders) {
					if (l == null) {
						continue;
					}

					Map<String, CmfAttribute<V>> newAttributes = null;
					try {
						newAttributes = l.getAttributeValues(c, object);
					} catch (Exception e) {
						if (isFailOnError()) {
							// An exceptikon was caught, but we need to fail on it
							throw new Exception(
								String.format("Exception raised while loading external metadata attributes for %s",
									object.getDescription()),
								e);
						} else {
							this.log.warn("Failed to load the external metadata for set [{}] for {}", this.id,
								object.getDescription(), e);
						}
					}

					if ((newAttributes == null) && isFailOnMissing()) {
						// The attribute values are required, but none were found...this is an
						// error!
						throw new Exception(String.format(
							"Did not find the required external metadata attributes for %s", object.getDescription()));
					}

					if (newAttributes != null) {
						finalAttributes.putAll(newAttributes);
					}
				}
			}
			if (finalAttributes.isEmpty()) {
				finalAttributes = null;
			}
			return finalAttributes;
		}
	}

	@Override
	public void close() {
		shareLockedUpgradable(() -> this.initializedLoaders, Objects::nonNull, (e) -> {
			if (this.initializedLoaders == null) { return; }
			for (AttributeValuesLoader loader : this.initializedLoaders) {
				try {
					loader.close();
				} catch (Throwable t) {
					this.log.error("Exception caught closing a {} attribute loader in MetadataSet {}",
						loader.getClass().getSimpleName(), getId(), t);
				}
			}
			this.initializedLoaders = null;
		});
	}
}