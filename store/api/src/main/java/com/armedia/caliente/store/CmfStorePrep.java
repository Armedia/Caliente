package com.armedia.caliente.store;

import java.util.function.Supplier;

import com.armedia.caliente.store.xml.StoreConfiguration;
import com.armedia.commons.utilities.CfgTools;

public interface CmfStorePrep extends Supplier<CfgTools> {

	public void prepareStore(StoreConfiguration cfg, boolean cleanData) throws CmfStoragePreparationException;

	public void close();
}