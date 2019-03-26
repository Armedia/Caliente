package com.armedia.caliente.cli.ticketdecoder.xml;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import com.armedia.commons.utilities.Tools;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "contents.t", propOrder = {
	"contents"
})
@XmlRootElement(name = "contents")
public class Contents {

	@XmlElement(name = "content", required = true)
	protected List<Content> contents;

	public List<Content> getContents() {
		if (this.contents == null) {
			this.contents = new ArrayList<>();
		}
		return this.contents;
	}

	@Override
	public int hashCode() {
		return Tools.hashTool(this, null, this.contents);
	}

	@Override
	public boolean equals(Object obj) {
		if (!Tools.baseEquals(this, obj)) { return false; }
		Contents other = Contents.class.cast(obj);
		if (!Tools.equals(this.contents, other.contents)) { return false; }
		return true;
	}

	@Override
	public String toString() {
		return String.format("Contents [contents=%s]", this.contents);
	}
}