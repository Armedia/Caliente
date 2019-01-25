package com.armedia.caliente.store.local;

import java.util.List;

@FunctionalInterface
public interface XmlStoreProperties<P extends XmlProperty> {

	public List<P> getProperty();

}