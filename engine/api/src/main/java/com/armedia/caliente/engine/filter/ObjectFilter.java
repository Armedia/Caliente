package com.armedia.caliente.engine.filter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.armedia.caliente.engine.dynamic.jaxb.ExternalMetadata;
import com.armedia.caliente.engine.dynamic.jaxb.XmlInstanceException;
import com.armedia.caliente.engine.dynamic.jaxb.XmlInstances;
import com.armedia.caliente.engine.dynamic.jaxb.extmeta.MetadataSource;
import com.armedia.caliente.store.CmfAttribute;
import com.armedia.caliente.store.CmfObject;

public class ObjectFilter {

	private static final Collection<String> ALL_SOURCES = null;

	private final Logger log = LoggerFactory.getLogger(getClass());

	private static final XmlInstances<ExternalMetadata> INSTANCES = new XmlInstances<>(ExternalMetadata.class,
		"external-metadata.xml");

	private boolean initialized = false;

	private final String locationDesc;
	private final ExternalMetadata metadata;

	private final Map<String, MetadataSource> sources = new LinkedHashMap<>();

	private final ReadWriteLock rwLock = new ReentrantReadWriteLock();

	public ObjectFilter(String location) throws FilterException {
		if (location == null) {
			this.locationDesc = "the default configuration";
		} else {
			this.locationDesc = String.format("configuration [%s]", location);
		}
		try {
			this.metadata = ObjectFilter.INSTANCES.getInstance(location);
		} catch (XmlInstanceException e) {
			throw new FilterException(
				String.format("Failed to load the external metadata configuration from %s", this.locationDesc), e);
		}
	}

	public void initialize() throws FilterException {
		initialize(null);
	}

	private void initialize(final Lock r) throws FilterException {
		final Lock w = this.rwLock.writeLock();
		if (r != null) {
			r.unlock();
		}
		w.lock();
		try {
			if (this.initialized) { return; }
			for (final MetadataSource desc : this.metadata.getSources()) {
				try {
					desc.initialize();
				} catch (Exception e) {
					if (desc.isFailOnError()) {
						// This item is required, so we must abort
						throw new FilterException("Failed to initialize a required external metadata source", e);
					}
				}
				this.sources.put(desc.getId(), desc);
			}
			this.initialized = true;
		} finally {
			if (!this.initialized) {
				closeSources();
			}
			if (r != null) {
				r.lock();
			}
			w.unlock();
		}
	}

	public <V> Map<String, CmfAttribute<V>> getAttributeValues(CmfObject<V> object) throws FilterException {
		return getAttributeValues(object, ObjectFilter.ALL_SOURCES);
	}

	public <V> Map<String, CmfAttribute<V>> getAttributeValues(CmfObject<V> object, String firstSourceName,
		String... sourceNames) throws FilterException {
		if (StringUtils.isEmpty(firstSourceName)) { throw new IllegalArgumentException(
			"Must provide the name of a source to retrieve the values from"); }
		List<String> finalSources = new ArrayList<>();
		finalSources.add(firstSourceName);
		for (String source : sourceNames) {
			if (StringUtils.isEmpty(source)) { throw new IllegalArgumentException(
				String.format("The given source name [%s] is not valid from %s", source, this.locationDesc)); }
			finalSources.add(source);
		}
		return getAttributeValues(object, finalSources);
	}

	public <V> Map<String, CmfAttribute<V>> getAttributeValues(CmfObject<V> object, Collection<String> sourceNames)
		throws FilterException {
		Objects.requireNonNull(object, "Must provide a CmfObject instance to retrieve extra metadata for");
		final Lock l = this.rwLock.readLock();
		l.lock();
		try {
			if (this.metadata == null) {
				initialize(l);
			}
			if (sourceNames == null) {
				sourceNames = this.sources.keySet();
			}
			Map<String, CmfAttribute<V>> finalMap = new HashMap<>();
			for (String src : sourceNames) {
				final MetadataSource source = this.sources.get(src);
				if (source == null) { throw new FilterException(
					String.format("No metadata source named [%s] has been defined at %s", src, this.locationDesc)); }

				Map<String, CmfAttribute<V>> m = null;
				try {
					m = source.getAttributeValues(object);
				} catch (Exception e) {
					if (source.isFailOnError()) {
						// There was an error which we should fail on
						throw new FilterException(String.format(
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
						throw new FilterException(String.format(
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
			return finalMap;
		} finally {
			l.unlock();
		}
	}

	private void closeSources() {
		try {
			for (MetadataSource desc : this.sources.values()) {
				try {
					desc.close();
				} catch (Throwable t) {
					this.log.warn("Exception caught while closing metadata source [{}] at {}", desc.getId(),
						this.locationDesc, t);
				}
			}
		} finally {
			this.sources.clear();
		}
	}

	public void close() {
		final Lock l = this.rwLock.writeLock();
		l.lock();
		try {
			if (!this.initialized) { return; }
			try {
				closeSources();
			} finally {
				this.initialized = false;
			}
		} finally {
			l.unlock();
		}
	}
}