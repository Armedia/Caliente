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
	"path", "renditions"
})
@XmlRootElement(name = "content")
public class Content {

	@XmlAttribute(name = "id", required = true)
	protected String id;

	@XmlElement(name = "path", required = true)
	protected String path;

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

	public String getPath() {
		return this.path;
	}

	public Content setPath(String path) {
		this.path = path;
		return this;
	}

	public List<Rendition> getRenditions() {
		if (this.renditions == null) {
			this.renditions = new ArrayList<>();
		}
		return this.renditions;
	}

	@Override
	public int hashCode() {
		return Tools.hashTool(this, null, this.id, this.path, this.renditions);
	}

	@Override
	public boolean equals(Object obj) {
		if (!Tools.baseEquals(this, obj)) { return false; }
		Content other = Content.class.cast(obj);
		if (!Tools.equals(this.id, other.id)) { return false; }
		if (!Tools.equals(this.path, other.path)) { return false; }
		if (!Tools.equals(this.renditions, other.renditions)) { return false; }
		return true;
	}

	@Override
	public String toString() {
		return String.format("RenditionInfo [id=%s, path=[%s], renditions=%s]", this.id, this.path, this.renditions);
	}
}