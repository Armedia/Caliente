package com.armedia.caliente.cli.ticketdecoder.xml;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlValue;

import org.apache.commons.lang3.StringUtils;

import com.armedia.commons.utilities.Tools;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "rendition.t", propOrder = {
	"path"
})
@XmlRootElement(name = "rendition")
public class Rendition {

	@XmlAttribute(name = "number", required = true)
	protected long number;

	@XmlAttribute(name = "page", required = true)
	protected long page;

	@XmlAttribute(name = "format", required = true)
	protected String format;

	@XmlAttribute(name = "length", required = true)
	protected long length;

	@XmlAttribute(name = "modifier", required = false)
	protected String modifier;

	@XmlAttribute(name = "hash", required = false)
	protected String hash;

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
		if (StringUtils.isBlank(modifier)) {
			modifier = null;
		}
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

	public long getLength() {
		return this.length;
	}

	public Rendition setLength(long length) {
		this.length = length;
		return this;
	}

	public String getHash() {
		return this.hash;
	}

	public Rendition setHash(String hash) {
		if (StringUtils.isBlank(hash)) {
			hash = null;
		}
		this.hash = hash;
		return this;
	}

	@Override
	public int hashCode() {
		return Tools.hashTool(this, null, this.number, this.modifier, this.page, this.format, this.length, this.hash,
			this.path);
	}

	@Override
	public boolean equals(Object obj) {
		if (!Tools.baseEquals(this, obj)) { return false; }
		Rendition other = Rendition.class.cast(obj);
		if (this.number != other.number) { return false; }
		if (this.page != other.page) { return false; }
		if (this.length != other.length) { return false; }
		if (!Tools.equals(this.modifier, other.modifier)) { return false; }
		if (!Tools.equals(this.format, other.format)) { return false; }
		if (!Tools.equals(this.hash, other.hash)) { return false; }
		if (!Tools.equals(this.path, other.path)) { return false; }
		return true;
	}

	@Override
	public String toString() {
		return String.format("Rendition [number=%d, page=%d, length=%d, modifier=%s, format=%s, hash=%s, path=[%s]]",
			this.number, this.page, this.length, this.modifier, this.format, this.hash, this.path);
	}
}