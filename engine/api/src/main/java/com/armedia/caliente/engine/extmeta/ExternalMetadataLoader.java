package com.armedia.caliente.engine.extmeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.armedia.caliente.engine.transform.TransformationException;
import com.armedia.caliente.engine.xml.XmlInstances;
import com.armedia.caliente.engine.xml.extmeta.ExternalMetadata;
import com.armedia.caliente.engine.xml.extmeta.MetadataSourceDescriptor;
import com.armedia.caliente.store.CmfAttribute;
import com.armedia.caliente.store.CmfObject;

public class ExternalMetadataLoader {

	private static final XmlInstances<ExternalMetadata> INSTANCES = new XmlInstances<>(ExternalMetadata.class,
		"external-metadata.xml");

	private final ExternalMetadata metadata;

	private final List<MetadataSourceDescriptor> sources = new ArrayList<>();

	public ExternalMetadataLoader(String location) throws TransformationException {
		this.metadata = ExternalMetadataLoader.INSTANCES.getInstance(location);
	}

	public synchronized void initialize() throws Exception {
		if (this.metadata == null) { return; }
		for (final MetadataSourceDescriptor desc : this.metadata.getSources()) {
			try {
				desc.initialize();
			} catch (Exception e) {
				if (desc.isFailOnError()) {
					// This item is required, so we must abort
					throw new Exception("Failed to initialize a required external metadata source", e);
				}
			}
			this.sources.add(desc);
		}
	}

	public <V> Map<String, CmfAttribute<V>> getAttributeValues(CmfObject<V> object) throws Exception {
		Map<String, CmfAttribute<V>> finalMap = new HashMap<>();
		for (final MetadataSourceDescriptor desc : this.metadata.getSources()) {
			finalMap.putAll(desc.getAttributeValues(object));
		}
		return finalMap;
	}

	public synchronized void close() {
		for (MetadataSourceDescriptor desc : this.sources) {
			desc.close();
		}
	}
}