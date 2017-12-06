package com.armedia.cmf.generator;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "content.t", propOrder = {
	"page"
})
@XmlSeeAlso({
	Rendition.class
})
public class Content {

	@XmlElement(name = "page", required = true)
	protected List<Page> page;

	public List<Page> getPage() {
		if (this.page == null) {
			this.page = new ArrayList<>();
		}
		return this.page;
	}
}