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
package com.armedia.caliente.engine.cmis;

import org.apache.chemistry.opencmis.commons.SessionParameter;

import com.armedia.commons.utilities.ConfigurationSetting;

public enum CmisSessionSetting implements ConfigurationSetting {
	//
	USER(SessionParameter.USER),
	PASSWORD(SessionParameter.PASSWORD),
	BINDING_TYPE(SessionParameter.BINDING_TYPE),
	BINDING_SPI_CLASS(SessionParameter.BINDING_SPI_CLASS),
	URL(null),
	REPOSITORY_ID(SessionParameter.REPOSITORY_ID),
	AUTHENTICATION_PROVIDER_CLASS(SessionParameter.AUTHENTICATION_PROVIDER_CLASS),
	OBJECT_FACTORY_CLASS(SessionParameter.OBJECT_FACTORY_CLASS),
	AUTH_HTTP_BASIC(SessionParameter.AUTH_HTTP_BASIC),
	AUTH_SOAP_USERNAMETOKEN(SessionParameter.AUTH_SOAP_USERNAMETOKEN),
	COMPRESSION(SessionParameter.COMPRESSION),
	CLIENT_COMPRESSION(SessionParameter.CLIENT_COMPRESSION),
	COOKIES(SessionParameter.COOKIES),
	CONNECT_TIMEOUT(SessionParameter.CONNECT_TIMEOUT),
	READ_TIMEOUT(SessionParameter.READ_TIMEOUT),
	PROXY_USER(SessionParameter.PROXY_USER),
	PROXY_PASSWORD(SessionParameter.PROXY_PASSWORD),
	CACHE_CLASS(SessionParameter.CACHE_CLASS),
	CACHE_SIZE_OBJECTS(SessionParameter.CACHE_SIZE_OBJECTS),
	CACHE_TTL_OBJECTS(SessionParameter.CACHE_TTL_OBJECTS),
	CACHE_SIZE_PATHTOID(SessionParameter.CACHE_SIZE_PATHTOID),
	CACHE_TTL_PATHTOID(SessionParameter.CACHE_TTL_PATHTOID),
	CACHE_PATH_OMIT(SessionParameter.CACHE_PATH_OMIT),
	CACHE_SIZE_REPOSITORIES(SessionParameter.CACHE_SIZE_REPOSITORIES),
	CACHE_SIZE_TYPES(SessionParameter.CACHE_SIZE_TYPES),
	CACHE_SIZE_LINKS(SessionParameter.CACHE_SIZE_LINKS),
	LOCALE_ISO639_LANGUAGE(SessionParameter.LOCALE_ISO639_LANGUAGE),
	LOCALE_ISO3166_COUNTRY(SessionParameter.LOCALE_ISO3166_COUNTRY),
	LOCALE_VARIANT(SessionParameter.LOCALE_VARIANT),
	DEFAULT_PAGE_SIZE(null, 0),
	DOMAIN(null, ""),
	//
	;

	private final String label;
	private final String sessionParameter;
	private final Object defaultValue;

	private CmisSessionSetting(String sessionParameter) {
		this(sessionParameter, null);
	}

	private CmisSessionSetting(String sessionParameter, Object defaultValue) {
		this.label = name().toLowerCase().replace('_', '.');
		this.sessionParameter = sessionParameter;
		this.defaultValue = defaultValue;
	}

	final String getSessionParameter() {
		return this.sessionParameter;
	}

	@Override
	public final String getLabel() {
		return this.label;
	}

	@Override
	public final Object getDefaultValue() {
		return this.defaultValue;
	}
}