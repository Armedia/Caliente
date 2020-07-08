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
package com.armedia.caliente.engine.dynamic.metadata;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.armedia.caliente.engine.dynamic.xml.ExternalMetadata;
import com.armedia.caliente.engine.dynamic.xml.XmlInstances;
import com.armedia.caliente.engine.dynamic.xml.XmlNotFoundException;
import com.armedia.caliente.engine.dynamic.xml.metadata.MetadataSet;
import com.armedia.caliente.engine.dynamic.xml.metadata.MetadataSource;
import com.armedia.caliente.store.CmfAttribute;
import com.armedia.caliente.store.CmfObject;
import com.armedia.commons.utilities.Tools;
import com.armedia.commons.utilities.concurrent.BaseShareableLockable;
import com.armedia.commons.utilities.concurrent.MutexAutoLock;
import com.armedia.commons.utilities.concurrent.SharedAutoLock;
import com.armedia.commons.utilities.function.CheckedFunction;

public class ExternalMetadataLoader extends BaseShareableLockable {

	private static final Collection<String> ALL_SOURCES = null;

	private static final XmlInstances<ExternalMetadata> INSTANCES = new XmlInstances<>(ExternalMetadata.class);

	public static ExternalMetadataLoader getExternalMetadataLoader(String location, boolean failIfMissing)
		throws ExternalMetadataException {
		try {
			try {
				ExternalMetadata externalMetadata = ExternalMetadataLoader.INSTANCES.getInstance(location);
				if (externalMetadata == null) { return null; }
				return new ExternalMetadataLoader(location, externalMetadata);
			} catch (final XmlNotFoundException e) {
				if (!failIfMissing) { return null; }
				throw e;
			}
		} catch (Exception e) {
			String pre = "";
			String post = "";
			if (location == null) {
				pre = "default ";
			} else {
				post = String.format(" from [%s]", location);
			}
			throw new ExternalMetadataException(
				String.format("Failed to load the %sexternal metadata configuration%s", pre, post), e);
		}
	}

	public static String getDefaultLocation() {
		return ExternalMetadataLoader.INSTANCES.getDefaultFileName();
	}

	private final Logger log = LoggerFactory.getLogger(getClass());

	private boolean initialized = false;

	private final String locationDesc;
	private final ExternalMetadata metadata;

	private final Map<String, MetadataSource> metadataSources = new LinkedHashMap<>();
	private final Map<String, MetadataSet> metadataSets = new LinkedHashMap<>();

	private ExternalMetadataLoader(String location, ExternalMetadata metadata) {
		if (location == null) {
			this.locationDesc = "the default configuration";
		} else {
			this.locationDesc = String.format("configuration [%s]", location);
		}
		this.metadata = metadata;
	}

	public void initialize() throws ExternalMetadataException {
		shareLockedUpgradable(() -> this.initialized, (t) -> !t, (i) -> {
			try {
				for (final MetadataSource src : this.metadata.getMetadataSources()) {
					try {
						src.initialize();
					} catch (Exception e) {
						throw new ExternalMetadataException(
							String.format("Failed to initialize the external metadata source [%s]", src.getName()), e);
					}
					this.metadataSources.put(src.getName(), src);
				}

				final Map<String, MetadataSource> frozenSources = Tools.freezeMap(this.metadataSources);
				final CheckedFunction<String, Connection, SQLException> connectionSource = (key) -> {
					MetadataSource mds = frozenSources.get(key);
					if (mds == null) {
						throw new SQLException(String.format("No DataSource named [%s] to get a connection from", key));
					}
					return mds.getConnection();
				};

				for (final MetadataSet desc : this.metadata.getMetadataSets()) {
					// It's OK to repeat this check...no harm in it at this point
					if (this.metadataSources.isEmpty()) {
						throw new ExternalMetadataException(
							"No metadata sources are defined - this is a configuration error!");
					}
					try {
						desc.initialize(connectionSource);
					} catch (Exception e) {
						if (desc.isFailOnError()) {
							// This item is required, so we must abort
							throw new ExternalMetadataException(
								"Failed to initialize a required external metadata source", e);
						}
					}
					this.metadataSets.put(desc.getId(), desc);
				}

				this.initialized = true;
			} finally {
				if (!this.initialized) {
					closeSources();
				}
			}
		});
	}

	public <V> Map<String, CmfAttribute<V>> getAttributeValues(CmfObject<V> object) throws ExternalMetadataException {
		return getAttributeValues(object, ExternalMetadataLoader.ALL_SOURCES);
	}

	public <V> Map<String, CmfAttribute<V>> getAttributeValues(CmfObject<V> object, String firstSourceName,
		String... sourceNames) throws ExternalMetadataException {
		if (StringUtils.isEmpty(firstSourceName)) {
			throw new IllegalArgumentException("Must provide the name of a source to retrieve the values from");
		}
		List<String> finalSources = new ArrayList<>();
		finalSources.add(firstSourceName);
		for (String source : sourceNames) {
			if (StringUtils.isEmpty(source)) {
				throw new IllegalArgumentException(
					String.format("The given source name [%s] is not valid from %s", source, this.locationDesc));
			}
			finalSources.add(source);
		}
		return getAttributeValues(object, finalSources);
	}

	public <V> Map<String, CmfAttribute<V>> getAttributeValues(CmfObject<V> object, Collection<String> sourceNames)
		throws ExternalMetadataException {
		Objects.requireNonNull(object, "Must provide a CmfObject instance to retrieve extra metadata for");
		initialize();
		try (SharedAutoLock lock = autoSharedLock()) {
			if (sourceNames == null) {
				sourceNames = this.metadataSets.keySet();
			}
			Map<String, CmfAttribute<V>> finalMap = new HashMap<>();
			for (String src : sourceNames) {
				final MetadataSet source = this.metadataSets.get(src);
				if (source == null) {
					throw new ExternalMetadataException(
						String.format("No metadata source named [%s] has been defined at %s", src, this.locationDesc));
				}

				Map<String, CmfAttribute<V>> m = null;
				try {
					m = source.getAttributeValues(object);
				} catch (Exception e) {
					if (source.isFailOnError()) {
						// There was an error which we should fail on
						throw new ExternalMetadataException(String.format(
							"Exception caught while retrieving required external metadata for %s from source [%s] at %s",
							object.getDescription(), source.getId(), this.locationDesc), e);
					}
					this.log.warn("Exception caught while retrieving external metadata for {} from source [{}] at {}",
						object.getDescription(), source.getId(), this.locationDesc, e);
					continue;
				}
				if (m == null) {
					if (source.isFailOnMissing()) {
						// The data is required, but not present - explode!!
						throw new ExternalMetadataException(String.format(
							"Did not retrieve any required external metadata for %s from source [%s] at %s",
							object.getDescription(), source.getId(), this.locationDesc));
					}
					if (this.log.isTraceEnabled()) {
						this.log.warn("Did not retrieve any external metadata for {} from source [{}] at {}",
							object.getDescription(), source.getId(), this.locationDesc);
					}
					continue;
				}

				// All is well...store what was retrieved
				finalMap.putAll(m);
			}
			return finalMap.isEmpty() ? null : finalMap;
		}
	}

	private void closeSources() {
		try {
			for (MetadataSet desc : this.metadataSets.values()) {
				try {
					desc.close();
				} catch (Throwable t) {
					this.log.warn("Exception caught while closing metadata set [{}] at {}", desc.getId(),
						this.locationDesc, t);
				}
			}
			for (MetadataSource src : this.metadataSources.values()) {
				try {
					src.close();
				} catch (Throwable t) {
					this.log.warn("Exception caught while closing metadata source [{}] at {}", src.getName(),
						this.locationDesc, t);
				}
			}
		} finally {
			this.metadataSets.clear();
		}
	}

	public void close() {
		try (MutexAutoLock lock = autoMutexLock()) {
			if (!this.initialized) { return; }
			try {
				closeSources();
			} finally {
				this.initialized = false;
			}
		}
	}
}