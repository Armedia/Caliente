package com.armedia.caliente.engine.importer;

import java.io.Serializable;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

import com.armedia.caliente.store.CmfObjectRef;
import com.armedia.caliente.store.CmfType;

public class ImportRestriction implements Serializable {

	private static final long serialVersionUID = 1L;

	private static final Pattern OBJECT_RESTRICTION_PARSER = Pattern.compile("^\\s*%([^=]+)=(.+)\\s*$");
	private static final String RENDERER = "%%%s=%s";

	public static CmfObjectRef parse(String str) {
		String err = null;
		str = StringUtils.strip(str);
		if (!StringUtils.isBlank(str)) {
			// Parse it out...
			Matcher m = ImportRestriction.OBJECT_RESTRICTION_PARSER.matcher(str);
			if (m.matches()) {
				final String id = StringUtils.strip(m.group(2));
				if (!StringUtils.isEmpty(id)) {
					final String typeStr = m.group(1);
					try {
						return new CmfObjectRef(CmfType.decode(typeStr), StringUtils.strip(id));
					} catch (IllegalArgumentException e) {
						// Bad type! Ignore...
						err = String.format("Unknown object type or abbreviation [%s]", typeStr);
					}
				} else {
					err = "No object ID";
				}
			} else {
				err = String.format("Doesn't match the required format (RE = /%s/)",
					ImportRestriction.OBJECT_RESTRICTION_PARSER.pattern());
			}
		} else {
			err = "Empty string";
		}
		throw new IllegalArgumentException(String.format("Bad restrictor spec [%s] - %s", str, err));
	}

	public static String render(CmfObjectRef ref) {
		Objects.requireNonNull(ref, "Must provide a non-null CmfObjectRef instance");
		return ImportRestriction.render(ref.getType(), ref.getId());
	}

	public static String render(CmfType type, String id) {
		Objects.requireNonNull(type, "Must provdie a non-null object type");
		if (StringUtils.isBlank(id)) { throw new IllegalArgumentException("Must provide a non-null, non-blank ID"); }
		return String.format(ImportRestriction.RENDERER, type.abbrev, id);
	}
}