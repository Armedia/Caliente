package com.armedia.caliente.engine.cmis;

import org.apache.chemistry.opencmis.commons.SessionParameter;

import com.armedia.commons.utilities.ConfigurationSetting;

public enum CmisSessionSetting implements ConfigurationSetting {
	//
	USER(SessionParameter.USER),
	PASSWORD(SessionParameter.PASSWORD),
	BINDING_TYPE(SessionParameter.BINDING_TYPE),
	BINDING_SPI_CLASS(SessionParameter.BINDING_SPI_CLASS),
	ATOMPUB_URL(SessionParameter.ATOMPUB_URL),
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