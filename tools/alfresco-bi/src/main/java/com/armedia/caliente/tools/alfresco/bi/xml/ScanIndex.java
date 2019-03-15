package com.armedia.caliente.tools.alfresco.bi.xml;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import com.armedia.commons.utilities.Tools;

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
	public int hashCode() {
		return Tools.hashTool(this, null, this.items);
	}

	@Override
	public boolean equals(Object obj) {
		if (!Tools.baseEquals(this, obj)) { return false; }
		ScanIndex other = ScanIndex.class.cast(obj);
		if (!Tools.equals(this.items.size(), other.items.size())) { return false; }
		if (!Tools.equals(this.items, other.items)) { return false; }
		return true;
	}

	@Override
	public String toString() {
		return String.format("ScanIndex [items=%s]", this.items);
	}
}