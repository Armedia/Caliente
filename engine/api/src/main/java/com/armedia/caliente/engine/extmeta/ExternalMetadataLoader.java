package com.armedia.caliente.engine.extmeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.armedia.caliente.engine.xml.ExternalMetadata;
import com.armedia.caliente.engine.xml.XmlInstanceException;
import com.armedia.caliente.engine.xml.XmlInstances;
import com.armedia.caliente.engine.xml.extmeta.MetadataSource;
import com.armedia.caliente.store.CmfAttribute;
import com.armedia.caliente.store.CmfObject;

public class ExternalMetadataLoader {

	private final Logger log = LoggerFactory.getLogger(getClass());

	private static final XmlInstances<ExternalMetadata> INSTANCES = new XmlInstances<>(ExternalMetadata.class,
		"external-metadata.xml");

	private ExternalMetadata metadata = null;

	private final List<MetadataSource> sources = new ArrayList<>();

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

	public synchronized void initialize() throws ExternalMetadataException {
		if (this.metadata == null) { return; }
		for (final MetadataSource desc : this.metadata.getSources()) {
			try {
				desc.initialize();
			} catch (Exception e) {
				if (desc.isFailOnError()) {
					// This item is required, so we must abort
					throw new ExternalMetadataException("Failed to initialize a required external metadata source", e);
				}
			}
			this.sources.add(desc);
		}
	}

	public <V> Map<String, CmfAttribute<V>> getAttributeValues(CmfObject<V> object) throws ExternalMetadataException {
		Map<String, CmfAttribute<V>> finalMap = new HashMap<>();
		for (final MetadataSource desc : this.sources) {
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
				this.log.warn("Exception caught while retrieving external metadata for {} [{}]({})", object.getType(),
					object.getLabel(), object.getId(), e);
				continue;
			}
			if (m == null) {
				if (desc.isFailOnMissing()) {
					// The data is required, but not present - explode!!
					throw new ExternalMetadataException(
						String.format("Did not find any required external metadata for %s [%s](%s)", object.getType(),
							object.getLabel(), object.getId()));
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
	}

	public synchronized void close() {
		if (this.metadata == null) { return; }
		try {
			for (MetadataSource desc : this.sources) {
				try {
					desc.close();
				} catch (Throwable t) {
					this.log.warn("Exception caught while closing a metadata source", t);
				}
			}
		} finally {
			this.metadata = null;
		}
	}
}