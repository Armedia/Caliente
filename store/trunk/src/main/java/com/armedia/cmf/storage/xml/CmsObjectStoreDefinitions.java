package com.armedia.cmf.storage.xml;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "objectstores.t", propOrder = {
	"setting", "objectstore"
})
@XmlRootElement(name = "objectstores")
public class CmsObjectStoreDefinitions extends SettingContainer {

	@XmlElement(required = true)
	protected List<CmsObjectStoreConfiguration> objectstore;

	public List<CmsObjectStoreConfiguration> getObjectStores() {
		if (this.objectstore == null) {
			this.objectstore = new ArrayList<CmsObjectStoreConfiguration>();
		}
		return this.objectstore;
	}
}