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
package com.armedia.caliente.store.s3;

import com.armedia.commons.utilities.ConfigurationSetting;

public enum S3ContentStoreSetting implements ConfigurationSetting {
	//
	TEMP,
	REGION(S3ContentStoreFactory.DEFAULT_REGION),
	ENDPOINT,
	CREDENTIAL_TYPE(S3ContentStoreFactory.CredentialType.STATIC),
	ACCESS_KEY,
	SECRET_KEY,
	SESSION_TOKEN,
	PROFILE_LOCATION,
	PROFILE_NAME,
	CREATE_MISSING_BUCKET(false),
	ATTACH_METADATA(false),
	TRANSLATE_ATTRIBUTE_NAMES(true),
	BUCKET,
	BASE_PATH,
	URI_ORGANIZER,
	FAIL_ON_COLLISIONS(false),
	CSV_MAPPINGS,
	STORE_PROPERTIES(true),
	CHAR_FIX(S3ContentStore.CharFixer.REPLACE),
	//
	;

	private final String label;
	private final Object defaultValue;

	private S3ContentStoreSetting() {
		this(null);
	}

	private S3ContentStoreSetting(Object defaultValue) {
		String l = name();
		l = l.toLowerCase();
		l = l.replaceAll("_", ".");
		this.label = l;
		this.defaultValue = defaultValue;
	}

	@Override
	public String getLabel() {
		return this.label;
	}

	@Override
	public Object getDefaultValue() {
		return this.defaultValue;
	}
}