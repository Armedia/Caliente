package com.armedia.caliente.engine.xml.extmeta;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlValue;

import org.apache.commons.lang3.StringUtils;

import com.armedia.commons.utilities.Tools;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "externalMetadataNamesList.t", propOrder = {
	"value"
})
public class SeparatedValuesList implements AttributeNamesLister {

	private static final Pattern SPLITTER = Pattern.compile("(?<!\\\\)#");

	public static final Character DEFAULT_SEPARATOR = Character.valueOf(',');

	@XmlValue
	protected String value;

	@XmlAttribute(name = "separator")
	protected String separator;

	public String getValue() {
		return this.value;
	}

	public void setValue(String value) {
		this.value = value;
	}

	public Character getSeparator() {
		return (StringUtils.isEmpty(this.separator) ? SeparatedValuesList.DEFAULT_SEPARATOR : this.separator.charAt(0));
	}

	public void setSeparator(Character value) {
		this.separator = Tools.toString(value);
	}

	@Override
	public Set<String> getNames() {
		Set<String> ret = new HashSet<>();
		if (this.value != null) {
			Matcher m = SeparatedValuesList.SPLITTER.matcher(this.value);
			int prev = 0;
			while (m.find()) {
				String str = this.value.substring(prev, m.start());
				str = StringUtils.strip(str);
				str = str.replaceAll("\\\\,", ",");
				if (!StringUtils.isEmpty(str) && !StringUtils.isAnyBlank(str)) {
					ret.add(str);
				}
				prev = m.end();
			}
		}
		return ret;
	}
}