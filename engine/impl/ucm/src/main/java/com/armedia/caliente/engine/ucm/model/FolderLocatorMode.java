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

import java.net.URI;

import oracle.stellent.ridc.model.DataBinder;

enum FolderLocatorMode {
	//
	BY_PATH {
		@Override
		protected void applySearchParameters(DataBinder binder, Object key) {
			binder.putLocal("path", key.toString());
		}

		@Override
		protected Object sanitizeKey(Object key) {
			return UcmModel.sanitizePath(key != null ? key.toString() : null);
		}
	},
	BY_GUID {
		@Override
		protected void applySearchParameters(DataBinder binder, Object key) {
			BY_URI.applySearchParameters(binder, UcmUniqueURI.class.cast(key).getURI());
		}
	},
	BY_URI {
		@Override
		protected void applySearchParameters(DataBinder binder, Object key) {
			URI uri = URI.class.cast(key);
			binder.putLocal("fFolderGUID", uri.getSchemeSpecificPart());
		}
	},
	//
	;

	protected abstract void applySearchParameters(DataBinder binder, Object key);

	protected Object sanitizeKey(Object key) {
		return key;
	}
}