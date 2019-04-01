package com.armedia.caliente.cli.ticketdecoder.xml;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import com.armedia.commons.utilities.Tools;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "content.t", propOrder = {
	"paths", "renditions"
})
@XmlRootElement(name = "content")
public class Content {

	@XmlAttribute(name = "id", required = true)
	protected String id;

	@XmlElementWrapper(name = "paths", required = true)
	@XmlElement(name = "path", required = true)
	protected List<String> paths;

	@XmlElementWrapper(name = "renditions", required = true)
	@XmlElement(name = "rendition", required = true)
	protected List<Rendition> renditions;

	public String getId() {
		return this.id;
	}

	public Content setId(String id) {
		this.id = id;
		return this;
	}

	public List<String> getPaths() {
		if (this.paths == null) {
			this.paths = new ArrayList<>();
		}
		return this.paths;
	}

	public List<Rendition> getRenditions() {
		if (this.renditions == null) {
			this.renditions = new ArrayList<>();
		}
		return this.renditions;
	}

	@Override
	public int hashCode() {
		return Tools.hashTool(this, null, this.id, this.paths, this.renditions);
	}

	@Override
	public boolean equals(Object obj) {
		if (!Tools.baseEquals(this, obj)) { return false; }
		Content other = Content.class.cast(obj);
		if (!Tools.equals(this.id, other.id)) { return false; }
		if (!Tools.equals(this.paths, other.paths)) { return false; }
		if (!Tools.equals(this.renditions, other.renditions)) { return false; }
		return true;
	}

	@Override
	public String toString() {
		return String.format("Content [id=%s, paths=%s, renditions=%s]", this.id, this.paths, this.renditions);
	}
}