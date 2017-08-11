package com.armedia.caliente.engine.alfresco.bi.cache;

import java.math.BigDecimal;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "version.t", propOrder = {
	"number", "content", "metadata"
})
@XmlRootElement(name = "version")
public class CacheItemVersion implements Cloneable {
	@XmlElement(required = true)
	protected BigDecimal number;

	@XmlElement(required = true)
	protected String content;

	@XmlElement(required = true)
	protected String metadata;

	protected CacheItemVersion(CacheItemVersion copy) {
		if (copy != null) {
			this.number = copy.number;
			this.content = copy.content;
			this.metadata = copy.metadata;
		}
	}

	public CacheItemVersion() {

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
	public String toString() {
		return String.format("CacheItemVersion [number=%s, content=%s, metadata=%s]", this.number, this.content,
			this.metadata);
	}

	@Override
	public CacheItemVersion clone() {
		return new CacheItemVersion(this);
	}
}