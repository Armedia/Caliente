package com.armedia.caliente.tools.alfresco.bi.xml;

import java.math.BigDecimal;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import com.armedia.commons.utilities.Tools;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "version.t", propOrder = {
	"number", "content", "metadata"
})
@XmlRootElement(name = "version")
public class ScanIndexItemVersion implements Cloneable {
	@XmlElement(required = true)
	protected BigDecimal number;

	@XmlElement(required = true)
	protected String content;

	@XmlElement(required = true)
	protected String metadata;

	protected ScanIndexItemVersion(ScanIndexItemVersion copy) {
		if (copy != null) {
			this.number = copy.number;
			this.content = copy.content;
			this.metadata = copy.metadata;
		}
	}

	public ScanIndexItemVersion() {

	}

	public BigDecimal getNumber() {
		return this.number;
	}

	public void setNumber(BigDecimal number) {
		this.number = number;
	}

	public String getContent() {
		return this.content;
	}

	public void setContent(String content) {
		this.content = content;
	}

	public String getMetadata() {
		return this.metadata;
	}

	public void setMetadata(String metadata) {
		this.metadata = metadata;
	}

	@Override
	public int hashCode() {
		return Tools.hashTool(this, null, this.number, this.content, this.metadata);
	}

	@Override
	public boolean equals(Object obj) {
		if (!Tools.baseEquals(this, obj)) { return false; }
		ScanIndexItemVersion other = ScanIndexItemVersion.class.cast(obj);
		if (!Tools.equals(this.number, other.number)) { return false; }
		if (!Tools.equals(this.content, other.content)) { return false; }
		if (!Tools.equals(this.metadata, other.metadata)) { return false; }
		return true;
	}

	@Override
	public String toString() {
		return String.format("ScanIndexItemVersion [number=%s, content=%s, metadata=%s]", this.number, this.content,
			this.metadata);
	}

	@Override
	public ScanIndexItemVersion clone() {
		return new ScanIndexItemVersion(this);
	}
}