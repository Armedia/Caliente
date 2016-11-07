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
public class StoreDefinitions extends SettingContainer {

	@XmlElement(required = true)
	protected List<StoreConfiguration> objectstore;

	@XmlElement(required = true)
	protected List<StoreConfiguration> contentstore;

	@Override
	protected void afterUnmarshal(Unmarshaller unmarshaller, Object parent) {
		super.afterUnmarshal(unmarshaller, parent);
	}

	@Override
	protected void beforeMarshal(Marshaller marshaller) {
		super.beforeMarshal(marshaller);
	}

	public List<StoreConfiguration> getObjectStores() {
		if (this.objectstore == null) {
			this.objectstore = new ArrayList<StoreConfiguration>();
		}
		return this.objectstore;
	}

	public List<StoreConfiguration> getContentStores() {
		if (this.contentstore == null) {
			this.contentstore = new ArrayList<StoreConfiguration>();
		}
		return this.contentstore;
	}

	@Override
	public StoreDefinitions clone() {
		StoreDefinitions newClone = StoreDefinitions.class.cast(super.clone());
		if (this.objectstore != null) {
			newClone.objectstore = new ArrayList<StoreConfiguration>(this.objectstore);
		}
		if (this.contentstore != null) {
			newClone.contentstore = new ArrayList<StoreConfiguration>(this.contentstore);
		}
		return newClone;
	}
}