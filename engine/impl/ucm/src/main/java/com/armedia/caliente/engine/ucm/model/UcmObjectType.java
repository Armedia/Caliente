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
package com.armedia.caliente.engine.ucm.model;

import java.util.EnumMap;
import java.util.Map;

import com.armedia.caliente.store.CmfObject;
import com.armedia.commons.utilities.Tools;

public enum UcmObjectType {
	//
	FILE(CmfObject.Archetype.DOCUMENT), //
	FOLDER(CmfObject.Archetype.FOLDER), //
	//
	;

	private static final Map<CmfObject.Archetype, UcmObjectType> REVERSE;

	static {
		Map<CmfObject.Archetype, UcmObjectType> reverse = new EnumMap<>(CmfObject.Archetype.class);
		for (UcmObjectType t : UcmObjectType.values()) {
			UcmObjectType old = reverse.put(t.archetype, t);
			if (old != null) {
				throw new RuntimeException(
					String.format("UcmTypes %s and %s have identical CMF mappings to %s", t, old, t.archetype));
			}
		}
		REVERSE = Tools.freezeMap(reverse);
	}

	public final CmfObject.Archetype archetype;

	private UcmObjectType(CmfObject.Archetype archetype) {
		this.archetype = archetype;
	}

	public static UcmObjectType resolve(CmfObject.Archetype type) {
		return UcmObjectType.REVERSE.get(type);
	}
}