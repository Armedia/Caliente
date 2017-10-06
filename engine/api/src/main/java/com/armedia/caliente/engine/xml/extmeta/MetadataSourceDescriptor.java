package com.armedia.caliente.engine.xml.extmeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;

import com.armedia.caliente.engine.xml.ExternalMetadataContext;
import com.armedia.caliente.store.CmfAttribute;
import com.armedia.caliente.store.CmfObject;
import com.armedia.commons.utilities.Tools;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "externalMetadataSource.t", propOrder = {
	"settings", "sources"
})
public class MetadataSourceDescriptor {

	@XmlElementWrapper(name = "settings")
	@XmlElement(name = "setting", required = true)
	protected List<SettingT> settings;

	@XmlElements({
		@XmlElement(name = "from-sql", type = MetadataFromSQL.class),
		@XmlElement(name = "from-ddl", type = MetadataFromDDL.class)
	})
	protected List<AttributeValuesLoader> sources;

	@XmlAttribute(name = "failOnError", required = false)
	protected Boolean failOnError;

	@XmlAttribute(name = "failOnMissing", required = false)
	protected Boolean failOnMissing;

	@XmlTransient
	private ExternalMetadataContext context = null;

	public List<AttributeValuesLoader> getSources() {
		if (this.sources == null) {
			this.sources = new ArrayList<>();
		}
		return this.sources;
	}

	public List<SettingT> getSettings() {
		if (this.settings == null) {
			this.settings = new ArrayList<>();
		}
		return this.settings;
	}

	public Map<String, String> getSettingsMap() {
		Map<String, String> ret = new TreeMap<>();
		for (SettingT s : getSettings()) {
			String name = s.getName();
			String value = s.getValue();
			if ((name != null) && (value != null)) {
				ret.put(name, value);
			}
		}
		return ret;
	}

	public boolean isFailOnError() {
		return Tools.coalesce(this.failOnError, Boolean.FALSE);
	}

	public void setFailOnError(Boolean value) {
		this.failOnError = value;
	}

	public boolean isFailOnMissing() {
		return Tools.coalesce(this.failOnMissing, Boolean.FALSE);
	}

	public void setFailOnMissing(Boolean value) {
		this.failOnMissing = value;
	}

	public synchronized void initialize() throws Exception {
		if (this.context != null) { return; }
		// TODO initialize the base connection and datasource
	}

	public <V> Map<String, CmfAttribute<V>> getAttributeValues(CmfObject<V> object) throws Exception {
		Map<String, CmfAttribute<V>> finalAttributes = new HashMap<>();
		for (AttributeValuesLoader l : getSources()) {
			if (l != null) {
				Map<String, CmfAttribute<V>> newAttributes = null;
				try {
					newAttributes = l.getAttributeValues(this.context, object);
				} catch (Exception e) {
					if (isFailOnError()) {
						// An exceptikon was caught, but we need to fail on it
						throw new Exception(
							String.format("Exception raised while loading external metadata attributes for %s (%s)[%s]",
								object.getType(), object.getLabel(), object.getId()),
							e);
					} else {
						// TODO: Log this exception anyway...
					}
				}

				if ((newAttributes == null) && isFailOnMissing()) {
					// The attribute values are required, but none were found...this is an
					// error!
					throw new Exception(
						String.format("Did not find the required external metadata attributes for %s (%s)[%s]",
							object.getType(), object.getLabel(), object.getId()));
				}

				if (newAttributes != null) {
					finalAttributes.putAll(newAttributes);
				}
			}
		}
		return null;
	}

	public synchronized void close() {
		if (this.context == null) { return; }
		// TODO close everything up
	}
}