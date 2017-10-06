package com.armedia.caliente.engine.xml.extmeta;

import java.util.HashSet;
import java.util.Set;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlValue;

import org.apache.commons.lang3.StringUtils;

import com.armedia.commons.utilities.Tools;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "externalMetadataNamesList.t", propOrder = {
	"value"
})
public class SeparatedValuesList implements AttributeNamesLister {

	public static final Character DEFAULT_SEPARATOR = Character.valueOf(',');

	@XmlValue
	protected String value;

	@XmlAttribute(name = "separator")
	protected String separator;

	@XmlTransient
	private volatile Set<String> values = null;

	public String getValue() {
		return this.value;
	}

	public synchronized void setValue(String value) {
		this.value = value;
		this.values = null;
		getNames();
	}

	public Character getSeparator() {
		return (StringUtils.isEmpty(this.separator) ? SeparatedValuesList.DEFAULT_SEPARATOR : this.separator.charAt(0));
	}

	public void setSeparator(Character value) {
		this.separator = Tools.toString(value);
	}

	@Override
	public Set<String> getNames() {
		if (this.values != null) {
			synchronized (this) {
				if (this.values != null) {
					this.values = new HashSet<>();
					if (this.value != null) {
						for (String s : Tools.splitEscaped(this.value, getSeparator())) {
							s = StringUtils.strip(s);
							if (!StringUtils.isEmpty(s) && !StringUtils.isAnyBlank(s)) {
								this.values.add(s);
							}
						}
					}
				}
			}
		}
		return this.values;
	}
}