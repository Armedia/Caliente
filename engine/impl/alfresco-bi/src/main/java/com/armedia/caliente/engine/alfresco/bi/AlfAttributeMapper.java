package com.armedia.caliente.engine.alfresco.bi;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Collections;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;

import javax.xml.stream.XMLStreamException;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.concurrent.ConcurrentException;
import org.apache.commons.lang3.concurrent.ConcurrentInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.armedia.caliente.engine.tools.KeyLockableCache;
import com.armedia.caliente.store.CmfObject;
import com.armedia.caliente.tools.xml.XmlProperties;
import com.armedia.commons.utilities.Tools;

public class AlfAttributeMapper {
	private static final Logger LOG = LoggerFactory.getLogger(AlfAttributeMapper.class);
	private static final String PREFIX = "mappings/";
	private static final String MAPPINGS_SUFFIX = "-attribute-mappings.xml";
	private static final String COPIES_SUFFIX = "-attribute-copies.xml";

	private static final KeyLockableCache<String, AlfAttributeMapper> MAPPERS = new KeyLockableCache<String, AlfAttributeMapper>() {
		@Override
		protected KeyLockableCache<String, AlfAttributeMapper>.CacheItem newCacheItem(AlfAttributeMapper value) {
			return new DirectCacheItem(value);
		}
	};

	private static final AlfAttributeMapper NULL_MAPPER = new AlfAttributeMapper();

	private static Map<String, String> loadMappings(Class<?> klazz, String sourceProduct, String sourceVersion,
		String suffix) throws IOException, XMLStreamException {
		sourceProduct = StringUtils.lowerCase(sourceProduct);
		sourceProduct = sourceProduct.replaceAll("\\s+", "_");
		sourceVersion = StringUtils.lowerCase(sourceVersion);
		sourceVersion = sourceVersion.replaceAll("\\s+", "_");

		Map<String, String> commonMappings = new TreeMap<>();
		String commonName = String.format("%s%s%s", AlfAttributeMapper.PREFIX, sourceProduct, suffix);
		URL url = klazz.getResource(commonName);
		if (url != null) {
			// Load the mappings from the URL
			try (InputStream in = url.openStream()) {
				Properties props = XmlProperties.loadFromXML(in);
				for (String property : props.stringPropertyNames()) {
					commonMappings.put(property, props.getProperty(property));
				}
			}
		}

		String versionName = String.format("%s%s-%s%s", AlfAttributeMapper.PREFIX, sourceProduct, sourceVersion,
			suffix);
		url = klazz.getResource(versionName);
		if (url != null) {
			// Load the mappings from the URL, overriding the existing values. Empty values cause
			// the existing mapping to be removed. Other values simply override the existing
			// mappings
			try (InputStream in = url.openStream()) {
				Properties props = XmlProperties.loadFromXML(in);
				for (String property : props.stringPropertyNames()) {
					String value = props.getProperty(property);
					value = StringUtils.strip(value);
					if (StringUtils.isEmpty(value)) {
						commonMappings.remove(property);
					} else {
						commonMappings.put(property, value);
					}
				}
			}
		}

		return commonMappings;
	}

	private static String getKey(CmfObject<?> object) {
		return String.format("%s::%s", object.getProductName().toLowerCase(), object.getProductVersion().toLowerCase());
	}

	public static AlfAttributeMapper getMapper(final CmfObject<?> object) {
		try {
			return AlfAttributeMapper.MAPPERS.createIfAbsent(AlfAttributeMapper.getKey(object),
				new ConcurrentInitializer<AlfAttributeMapper>() {
					@Override
					public AlfAttributeMapper get() throws ConcurrentException {
						try {
							return new AlfAttributeMapper(object.getProductName(), object.getProductVersion());
						} catch (IOException | XMLStreamException e) {
							AlfAttributeMapper.LOG.error("Failed to load the mappings for {} {}",
								object.getProductName(), object.getProductVersion(), e);
							return AlfAttributeMapper.NULL_MAPPER;
						}
					}
				});
		} catch (ConcurrentException e) {
			throw new RuntimeException("Unexpected exception raised during initializer invocation", e);
		}
	}

	private final Map<String, String> attributeMap;
	private final Map<String, String> specialCopies;

	private AlfAttributeMapper() {
		this.attributeMap = Collections.emptyMap();
		this.specialCopies = Collections.emptyMap();
	}

	private AlfAttributeMapper(String sourceProduct, String sourceVersion) throws IOException, XMLStreamException {
		this.attributeMap = Tools.freezeMap(AlfAttributeMapper.loadMappings(getClass(), sourceProduct, sourceVersion,
			AlfAttributeMapper.MAPPINGS_SUFFIX));
		this.specialCopies = Tools.freezeMap(AlfAttributeMapper.loadMappings(getClass(), sourceProduct, sourceVersion,
			AlfAttributeMapper.COPIES_SUFFIX));
	}

	public Set<String> getSpecialCopies() {
		return this.specialCopies.keySet();
	}

	public String getSourceAttribute(String tgtAttribute) {
		if (tgtAttribute == null) { return null; }
		return this.attributeMap.get(tgtAttribute);
	}

	public String getSpecialCopyMapping(String specialName) {
		return this.specialCopies.get(specialName);
	}

	public String getSpecialCopySourceAttribute(String specialName) {
		String srcName = getSpecialCopyMapping(specialName);
		if (srcName == null) { return null; }
		// If it didn't need mapping, stick to the original name.
		return Tools.coalesce(getSourceAttribute(srcName), srcName);
	}
}