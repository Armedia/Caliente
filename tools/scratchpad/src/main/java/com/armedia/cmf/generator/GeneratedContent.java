package com.armedia.cmf.generator;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAnyElement;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "generated_content.t", propOrder = {
	"any"
})
public class GeneratedContent {

	@XmlAnyElement(lax = true)
	protected List<Object> any;

	@XmlAttribute(name = "engine")
	protected String engine;

	public List<Object> getAny() {
		if (this.any == null) {
			this.any = new ArrayList<>();
		}
		return this.any;
	}

	public String getEngine() {
		return this.engine;
	}

	public void setEngine(String value) {
		this.engine = value;
	}
}