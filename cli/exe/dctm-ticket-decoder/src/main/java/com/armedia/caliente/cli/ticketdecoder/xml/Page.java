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
@XmlType(name = "page.t", propOrder = {
	"path"
})
@XmlRootElement(name = "page")
public class Page {

	@XmlAttribute(name = "number", required = true)
	protected long number;

	@XmlAttribute(name = "length", required = true)
	protected long length;

	@XmlAttribute(name = "hash", required = false)
	protected String hash;

	@XmlValue
	protected String path;

	public long getNumber() {
		return this.number;
	}

	public Page setNumber(long number) {
		this.number = number;
		return this;
	}

	public long getLength() {
		return this.length;
	}

	public Page setLength(long length) {
		this.length = length;
		return this;
	}

	public String getHash() {
		return this.hash;
	}

	public Page setHash(String hash) {
		if (StringUtils.isBlank(hash)) {
			hash = null;
		}
		this.hash = hash;
		return this;
	}

	public String getPath() {
		return this.path;
	}

	public Page setPath(String path) {
		this.path = path;
		return this;
	}

	@Override
	public int hashCode() {
		return Tools.hashTool(this, null, this.number, this.length, this.hash, this.path);
	}

	@Override
	public boolean equals(Object obj) {
		if (!Tools.baseEquals(this, obj)) { return false; }
		Page other = Page.class.cast(obj);
		if (this.number != other.number) { return false; }
		if (this.length != other.length) { return false; }
		if (!Tools.equals(this.hash, other.hash)) { return false; }
		if (!Tools.equals(this.path, other.path)) { return false; }
		return true;
	}

	@Override
	public String toString() {
		return String.format("Rendition [number=%d, length=%d, hash=%s, path=[%s]]", this.number, this.length,
			this.hash, this.path);
	}
}