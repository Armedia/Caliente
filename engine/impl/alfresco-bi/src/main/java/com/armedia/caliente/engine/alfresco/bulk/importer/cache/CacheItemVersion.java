package com.armedia.caliente.engine.alfresco.bulk.importer.cache;

import java.math.BigDecimal;

import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "version.t", propOrder = {
	"number", "content", "metadata"
})
@XmlRootElement(name = "version")
public class CacheItemVersion implements Cloneable {
	@XmlElement(required = true)
	protected String number;

	@XmlTransient
	protected BigDecimal numberBd;

	@XmlElement(required = true)
	protected String content;

	@XmlElement(required = true)
	protected String metadata;

	protected CacheItemVersion(CacheItemVersion copy) {
		if (copy != null) {
			this.number = copy.number;
			this.numberBd = copy.numberBd;
			this.content = copy.content;
			this.metadata = copy.metadata;
		}
	}

	public CacheItemVersion() {

	}

	protected void afterUnmarshal(Unmarshaller unmarshaller, Object parent) {
		if (this.number == null) {
			this.numberBd = null;
		} else {
			this.numberBd = new BigDecimal(this.number);
		}
	}

	protected void beforeMarshal(Marshaller marshaller) {
		if (this.numberBd == null) {
			this.number = null;
		} else {
			this.number = this.numberBd.toString();
		}
	}

	public BigDecimal getNumber() {
		return this.numberBd;
	}

	public void setNumber(BigDecimal number) {
		this.numberBd = number;
		this.number = (number != null ? number.toString() : null);
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