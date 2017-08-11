package com.armedia.caliente.engine.alfresco.bi.cache;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "scan.t", propOrder = {
	"items"
})
@XmlRootElement(name = "scan")
public class Cache {
	@XmlElement(name = "item", required = true)
	protected List<CacheItem> items;

	public List<CacheItem> getItems() {
		if (this.items == null) {
			this.items = new ArrayList<>();
		}
		return this.items;
	}

	@Override
	public String toString() {
		return String.format("Cache [items=%s]", this.items);
	}
}