
package com.armedia.caliente.engine.dynamic.xml.actions;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlType;

import com.armedia.caliente.engine.dynamic.xml.Expression;
import com.armedia.commons.utilities.Tools;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "actionLoadExternalMetadataSet.t")
public class ExternalMetadataSet extends Expression {

	@XmlAttribute(name = "override")
	protected Boolean override;

	public ExternalMetadataSet() {
		super();
	}

	public ExternalMetadataSet(String lang, String script) {
		super(lang, script);
	}

	public ExternalMetadataSet(String script) {
		super(script);
	}

	public boolean isOverride() {
		return Tools.coalesce(this.override, Boolean.FALSE);
	}

	public ExternalMetadataSet setOverride(Boolean override) {
		this.override = override;
		return this;
	}

}