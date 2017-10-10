package com.armedia.caliente.engine.extmeta;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.armedia.caliente.engine.xml.ExternalMetadata;
import com.armedia.caliente.engine.xml.XmlInstanceException;
import com.armedia.caliente.engine.xml.XmlInstances;
import com.armedia.caliente.engine.xml.extmeta.MetadataSource;
import com.armedia.caliente.store.CmfAttribute;
import com.armedia.caliente.store.CmfObject;

public class ExternalMetadataLoader {

	private static final Collection<String> ALL_SOURCES = null;

	private final Logger log = LoggerFactory.getLogger(getClass());

	private static final XmlInstances<ExternalMetadata> INSTANCES = new XmlInstances<>(ExternalMetadata.class,
		"external-metadata.xml");

	private boolean initialized = false;

	private final ExternalMetadata metadata;

	private final Map<String, MetadataSource> sources = new HashMap<>();

	private final ReadWriteLock rwLock = new ReentrantReadWriteLock();

	public ExternalMetadataLoader(String location) throws ExternalMetadataException {
		try {
			this.metadata = ExternalMetadataLoader.INSTANCES.getInstance(location);
		} catch (XmlInstanceException e) {
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

	public void initialize() throws ExternalMetadataException {
		initialize(null);
	}

	private void initialize(final Lock r) throws ExternalMetadataException {
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
						throw new ExternalMetadataException("Failed to initialize a required external metadata source",
							e);
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

	public <V> Map<String, CmfAttribute<V>> getAttributeValues(CmfObject<V> object, String... sources)
		throws ExternalMetadataException {
		if (sources != null) { return getAttributeValues(object, Arrays.asList(sources)); }
		return getAttributeValues(object, ExternalMetadataLoader.ALL_SOURCES);
	}

	public <V> Map<String, CmfAttribute<V>> getAttributeValues(CmfObject<V> object, Collection<String> sources)
		throws ExternalMetadataException {
		final Lock l = this.rwLock.readLock();
		l.lock();
		try {
			if (this.metadata == null) {
				initialize(l);
			}
			if (sources == null) {
				sources = this.sources.keySet();
			}
			Map<String, CmfAttribute<V>> finalMap = new HashMap<>();
			for (String src : sources) {
				final MetadataSource desc = this.sources.get(src);
				if (desc == null) { throw new ExternalMetadataException(
					String.format("No metadata source named [%s] has been defined", src)); }

				Map<String, CmfAttribute<V>> m = null;
				try {
					m = desc.getAttributeValues(object);
				} catch (Exception e) {
					if (desc.isFailOnError()) {
						// There was an error which we should fail on
						throw new ExternalMetadataException(
							String.format("Exception caught while loading required external metadata for %s [%s](%s)",
								object.getType(), object.getLabel(), object.getId()),
							e);
					}
					this.log.warn("Exception caught while retrieving external metadata for {} [{}]({})",
						object.getType(), object.getLabel(), object.getId(), e);
					continue;
				}
				if (m == null) {
					if (desc.isFailOnMissing()) {
						// The data is required, but not present - explode!!
						throw new ExternalMetadataException(
							String.format("Did not find any required external metadata for %s [%s](%s)",
								object.getType(), object.getLabel(), object.getId()));
					}
					if (this.log.isTraceEnabled()) {
						this.log.warn("Did not retrieve any external metadata for {} [{}]({})", object.getType(),
							object.getLabel(), object.getId());
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
					this.log.warn("Exception caught while closing a metadata source", t);
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