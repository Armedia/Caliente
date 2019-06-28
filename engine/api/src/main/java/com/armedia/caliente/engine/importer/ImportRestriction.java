/*******************************************************************************
 * #%L
 * Armedia Caliente
 * %%
 * Copyright (c) 2010 - 2019 Armedia LLC
 * %%
 * This file is part of the Caliente software. 
 *  
 * If the software was purchased under a paid Caliente license, the terms of 
 * the paid license agreement will prevail.  Otherwise, the software is 
 * provided under the following open source license terms:
 *
 * Caliente is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *   
 * Caliente is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Caliente. If not, see <http://www.gnu.org/licenses/>.
 * #L%
 *******************************************************************************/
package com.armedia.caliente.engine.importer;

import java.io.Serializable;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

import com.armedia.caliente.store.CmfObject;
import com.armedia.caliente.store.CmfObjectRef;

public class ImportRestriction implements Serializable {

	private static final long serialVersionUID = 1L;

	private static final Pattern OBJECT_RESTRICTION_PARSER = Pattern.compile("^\\s*%([^=]+)=(.+)\\s*$");
	private static final String RENDERER = "%%%s=%s";

	public static CmfObjectRef parse(String str) {
		return ImportRestriction.parse(str, true);
	}

	public static CmfObjectRef parseQuiet(String str) {
		return ImportRestriction.parse(str, false);
	}

	public static CmfObjectRef parse(String str, boolean raiseError) {
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
						return new CmfObjectRef(CmfObject.Archetype.decode(typeStr), StringUtils.strip(id));
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
		if (raiseError) {
			throw new IllegalArgumentException(String.format("Bad restrictor spec [%s] - %s", str, err));
		}
		return null;
	}

	public static String render(CmfObjectRef ref) {
		Objects.requireNonNull(ref, "Must provide a non-null CmfObjectRef instance");
		return ImportRestriction.render(ref.getType(), ref.getId());
	}

	public static String render(CmfObject.Archetype type, String id) {
		Objects.requireNonNull(type, "Must provdie a non-null object type");
		if (StringUtils.isBlank(id)) { throw new IllegalArgumentException("Must provide a non-null, non-blank ID"); }
		return String.format(ImportRestriction.RENDERER, type.abbrev, id);
	}
}