package com.armedia.caliente.engine.xml.extmeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlType;

import com.armedia.caliente.engine.xml.ExternalMetadataContext;
import com.armedia.caliente.store.CmfAttribute;
import com.armedia.caliente.store.CmfObject;
import com.armedia.commons.utilities.Tools;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "externalMetadataSource.t", propOrder = {
	"settings", "metadata"
})
public class MetadataSourceDescriptor implements AttributeValuesSource {

	@XmlElementWrapper(name = "settings")
	@XmlElement(name = "setting", required = true)
	protected List<SettingT> settings;

	@XmlElement(name = "metadata", required = true)
	protected List<Metadata> metadata;

	@XmlAttribute(name = "name", required = true)
	protected String name;

	@XmlAttribute(name = "failOnError", required = false)
	protected Boolean failOnError;

	@XmlAttribute(name = "failOnMissing", required = false)
	protected Boolean failOnMissing;

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

	public List<Metadata> getMetadata() {
		if (this.metadata == null) {
			this.metadata = new ArrayList<>();
		}
		return this.metadata;
	}

	public String getName() {
		return this.name;
	}

	public void setName(String value) {
		this.name = value;
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

	@Override
	public ExternalMetadataContext initialize() throws Exception {
		// TODO initialize the base connection and datasource
		return null;
	}

	@Override
	public <V> CmfAttribute<V> getAttributeValues(ExternalMetadataContext ctx, CmfObject<V> object) throws Exception {
		// TODO: Go through each of the metadata sources and retrieve the attribute data
		return null;
	}

	@Override
	public void close(ExternalMetadataContext ctx) {
		// TODO close everything and clean up!
	}
}