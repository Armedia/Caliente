package com.armedia.caliente.engine.exporter;

import java.util.List;

import com.armedia.caliente.store.CmfAttributeTranslator;
import com.armedia.caliente.store.CmfContentStore;
import com.armedia.caliente.store.CmfContentStream;
import com.armedia.caliente.store.CmfObject;

public interface ContentExtractor extends AutoCloseable {

	public <VALUE> List<CmfContentStream> storeContent(CmfAttributeTranslator<VALUE> translator,
		CmfObject<VALUE> marshalled, CmfContentStore<?, ?> streamStore, boolean includeRenditions);

}