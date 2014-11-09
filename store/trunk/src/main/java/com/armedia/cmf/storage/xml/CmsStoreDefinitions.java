package com.armedia.cmf.storage.xml;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "stores.t", propOrder = {
	"setting", "objectstore", "contentstore"
})
@XmlRootElement(name = "stores")
public class CmsStoreDefinitions extends SettingContainer {

	@XmlElement(required = true)
	protected List<CmsStoreConfiguration> objectstore;

	@XmlElement(required = true)
	protected List<CmsStoreConfiguration> contentstore;

	@Override
	protected void afterUnmarshal(Unmarshaller unmarshaller, Object parent) {
		super.afterUnmarshal(unmarshaller, parent);
	}

	@Override
	protected void beforeMarshal(Marshaller marshaller) {
		super.beforeMarshal(marshaller);
	}

	public List<CmsStoreConfiguration> getObjectStores() {
		if (this.objectstore == null) {
			this.objectstore = new ArrayList<CmsStoreConfiguration>();
		}
		return this.objectstore;
	}

	public List<CmsStoreConfiguration> getContentStores() {
		if (this.contentstore == null) {
			this.contentstore = new ArrayList<CmsStoreConfiguration>();
		}
		return this.contentstore;
	}
}