package com.armedia.caliente.tools.xml;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "properties.t", propOrder = {
	"comment", "entries"
})
@XmlRootElement(name = "properties")
public class PropertiesRoot {

	@XmlElement(name = "entry")
	protected List<PropertiesEntry> entries;

	@XmlElement(name = "comment")
	protected PropertiesComment comment;

	public String getComment() {
		if (this.comment == null) { return null; }
		return this.comment.getValue();
	}

	public void setComment(String comment) {
		if (comment == null) {
			this.comment = null;
		} else {
			this.comment = new PropertiesComment();
			this.comment.setValue(comment);
		}
	}

	public List<PropertiesEntry> getEntries() {
		if (this.entries == null) {
			this.entries = new ArrayList<>();
		}
		return this.entries;
	}
}