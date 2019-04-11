package com.armedia.caliente.cli.datagen.xml;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "document.t", propOrder = {
	"content", "rendition"
})
public class Document extends FsObject {

	@XmlElement(name = "content", required = false)
	protected Content content;

	@XmlElement(name = "rendition", required = false)
	protected List<Rendition> rendition;

	public Content getContent() {
		return this.content;
	}

	public void setContent(Content value) {
		this.content = value;
	}

	public List<Rendition> getRendition() {
		if (this.rendition == null) {
			this.rendition = new ArrayList<>();
		}
		return this.rendition;
	}
}