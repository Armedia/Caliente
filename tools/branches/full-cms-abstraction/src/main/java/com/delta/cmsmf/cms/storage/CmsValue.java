package com.delta.cmsmf.cms.storage;

import java.util.Date;

public interface CmsValue<T extends Object> {

	public CmsDataType getDataType();

	public boolean supports(CmsDataType targetType);

	public T getValue();

	public Boolean asBoolean();

	public Integer asInteger();

	public Double asDouble();

	public String asString();

	public Date asTemporal();

	public boolean isNull();

	public CmsValue<?> convert(CmsDataType newType);

	@Override
	public boolean equals(Object o);

	@Override
	public int hashCode();
}