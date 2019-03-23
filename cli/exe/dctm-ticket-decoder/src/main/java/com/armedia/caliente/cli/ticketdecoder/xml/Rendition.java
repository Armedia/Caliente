package com.armedia.caliente.cli.ticketdecoder.xml;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlValue;

import com.armedia.commons.utilities.Tools;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "rendition.t", propOrder = {
	"path"
})
@XmlRootElement(name = "rendition")
public class Rendition {

	@XmlAttribute(name = "number", required = true)
	protected long number;

	@XmlAttribute(name = "modifier", required = true)
	protected String modifier;

	@XmlAttribute(name = "page", required = true)
	protected long page;

	@XmlAttribute(name = "format", required = true)
	protected String format;

	@XmlValue
	protected String path;

	public long getNumber() {
		return this.number;
	}

	public Rendition setNumber(long number) {
		this.number = number;
		return this;
	}

	public String getModifier() {
		return this.modifier;
	}

	public Rendition setModifier(String modifier) {
		this.modifier = modifier;
		return this;
	}

	public long getPage() {
		return this.page;
	}

	public Rendition setPage(long page) {
		this.page = page;
		return this;
	}

	public String getFormat() {
		return this.format;
	}

	public Rendition setFormat(String format) {
		this.format = format;
		return this;
	}

	public String getPath() {
		return this.path;
	}

	public Rendition setPath(String path) {
		this.path = path;
		return this;
	}

	@Override
	public int hashCode() {
		return Tools.hashTool(this, null, this.number, this.modifier, this.page, this.format, this.path);
	}

	@Override
	public boolean equals(Object obj) {
		if (!Tools.baseEquals(this, obj)) { return false; }
		Rendition other = Rendition.class.cast(obj);
		if (!Tools.equals(this.number, other.number)) { return false; }
		if (!Tools.equals(this.modifier, other.modifier)) { return false; }
		if (!Tools.equals(this.page, other.page)) { return false; }
		if (!Tools.equals(this.format, other.format)) { return false; }
		if (!Tools.equals(this.path, other.path)) { return false; }
		return true;
	}

	@Override
	public String toString() {
		return String.format("Rendition [number=%s, modifier=%s, page=%s, format=%s, path=[%s]]", this.number,
			this.modifier, this.page, this.format, this.path);
	}
}