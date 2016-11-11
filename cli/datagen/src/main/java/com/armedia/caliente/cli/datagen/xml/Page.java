package com.armedia.caliente.cli.datagen.xml;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "page.t", propOrder = {
	"url", "generated"
})
public class Page {

	@XmlElement(name = "url", required = false)
	protected UrlContent url;

	@XmlElement(name = "generated", required = false)
	protected GeneratedContent generated;

	public UrlContent getUrl() {
		return this.url;
	}

	public void setUrl(UrlContent value) {
		this.url = value;
	}

	public GeneratedContent getGenerated() {
		return this.generated;
	}

	public void setGenerated(GeneratedContent value) {
		this.generated = value;
	}
}