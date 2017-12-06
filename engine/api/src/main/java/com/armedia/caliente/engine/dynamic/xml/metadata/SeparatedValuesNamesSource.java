package com.armedia.caliente.engine.dynamic.xml.metadata;

import java.sql.Connection;
import java.util.HashSet;
import java.util.Set;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlType;

import org.apache.commons.lang3.StringUtils;

import com.armedia.commons.utilities.Tools;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "externalMetadataNamesList.t", propOrder = {
	"value"
})
public class SeparatedValuesNamesSource extends AttributeNamesSource {

	public static final Character DEFAULT_SEPARATOR = Character.valueOf(',');

	@XmlAttribute(name = "separator")
	protected String separator;

	public Character getSeparator() {
		return (StringUtils.isEmpty(this.separator) ? SeparatedValuesNamesSource.DEFAULT_SEPARATOR
			: this.separator.charAt(0));
	}

	public void setSeparator(Character value) {
		this.separator = Tools.toString(value);
	}

	@Override
	protected final Set<String> getValues(Connection c) throws Exception {
		Set<String> values = new HashSet<>();
		for (String s : Tools.splitEscaped(getSeparator(), getValue())) {
			s = StringUtils.strip(s);
			if (!StringUtils.isEmpty(s)) {
				values.add(s);
			}
		}
		return values;
	}

}