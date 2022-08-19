/*******************************************************************************
 * #%L
 * Armedia Caliente
 * %%
 * Copyright (C) 2013 - 2019 Armedia, LLC
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
package com.armedia.caliente.store;

import java.util.Map;

public interface CmfNameFixer<VALUE> {

	public boolean supportsType(CmfObject.Archetype type);

	public String fixName(CmfObject.Archetype type, String objectId, String historyId);

	public String fixName(CmfObject<VALUE> dataObject);

	public void nameFixed(CmfObject<VALUE> dataObject, String oldName, String newName);

	public Map<String, String> getMappings(CmfObject.Archetype type);

	public default boolean handleException(Exception e) {
		return false;
	}

	public default boolean isEmpty() {
		return false;
	}
}