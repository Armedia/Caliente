package com.armedia.caliente.engine.alfresco.bi.importer.index;

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
public class ScanIndex {
	@XmlElement(name = "item", required = true)
	protected List<ScanIndexItem> items;

	public List<ScanIndexItem> getItems() {
		if (this.items == null) {
			this.items = new ArrayList<>();
		}
		return this.items;
	}

	@Override
	public String toString() {
		return String.format("ScanIndex [items=%s]", this.items);
	}
}