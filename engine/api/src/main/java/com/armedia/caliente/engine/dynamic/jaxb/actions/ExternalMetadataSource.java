
package com.armedia.caliente.engine.dynamic.jaxb.actions;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlType;

import com.armedia.caliente.engine.dynamic.jaxb.Expression;
import com.armedia.commons.utilities.Tools;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "actionLoadExternalMetadataSource.t")
public class ExternalMetadataSource extends Expression {

	@XmlAttribute(name = "override")
	protected Boolean override;

	public ExternalMetadataSource() {
		super();
	}

	public ExternalMetadataSource(String lang, String script) {
		super(lang, script);
	}

	public ExternalMetadataSource(String script) {
		super(script);
	}

	public boolean isOverride() {
		return Tools.coalesce(this.override, Boolean.FALSE);
	}

	public ExternalMetadataSource setOverride(Boolean override) {
		this.override = override;
		return this;
	}

}